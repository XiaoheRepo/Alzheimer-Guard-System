package com.xiaohelab.guard.server.application.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaohelab.guard.server.common.exception.BizException;
import com.xiaohelab.guard.server.domain.ai.entity.AiSessionEntity;
import com.xiaohelab.guard.server.domain.ai.entity.AiSessionMessageEntity;
import com.xiaohelab.guard.server.domain.ai.repository.AiSessionRepository;
import com.xiaohelab.guard.server.domain.guardian.repository.GuardianRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * AI 会话应用服务。
 * 负责：会话创建、消息发送（JSON ACK / SSE 流式）、消息读取、归档、配额查询。
 * LLM 调用：阿里云百炼 DashScope text-generation 接口。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiSessionService {

    private static final String DASHSCOPE_URL =
            "https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation";

    private final AiSessionRepository aiSessionRepository;
    private final GuardianRepository guardianRepository;
    private final ObjectMapper objectMapper;

    @Value("${guard.ai.api-key:}")
    private String apiKey;

    @Value("${guard.ai.model-name:qwen-max-latest}")
    private String modelName;

    @Value("${guard.ai.user-daily-quota:20000}")
    private int userDailyQuota;

    @Value("${guard.ai.patient-daily-quota:50000}")
    private int patientDailyQuota;

    /** 用于 SSE 异步推送的线程池 */
    private final Executor sseExecutor = Executors.newVirtualThreadPerTaskExecutor();

    // =========================================================================
    // 3.5.1 CREATE SESSION
    // =========================================================================

    @Transactional
    public AiSessionEntity createSession(Long userId, Long patientId, Long taskId) {
        // 校验用户对患者的监护授权
        if (guardianRepository.countActiveRelation(userId, patientId) == 0) {
            throw BizException.of("E_PRO_4030");
        }

        AiSessionEntity session = AiSessionEntity.create(
                generateSessionId(), userId, patientId, taskId, modelName);
        aiSessionRepository.insert(session);
        return session;
    }

    // =========================================================================
    // 3.5.2 LIST SESSIONS
    // =========================================================================

    public List<AiSessionEntity> listSessions(Long userId, Long patientId, int page, int size) {
        int offset = (page - 1) * size;
        return aiSessionRepository.listByUserId(userId, patientId, size, offset);
    }

    public long countSessions(Long userId, Long patientId) {
        return aiSessionRepository.countByUserId(userId, patientId);
    }

    // =========================================================================
    // GET SESSION BY ID (3.6.4)
    // =========================================================================

    public AiSessionEntity getSession(String sessionId, Long userId) {
        AiSessionEntity session = requireSession(sessionId);
        requireSessionOwnerOrAdmin(session, userId);
        return session;
    }

    // =========================================================================
    // 3.5.3 SEND MESSAGE – JSON ACK
    // =========================================================================

    @Transactional
    public Map<String, Object> sendMessageAck(String sessionId, Long userId,
                                              String prompt, String traceId) {
        AiSessionEntity session = requireSession(sessionId);
        requireSessionOwnerOrAdmin(session, userId);
        if (!"ACTIVE".equals(session.getStatus())) throw BizException.of("E_AI_4091");

        // 保存用户消息
        int nextSeq = aiSessionRepository.maxSequenceNo(sessionId) + 1;
        AiSessionMessageEntity userMsg = buildMessage(sessionId, nextSeq, "user", prompt, null);
        aiSessionRepository.insertMessage(userMsg);

        String messageId = generateMessageId();

        // 异步调用 LLM 并保存回复（不阻塞本次响应）
        sseExecutor.execute(() -> callLlmAndSave(session, prompt, nextSeq + 1, messageId, traceId));

        return Map.of(
                "session_id", sessionId,
                "message_id", messageId,
                "accepted", true,
                "stream_transport", "SSE",
                "stream_channel", "/api/v1/ai/sessions/" + sessionId + "/messages/" + messageId + "/stream",
                "stream_status", "QUEUED",
                "version", String.valueOf(session.getVersion()));
    }

    // =========================================================================
    // 3.5.3 SEND MESSAGE – SSE STREAM
    // =========================================================================

    public SseEmitter sendMessageStream(String sessionId, Long userId,
                                        String prompt, String traceId) {
        AiSessionEntity session = requireSession(sessionId);
        requireSessionOwnerOrAdmin(session, userId);
        if (!"ACTIVE".equals(session.getStatus())) throw BizException.of("E_AI_4091");

        SseEmitter emitter = new SseEmitter(120_000L);

        // 保存用户消息（同步）
        int nextSeq = aiSessionRepository.maxSequenceNo(sessionId) + 1;
        AiSessionMessageEntity userMsg = buildMessage(sessionId, nextSeq, "user", prompt, null);
        aiSessionRepository.insertMessage(userMsg);
        String messageId = generateMessageId();

        try {
            emitter.send(SseEmitter.event()
                    .name("ack")
                    .data("{\"session_id\":\"" + sessionId + "\",\"message_id\":\"" + messageId +
                          "\",\"stream_status\":\"STREAMING\"}"));
        } catch (Exception e) {
            emitter.completeWithError(e);
            return emitter;
        }

        sseExecutor.execute(() ->
                callLlmStream(session, prompt, nextSeq + 1, messageId, emitter, traceId));

        return emitter;
    }

    // =========================================================================
    // 3.5.7 GET MESSAGES
    // =========================================================================

    public List<AiSessionMessageEntity> getMessages(String sessionId, Long userId,
                                                    int page, int size) {
        AiSessionEntity session = requireSession(sessionId);
        requireSessionOwnerOrAdmin(session, userId);
        int offset = (page - 1) * size;
        return aiSessionRepository.listMessages(sessionId, size, offset);
    }

    public long countMessages(String sessionId) {
        return aiSessionRepository.countMessages(sessionId);
    }

    // =========================================================================
    // 3.5.10 QUOTA
    // =========================================================================

    public Map<String, Object> getQuota(String sessionId, Long userId) {
        AiSessionEntity session = requireSession(sessionId);
        requireSessionOwnerOrAdmin(session, userId);

        int tokenUsed = session.getTokenUsed() == null ? 0 : session.getTokenUsed();
        int remaining = Math.max(0, userDailyQuota - tokenUsed);
        String resetAt = LocalDate.now(ZoneOffset.UTC).plusDays(1)
                .atStartOfDay().toInstant(ZoneOffset.UTC).toString();

        return Map.of(
                "session_id", sessionId,
                "user_quota_daily", String.valueOf(userDailyQuota),
                "user_used_daily", String.valueOf(tokenUsed),
                "patient_quota_daily", String.valueOf(patientDailyQuota),
                "patient_used_daily", String.valueOf(tokenUsed),
                "remaining_tokens", String.valueOf(remaining),
                "reset_at", resetAt);
    }

    // =========================================================================
    // 3.5.11 ARCHIVE
    // =========================================================================

    @Transactional
    public AiSessionEntity archiveSession(String sessionId, Long userId) {
        AiSessionEntity session = requireSession(sessionId);
        requireSessionOwnerOrAdmin(session, userId);
        if (!"ACTIVE".equals(session.getStatus())) throw BizException.of("E_AI_4091");
        int updated = aiSessionRepository.archiveBySessionId(sessionId);
        if (updated == 0) throw BizException.of("E_AI_4091");
        session.archive();
        return session;
    }

    // =========================================================================
    // ADMIN LIST SESSIONS (3.5.4)
    // =========================================================================

    public List<AiSessionEntity> listAllSessions(Long userId, Long patientId, int page, int size) {
        int offset = (page - 1) * size;
        return aiSessionRepository.listAll(userId, patientId, size, offset);
    }

    public long countAllSessions(Long userId, Long patientId) {
        return aiSessionRepository.countAll(userId, patientId);
    }

    public AiSessionEntity adminGetSession(String sessionId) {
        return requireSession(sessionId);
    }

    public List<AiSessionMessageEntity> adminGetMessages(String sessionId, int page, int size) {
        requireSession(sessionId); // ensure exists
        int offset = (page - 1) * size;
        return aiSessionRepository.listMessages(sessionId, size, offset);
    }

    // =========================================================================
    // PRIVATE HELPERS
    // =========================================================================

    private AiSessionEntity requireSession(String sessionId) {
        return aiSessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> BizException.of("E_AI_4041"));
    }

    private void requireSessionOwnerOrAdmin(AiSessionEntity session, Long callerUserId) {
        if (!session.getUserId().equals(callerUserId)) throw BizException.of("E_AI_4033");
    }

    private AiSessionMessageEntity buildMessage(String sessionId, int seqNo, String role,
                                                String content, String tokenUsage) {
        return AiSessionMessageEntity.create(sessionId, seqNo, role, content, tokenUsage);
    }

    private String generateSessionId() {
        String ts = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
                .format(java.time.LocalDateTime.now(ZoneOffset.UTC));
        return "ais_" + ts + "_" + randomHex(4);
    }

    private String generateMessageId() {
        String ts = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
                .format(java.time.LocalDateTime.now(ZoneOffset.UTC));
        return "aim_" + ts + "_" + randomHex(4);
    }

    private String randomHex(int bytes) {
        byte[] b = new byte[bytes];
        new java.util.Random().nextBytes(b);
        StringBuilder sb = new StringBuilder();
        for (byte v : b) sb.append(String.format("%02x", v));
        return sb.toString();
    }

    /** 非流式调用 DashScope，保存回复并更新 token 消耗 */
    private void callLlmAndSave(AiSessionEntity session, String prompt,
                                int assistantSeq, String messageId, String traceId) {
        try {
            String requestBody = buildLlmRequest(session, prompt, false);
            HttpResponse<String> resp = postToDashScope(requestBody, false);

            if (resp.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(resp.body());
                String reply = root.path("output").path("choices").get(0)
                        .path("message").path("content").asText("");
                int totalTokens = root.path("usage").path("total_tokens").asInt(0);

                AiSessionMessageEntity assistantMsg = buildMessage(
                        session.getSessionId(), assistantSeq, "assistant", reply,
                        "{\"total_tokens\":" + totalTokens + "}");
                aiSessionRepository.insertMessage(assistantMsg);

                // CAS 更新 token 消耗（重试最多 3 次）
                for (int i = 0; i < 3; i++) {
                    AiSessionEntity latest = aiSessionRepository
                            .findBySessionId(session.getSessionId()).orElse(null);
                    if (latest == null) break;
                    int rows = aiSessionRepository.casAddTokens(
                            session.getSessionId(), latest.getVersion(), totalTokens);
                    if (rows > 0) break;
                }
            } else {
                log.warn("[AI] DashScope 非流式调用失败 session={} status={}",
                        session.getSessionId(), resp.statusCode());
            }
        } catch (Exception e) {
            log.error("[AI] callLlmAndSave 异常 session={}", session.getSessionId(), e);
        }
    }

    /** 流式调用 DashScope，逐 delta 推送到 SseEmitter */
    private void callLlmStream(AiSessionEntity session, String prompt,
                               int assistantSeq, String messageId,
                               SseEmitter emitter, String traceId) {
        try {
            String requestBody = buildLlmRequest(session, prompt, true);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(DASHSCOPE_URL))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .header("Accept", "text/event-stream")
                    .header("X-DashScope-SSE", "enable")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<java.io.InputStream> resp =
                    client.send(request, HttpResponse.BodyHandlers.ofInputStream());

            StringBuilder fullContent = new StringBuilder();
            int totalTokens = 0;
            int index = 0;

            if (resp.statusCode() == 200) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(resp.body()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("data:")) {
                            String data = line.substring(5).trim();
                            if ("[DONE]".equals(data)) break;
                            try {
                                JsonNode node = objectMapper.readTree(data);
                                String delta = node.path("output").path("choices").get(0)
                                        .path("message").path("content").asText("");
                                fullContent.append(delta);
                                index++;
                                emitter.send(SseEmitter.event()
                                        .name("delta")
                                        .data("{\"index\":" + index + ",\"content\":" +
                                              objectMapper.writeValueAsString(delta) + "}"));
                                if (!node.path("usage").isMissingNode()) {
                                    totalTokens = node.path("usage").path("total_tokens").asInt(0);
                                }
                            } catch (Exception parseEx) {
                                log.debug("[AI] 流式 delta 解析跳过: {}", data);
                            }
                        }
                    }
                }
            } else {
                emitter.send(SseEmitter.event()
                        .name("error")
                        .data("{\"code\":\"E_AI_4001\",\"message\":\"LLM call failed\"}"));
                emitter.complete();
                return;
            }

            int finalTokens = totalTokens;
            AiSessionMessageEntity assistantMsg = buildMessage(
                    session.getSessionId(), assistantSeq, "assistant",
                    fullContent.toString(), "{\"total_tokens\":" + finalTokens + "}");
            aiSessionRepository.insertMessage(assistantMsg);

            for (int i = 0; i < 3; i++) {
                AiSessionEntity latest = aiSessionRepository
                        .findBySessionId(session.getSessionId()).orElse(null);
                if (latest == null) break;
                int rows = aiSessionRepository.casAddTokens(
                        session.getSessionId(), latest.getVersion(), finalTokens);
                if (rows > 0) break;
            }

            String doneData = "{\"token_usage\":{\"total_tokens\":" + finalTokens +
                    "},\"version\":\"" + (session.getVersion() + 1) + "\"}";
            emitter.send(SseEmitter.event().name("done").data(doneData));
            emitter.complete();

        } catch (Exception e) {
            log.error("[AI] callLlmStream 异常 session={}", session.getSessionId(), e);
            try {
                emitter.send(SseEmitter.event()
                        .name("error")
                        .data("{\"code\":\"E_AI_4001\",\"message\":\"Internal error\"}"));
            } catch (Exception ignored) {}
            emitter.completeWithError(e);
        }
    }

    private String buildLlmRequest(AiSessionEntity session, String prompt, boolean stream) {
        List<AiSessionMessageEntity> history = aiSessionRepository
                .listMessages(session.getSessionId(), 10, 0);

        StringBuilder messages = new StringBuilder("[");
        for (AiSessionMessageEntity m : history) {
            if (messages.length() > 1) messages.append(",");
            messages.append("{\"role\":\"").append(escapeJson(m.getRole())).append("\",")
                    .append("\"content\":\"").append(escapeJson(m.getContent())).append("\"}");
        }
        if (messages.length() > 1) messages.append(",");
        messages.append("{\"role\":\"user\",\"content\":\"").append(escapeJson(prompt)).append("\"}");
        messages.append("]");

        return "{\"model\":\"" + escapeJson(modelName) + "\"," +
                "\"input\":{\"messages\":" + messages + "}," +
                "\"parameters\":{\"result_format\":\"message\"," +
                "\"incremental_output\":" + stream + "}}";
    }

    private HttpResponse<String> postToDashScope(String body, boolean stream) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(DASHSCOPE_URL))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }
}
