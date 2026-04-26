package com.xiaohelab.guard.server.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaohelab.guard.server.ai.dto.AiChatRequest;
import com.xiaohelab.guard.server.ai.dto.AiSessionCreateRequest;
import com.xiaohelab.guard.server.ai.dto.RagHit;
import com.xiaohelab.guard.server.ai.entity.AiSessionEntity;
import com.xiaohelab.guard.server.ai.repository.AiSessionRepository;
import com.xiaohelab.guard.server.common.dto.CursorResponse;
import com.xiaohelab.guard.server.common.error.ErrorCode;
import com.xiaohelab.guard.server.common.event.OutboxTopics;
import com.xiaohelab.guard.server.common.exception.BizException;
import com.xiaohelab.guard.server.common.security.AuthUser;
import com.xiaohelab.guard.server.common.security.SecurityUtil;
import com.xiaohelab.guard.server.common.util.BusinessNoUtil;
import com.xiaohelab.guard.server.common.util.CursorUtil;
import com.xiaohelab.guard.server.common.util.RedisKeys;
import com.xiaohelab.guard.server.gov.service.AuditLogger;
import com.xiaohelab.guard.server.outbox.service.OutboxService;
import com.xiaohelab.guard.server.patient.service.GuardianAuthorizationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 会话服务（毕设实现：同步响应占位，替代 SSE 流式）。
 * 生产环境请接入外部 LLM（Qwen/Deepseek 等），此处给出同步桩。
 */
@Service
public class AiSessionService {

    private static final Logger log = LoggerFactory.getLogger(AiSessionService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final AiSessionRepository sessionRepository;
    private final GuardianAuthorizationService authorizationService;
    private final AiQuotaService quotaService;
    private final OutboxService outboxService;
    private final AuditLogger auditLogger;
    private final StringRedisTemplate redis;
    private final DashScopeClient dashScopeClient;
    private final RagRetrievalService ragRetrievalService;

    public AiSessionService(AiSessionRepository sessionRepository,
                            GuardianAuthorizationService authorizationService,
                            AiQuotaService quotaService,
                            OutboxService outboxService,
                            AuditLogger auditLogger,
                            StringRedisTemplate redis,
                            DashScopeClient dashScopeClient,
                            RagRetrievalService ragRetrievalService) {
        this.sessionRepository = sessionRepository;
        this.authorizationService = authorizationService;
        this.quotaService = quotaService;
        this.outboxService = outboxService;
        this.auditLogger = auditLogger;
        this.redis = redis;
        this.dashScopeClient = dashScopeClient;
        this.ragRetrievalService = ragRetrievalService;
    }

    /**
     * 创建一个 AI 会话；若 prompt 非空则立即发起第一轮对话。
     * @param req 会话创建请求（patientId、taskId可选、首条 prompt）
     * @throws BizException E_PRO_4033 无监护权
     */
    @Transactional(rollbackFor = Exception.class)
    public AiSessionEntity create(AiSessionCreateRequest req) {
        AuthUser user = SecurityUtil.current();
        authorizationService.assertGuardian(user, req.getPatientId());
        AiSessionEntity s = new AiSessionEntity();
        s.setSessionId(BusinessNoUtil.sessionId());
        s.setUserId(user.getUserId());
        s.setPatientId(req.getPatientId());
        s.setTaskId(req.getTaskId());
        s.setMessages("[]");
        s.setStatus("ACTIVE");
        sessionRepository.save(s);
        if (req.getPrompt() != null && !req.getPrompt().isBlank()) {
            chat(s.getSessionId(), new AiChatRequest() {{ setPrompt(req.getPrompt()); }});
        }
        return s;
    }

    /**
     * 在已存在的会话上追加一轮对话：
     * <ol><li>预占配额（reserve）；</li>
     *     <li>将 user/assistant 消息 append 到 messages；</li>
     *     <li>计算 token 数；</li>
     *     <li>成功时 commit、异常时 rollback 配额。</li></ol>
     *
     * @param sessionId 会话业务 ID
     * @param req       单轮 prompt
     * @return 包含 session_id / reply / total_tokens 的响应地图
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> chat(String sessionId, AiChatRequest req) {
        AuthUser user = SecurityUtil.current();
        AiSessionEntity s = sessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> BizException.of(ErrorCode.E_AI_4041));
        if (!"ACTIVE".equals(s.getStatus())) throw BizException.of(ErrorCode.E_AI_4091);
        if (!user.isAdmin() && !s.getUserId().equals(user.getUserId())) {
            throw BizException.of(ErrorCode.E_AI_4033);
        }
        authorizationService.assertGuardian(user, s.getPatientId());
        // 1. 预占配额
        quotaService.reserve(user.getUserId(), s.getPatientId());
        try {
            List<Map<String, Object>> messages = deserializeMessages(s.getMessages());
            messages.add(Map.of("role", "user", "content", req.getPrompt(), "at", OffsetDateTime.now().toString()));
            // 2. 调用 LLM —— 毕设桩：将 prompt 转发为简易回复
            String reply = buildStubReply(req.getPrompt(), s);
            messages.add(Map.of("role", "assistant", "content", reply, "at", OffsetDateTime.now().toString()));
            s.setMessages(serializeMessages(messages));
            int promptTok = Math.max(1, req.getPrompt().length() / 2);
            int compTok = Math.max(1, reply.length() / 2);
            s.setPromptTokens(s.getPromptTokens() + promptTok);
            s.setCompletionTokens(s.getCompletionTokens() + compTok);
            s.setTotalTokens(s.getPromptTokens() + s.getCompletionTokens());
            sessionRepository.save(s);
            quotaService.commit(user.getUserId(), s.getPatientId());
            Map<String, Object> out = new HashMap<>();
            out.put("session_id", s.getSessionId());
            out.put("reply", reply);
            out.put("total_tokens", s.getTotalTokens());
            return out;
        } catch (Exception ex) {
            quotaService.rollback(user.getUserId(), s.getPatientId());
            throw ex;
        }
    }

    /**
     * 用户对某一会话提交评分反馈（1–5）。
     * @throws BizException E_AI_4041 会话不存在；E_AI_4033 非归属用户
     */
    @Transactional(rollbackFor = Exception.class)
    public AiSessionEntity feedback(String sessionId, Integer rating, String comment) {
        AuthUser user = SecurityUtil.current();
        AiSessionEntity s = sessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> BizException.of(ErrorCode.E_AI_4041));
        if (!s.getUserId().equals(user.getUserId())) throw BizException.of(ErrorCode.E_AI_4033);
        s.setFeedbackRating(rating);
        s.setFeedbackComment(comment);
        s.setFeedbackAt(OffsetDateTime.now());
        sessionRepository.save(s);
        return s;
    }

    /**
     * 查询会话详情。非管理员需有对应患者的监护权。
     */
    public AiSessionEntity get(String sessionId) {
        AuthUser user = SecurityUtil.current();
        AiSessionEntity s = sessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> BizException.of(ErrorCode.E_AI_4041));
        if (!user.isAdmin() && !s.getUserId().equals(user.getUserId())) {
            authorizationService.assertGuardian(user, s.getPatientId());
        }
        return s;
    }

    private List<Map<String, Object>> deserializeMessages(String raw) {
        try {
            if (raw == null || raw.isBlank()) return new ArrayList<>();
            return MAPPER.readValue(raw, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            log.warn("messages json parse fail, reset. {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    private String serializeMessages(List<Map<String, Object>> list) {
        try { return MAPPER.writeValueAsString(list); }
        catch (JsonProcessingException e) { throw BizException.of(ErrorCode.E_SYS_5000, "messages 序列化失败"); }
    }

    private String buildStubReply(String prompt, AiSessionEntity s) {
        // 1. 优先调用百炼 DashScope（V2.1 §19.4）
        if (dashScopeClient != null && dashScopeClient.isEnabled()) {
            String baseSystem = "你是「码上回家」阿尔兹海默症协同寻回系统的智能助手，"
                    + "面向走失患者的家属，帮助分析行为、轨迹、走失风险与寻回建议。"
                    + "回答务必简洁、可执行；不得编造未提供的事实。"
                    + "当前对话患者 ID=" + s.getPatientId()
                    + (s.getTaskId() != null ? "，关联寻回任务 ID=" + s.getTaskId() : "")
                    + "。请使用中文。";
            // FR-AI-004：拼入 RAG 上下文（降级为空时自动忽略）
            List<RagHit> hits = (ragRetrievalService != null)
                    ? ragRetrievalService.retrieveContext(s.getPatientId(), prompt)
                    : List.of();
            String system = appendRagContext(baseSystem, hits);
            if (!hits.isEmpty()) {
                // FR-AI-006 可解释性：记录引用来源 ID（前端展示见 Phase 4）
                List<String> refs = new ArrayList<>(hits.size());
                for (RagHit h : hits) refs.add(h.sourceType() + ":" + h.sourceId());
                log.info("[AI] RAG hit sessionId={} patientId={} refs={}",
                        s.getSessionId(), s.getPatientId(), refs);
            }
            var reply = dashScopeClient.chat(system, prompt);
            if (reply.isPresent() && !reply.get().isBlank()) {
                return reply.get();
            }
            log.warn("[AI] DashScope 返回空，回退本地桩 sessionId={}", s.getSessionId());
        }
        // 2. 降级桩
        return "[本地桩回复] 针对患者 ID=" + s.getPatientId() + " 您的问题：" + prompt
                + "\n建议：保持冷静、优先查看患者常规轨迹点、联系监护成员协同寻回。";
    }

    /** 把 RAG 命中拼到 systemPrompt 末尾；空命中时原样返回。 */
    private String appendRagContext(String baseSystem, List<RagHit> hits) {
        if (hits == null || hits.isEmpty()) return baseSystem;
        StringBuilder sb = new StringBuilder(baseSystem);
        sb.append("\n\n=== 患者背景知识（请优先参考） ===");
        for (int i = 0; i < hits.size(); i++) {
            RagHit h = hits.get(i);
            sb.append("\n[").append(i + 1).append("][").append(h.sourceType()).append("] ")
              .append(h.content()).append(" (相似度 ").append(String.format("%.2f", h.similarity())).append(")");
        }
        sb.append("\n===");
        return sb.toString();
    }

    // ============================================================
    // V2.1 §3.8.1 基线增量：list / archive / messages / cancel
    // ============================================================

    /**
     * 会话列表（V2.1 §3.8.1.1）。Cursor 分页（按 id 倒序）。
     */
    public CursorResponse<Map<String, Object>> list(Long patientId, Long taskId,
                                                    String status, String cursor, int pageSize) {
        AuthUser user = SecurityUtil.current();
        int size = Math.max(1, Math.min(pageSize, 50));
        Long cursorId = CursorUtil.decodeId(cursor);
        Page<AiSessionEntity> p = sessionRepository.findForList(
                user.getUserId(), patientId, taskId, status, cursorId,
                PageRequest.of(0, size));
        List<AiSessionEntity> rows = p.getContent();
        List<Map<String, Object>> items = new ArrayList<>(rows.size());
        for (AiSessionEntity s : rows) items.add(toSummary(s));
        boolean hasNext = rows.size() >= size;
        String nextCursor = hasNext ? CursorUtil.encode(rows.get(rows.size() - 1).getId()) : null;
        return CursorResponse.of(items, size, nextCursor, hasNext);
    }

    /**
     * 归档会话（V2.1 §3.8.1.3）。
     * <ul>
     *   <li>幂等：已 ARCHIVED 则直接返回当前快照；</li>
     *   <li>CAS：依赖 {@code @Version} 乐观锁，冲突抛 {@code E_AI_4091}；</li>
     *   <li>Outbox：发布 {@link OutboxTopics#AI_SESSION_ARCHIVED}。</li>
     * </ul>
     */
    @Transactional(rollbackFor = Exception.class)
    public AiSessionEntity archive(String sessionId) {
        AuthUser user = SecurityUtil.current();
        AiSessionEntity s = sessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> BizException.of(ErrorCode.E_AI_4041));
        // 1. 访问控制：仅所有者（或管理员）可归档
        if (!user.isAdmin() && !s.getUserId().equals(user.getUserId())) {
            throw BizException.of(ErrorCode.E_AI_4030);
        }
        if ("ARCHIVED".equals(s.getStatus())) {
            // 2. 幂等：已归档直接返回，避免重复 Outbox
            return s;
        }
        s.setStatus("ARCHIVED");
        s.setArchivedAt(OffsetDateTime.now());
        try {
            s = sessionRepository.save(s);
        } catch (org.springframework.orm.ObjectOptimisticLockingFailureException e) {
            throw BizException.of(ErrorCode.E_AI_4091, "会话并发冲突,请重试");
        }
        outboxService.publish(OutboxTopics.AI_SESSION_ARCHIVED,
                s.getSessionId(), s.getSessionId(),
                Map.of("session_id", s.getSessionId(),
                        "user_id", s.getUserId(),
                        "patient_id", s.getPatientId(),
                        "archived_at", s.getArchivedAt()));
        auditLogger.logSuccess("AI", "ai.session.archive", s.getSessionId(),
                "LOW", "CONFIRM_1", Map.of("patient_id", s.getPatientId()));
        log.info("[AI] session archived session_id={} user_id={}", s.getSessionId(), s.getUserId());
        return s;
    }

    /**
     * 会话消息历史（V2.1 §3.8.1.4）。按消息自增序号 cursor 分页（messages JSONB 数组内的 seq 位）。
     * <p>方向支持 {@code BACKWARD}（默认）/{@code FORWARD}。</p>
     */
    public CursorResponse<Map<String, Object>> getMessages(String sessionId, String cursor,
                                                           String direction, int pageSize) {
        AuthUser user = SecurityUtil.current();
        AiSessionEntity s = sessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> BizException.of(ErrorCode.E_AI_4041));
        if (!user.isAdmin() && !s.getUserId().equals(user.getUserId())) {
            throw BizException.of(ErrorCode.E_AI_4030);
        }
        int size = Math.max(1, Math.min(pageSize, 100));
        boolean backward = !"FORWARD".equalsIgnoreCase(direction);
        List<Map<String, Object>> all = deserializeMessages(s.getMessages());
        // 为每条消息注入 seq（从 1 开始）
        for (int i = 0; i < all.size(); i++) {
            Map<String, Object> m = new HashMap<>(all.get(i));
            m.putIfAbsent("seq", i + 1);
            m.putIfAbsent("message_id", "m_" + s.getSessionId() + "_" + (i + 1));
            all.set(i, m);
        }
        Long cursorSeq = CursorUtil.decodeId(cursor);
        List<Map<String, Object>> view = new ArrayList<>();
        if (backward) {
            // 从尾部向前
            int end = (cursorSeq == null) ? all.size() : Math.min(cursorSeq.intValue() - 1, all.size());
            int start = Math.max(0, end - size);
            for (int i = end - 1; i >= start; i--) view.add(all.get(i));
            boolean hasNext = start > 0;
            String next = hasNext ? CursorUtil.encode((long) (start + 1)) : null;
            return CursorResponse.of(view, size, next, hasNext);
        } else {
            int start = (cursorSeq == null) ? 0 : Math.max(0, cursorSeq.intValue());
            int end = Math.min(start + size, all.size());
            for (int i = start; i < end; i++) view.add(all.get(i));
            boolean hasNext = end < all.size();
            String next = hasNext ? CursorUtil.encode((long) end) : null;
            return CursorResponse.of(view, size, next, hasNext);
        }
    }

    /**
     * 取消正在进行的消息生成（V2.1 §3.8.1.5）。
     * <p>Redis 标记 {@code ai:cancel:{sid}:{mid}} 通知流式 worker；幂等。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> cancelMessage(String sessionId, String messageId) {
        AuthUser user = SecurityUtil.current();
        AiSessionEntity s = sessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> BizException.of(ErrorCode.E_AI_4041));
        if (!user.isAdmin() && !s.getUserId().equals(user.getUserId())) {
            throw BizException.of(ErrorCode.E_AI_4030);
        }
        String key = RedisKeys.aiCancel(sessionId, messageId);
        redis.opsForValue().set(key, "1", Duration.ofMinutes(5));
        outboxService.publish(OutboxTopics.AI_MESSAGE_CANCELLED,
                sessionId, sessionId,
                Map.of("session_id", sessionId, "message_id", messageId,
                        "user_id", s.getUserId(), "cancelled_at", OffsetDateTime.now()));
        log.info("[AI] cancel message sid={} mid={}", sessionId, messageId);
        return Map.of("session_id", sessionId, "message_id", messageId,
                "cancelled_at", OffsetDateTime.now());
    }

    private Map<String, Object> toSummary(AiSessionEntity s) {
        List<Map<String, Object>> msgs = deserializeMessages(s.getMessages());
        String title = null;
        Object lastAt = null;
        for (Map<String, Object> m : msgs) {
            if (title == null && "user".equals(m.get("role"))) {
                Object c = m.get("content");
                if (c != null) {
                    String str = c.toString();
                    title = str.length() > 24 ? str.substring(0, 24) + "…" : str;
                }
            }
            lastAt = m.get("at");
        }
        Map<String, Object> out = new HashMap<>();
        out.put("session_id", s.getSessionId());
        out.put("patient_id", s.getPatientId());
        out.put("task_id", s.getTaskId());
        out.put("status", s.getStatus());
        out.put("title", title == null ? "未命名会话" : title);
        out.put("message_count", msgs.size());
        out.put("total_tokens", s.getTotalTokens());
        out.put("model_name", s.getModelName());
        out.put("last_message_at", lastAt);
        out.put("created_at", s.getCreatedAt());
        out.put("archived_at", s.getArchivedAt());
        return out;
    }
}
