package com.xiaohelab.guard.server.application.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaohelab.guard.server.common.exception.BizException;
import com.xiaohelab.guard.server.infrastructure.persistence.do_.AiSessionDO;
import com.xiaohelab.guard.server.infrastructure.persistence.do_.AiSessionMessageDO;
import com.xiaohelab.guard.server.infrastructure.persistence.mapper.AiSessionMapper;
import com.xiaohelab.guard.server.infrastructure.persistence.mapper.AiSessionMessageMapper;
import com.xiaohelab.guard.server.infrastructure.persistence.mapper.SysUserPatientMapper;
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
import java.time.Instant;
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
    private static final int TOKEN_QUOTA_USER  = 20_000;
    private static final int TOKEN_QUOTA_PATIENT = 50_000;

    private final AiSessionMapper aiSessionMapper;
    private final AiSessionMessageMapper aiSessionMessageMapper;
    private final SysUserPatientMapper sysUserPatientMapper;
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
    public AiSessionDO createSession(Long userId, Long patientId, Long taskId) {
        // 校验用户对患者的监护授权
        long rel = sysUserPatientMapper.countActiveRelation(userId, patientId);
        if (rel == 0) throw BizException.of("E_PRO_4030");

        String sessionId = generateSessionId();
        AiSessionDO session = new AiSessionDO();
        session.setSessionId(sessionId);
        session.setUserId(userId);
        session.setPatientId(patientId);
        session.setTaskId(taskId);
        session.setMessages("[]");
        session.setRequestTokens(0);
        session.setResponseTokens(0);
        session.setTokenUsage("{}");
        session.setTokenUsed(0);
        session.setModelName(modelName);
        session.setStatus("ACTIVE");
        session.setVersion(1L);
        aiSessionMapper.insert(session);
        return session;
    }

    // =========================================================================
    // 3.5.2 LIST SESSIONS
    // =========================================================================

    public List<AiSessionDO> listSessions(Long userId, Long patientId, int page, int size) {
        int offset = (page - 1) * size;
        return aiSessionMapper.listByUserId(userId, patientId, size, offset);
    }

    public long countSessions(Long userId, Long patientId) {
        return aiSessionMapper.countByUserId(userId, patientId);
    }

    // =========================================================================
    // GET SESSION BY ID (3.6.4)
    // =========================================================================

    public AiSessionDO getSession(String sessionId, Long userId) {
        AiSessionDO session = requireSession(sessionId);
        requireSessionOwnerOrAdmin(session, userId);
        return session;
    }

    // =========================================================================
    // 3.5.3 SEND MESSAGE – JSON ACK
    // =========================================================================

    @Transactional
    public Map<String, Object> sendMessageAck(String sessionId, Long userId,
                                               String prompt, String traceId) {
        AiSessionDO session = requireSession(sessionId);
        requireSessionOwnerOrAdmin(session, userId);
        if (!"ACTIVE".equals(session.getStatus())) throw BizException.of("E_AI_4091");

        // 保存用户消息
        int nextSeq = aiSessionMessageMapper.maxSequenceNo(sessionId) + 1;
        AiSessionMessageDO userMsg = buildMessage(sessionId, nextSeq, "user", prompt, null);
        aiSessionMessageMapper.insert(userMsg);

        // 生成 message_id
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
        AiSessionDO session = requireSession(sessionId);
        requireSessionOwnerOrAdmin(session, userId);
        if (!"ACTIVE".equals(session.getStatus())) throw BizException.of("E_AI_4091");

        SseEmitter emitter = new SseEmitter(120_000L); // 2-min timeout

        // 保存用户消息（同步）
        int nextSeq = aiSessionMessageMapper.maxSequenceNo(sessionId) + 1;
        AiSessionMessageDO userMsg = buildMessage(sessionId, nextSeq, "user", prompt, null);
        aiSessionMessageMapper.insert(userMsg);
        String messageId = generateMessageId();

        // 先发 ack 事件
        try {
            emitter.send(SseEmitter.event()
                    .name("ack")
                    .data("{\"session_id\":\"" + sessionId + "\",\"message_id\":\"" + messageId +
                          "\",\"stream_status\":\"STREAMING\"}"));
        } catch (Exception e) {
            emitter.completeWithError(e);
            return emitter;
        }

        // 异步流式调用 LLM
        sseExecutor.execute(() ->
                callLlmStream(session, prompt, nextSeq + 1, messageId, emitter, traceId));

        return emitter;
    }

    // =========================================================================
    // 3.5.7 GET MESSAGES
    // =========================================================================

    public List<AiSessionMessageDO> getMessages(String sessionId, Long userId, int page, int size) {
        AiSessionDO session = requireSession(sessionId);
        requireSessionOwnerOrAdmin(session, userId);
        int offset = (page - 1) * size;
        return aiSessionMessageMapper.listBySessionId(sessionId, size, offset);
    }

    public long countMessages(String sessionId) {
        return aiSessionMessageMapper.countBySessionId(sessionId);
    }

    // =========================================================================
    // 3.5.10 QUOTA
    // =========================================================================

    public Map<String, Object> getQuota(String sessionId, Long userId) {
        AiSessionDO session = requireSession(sessionId);
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
    public AiSessionDO archiveSession(String sessionId, Long userId) {
        AiSessionDO session = requireSession(sessionId);
        requireSessionOwnerOrAdmin(session, userId);
        if (!"ACTIVE".equals(session.getStatus())) throw BizException.of("E_AI_4091");
        int updated = aiSessionMapper.archiveBySessionId(sessionId);
        if (updated == 0) throw BizException.of("E_AI_4091");
        session.setStatus("ARCHIVED");
        session.setArchivedAt(Instant.now());
        return session;
    }

    // =========================================================================
    // ADMIN LIST SESSIONS (3.5.4)
    // =========================================================================

    public List<AiSessionDO> listAllSessions(Long userId, Long patientId, int page, int size) {
        int offset = (page - 1) * size;
        return aiSessionMapper.listAll(userId, patientId, size, offset);
    }

    public long countAllSessions(Long userId, Long patientId) {
        return aiSessionMapper.countAll(userId, patientId);
    }

    /** Admin-level: get session by ID without ownership check */
    public AiSessionDO adminGetSession(String sessionId) {
        return requireSession(sessionId);
    }

    /** Admin-level: list messages without ownership check */
    public List<AiSessionMessageDO> adminGetMessages(String sessionId, int page, int size) {
        requireSession(sessionId); // ensure exists
        int offset = (page - 1) * size;
        return aiSessionMessageMapper.listBySessionId(sessionId, size, offset);
    }

    // =========================================================================
    // PRIVATE HELPERS
    // =========================================================================

    private AiSessionDO requireSession(String sessionId) {
        AiSessionDO s = aiSessionMapper.findBySessionId(sessionId);
        if (s == null) throw BizException.of("E_AI_4041");
        return s;
    }

    private void requireSessionOwnerOrAdmin(AiSessionDO session, Long callerUserId) {
        if (!session.getUserId().equals(callerUserId)) throw BizException.of("E_AI_4033");
    }

    private AiSessionMessageDO buildMessage(String sessionId, int seqNo, String role,
                                             String content, String tokenUsage) {
        AiSessionMessageDO msg = new AiSessionMessageDO();
        msg.setSessionId(sessionId);
        msg.setSequenceNo(seqNo);
        msg.setRole(role);
        msg.setContent(content);
        msg.setTokenUsage(tokenUsage != null ? tokenUsage : "{}");
        return msg;
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
    private void callLlmAndSave(AiSessionDO session, String prompt,
                                 int assistantSeq, String messageId, String traceId) {
        try {
            String requestBody = buildLlmRequest(session, prompt, false);
            HttpResponse<String> resp = postToDashScope(requestBody, false);

            if (resp.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(resp.body());
                String reply = root.path("output").path("choices").get(0)
                        .path("message").path("content").asText("");
                int totalTokens = root.path("usage").path("total_tokens").asInt(0);

                AiSessionMessageDO assistantMsg = buildMessage(
                        session.getSessionId(), assistantSeq, "assistant", reply,
                        "{\"total_tokens\":" + totalTokens + "}");
                aiSessionMessageMapper.insert(assistantMsg);

                // CAS 更新 token 消耗（重试最多 3 次）
                for (int i = 0; i < 3; i++) {
                    AiSessionDO latest = aiSessionMapper.findBySessionId(session.getSessionId());
                    if (latest == null) break;
                    int rows = aiSessionMapper.casAddTokens(
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
    private void callLlmStream(AiSessionDO session, String prompt,
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
                                // push delta event
                                emitter.send(SseEmitter.event()
                                        .name("delta")
                                        .data("{\"index\":" + index + ",\"content\":" +
                                              objectMapper.writeValueAsString(delta) + "}"));
                                // capture final usage
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

            // 保存 assistant 消息
            int finalTokens = totalTokens;
            AiSessionMessageDO assistantMsg = buildMessage(
                    session.getSessionId(), assistantSeq, "assistant",
                    fullContent.toString(),
                    "{\"total_tokens\":" + finalTokens + "}");
            aiSessionMessageMapper.insert(assistantMsg);

            // CAS 更新 token
            for (int i = 0; i < 3; i++) {
                AiSessionDO latest = aiSessionMapper.findBySessionId(session.getSessionId());
                if (latest == null) break;
                int rows = aiSessionMapper.casAddTokens(
                        session.getSessionId(), latest.getVersion(), finalTokens);
                if (rows > 0) break;
            }

            // push done event
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

    private String buildLlmRequest(AiSessionDO session, String prompt, boolean stream) {
        // 拼接最近几条消息作为上下文（取最新 10 条，节约 token）
        List<AiSessionMessageDO> history = aiSessionMessageMapper
                .listBySessionId(session.getSessionId(), 10, 0);

        StringBuilder messages = new StringBuilder("[");
        for (AiSessionMessageDO m : history) {
            if (messages.length() > 1) messages.append(",");
            messages.append("{\"role\":\"").append(escapeJson(m.getRole())).append("\",")
                    .append("\"content\":\"").append(escapeJson(m.getContent())).append("\"}");
        }
        // 本次用户消息（history 中可能还没有，取决于事务时序，此处直接追加）
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
