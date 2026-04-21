package com.xiaohelab.guard.server.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaohelab.guard.server.ai.dto.AiChatRequest;
import com.xiaohelab.guard.server.ai.dto.AiSessionCreateRequest;
import com.xiaohelab.guard.server.ai.entity.AiSessionEntity;
import com.xiaohelab.guard.server.ai.repository.AiSessionRepository;
import com.xiaohelab.guard.server.common.error.ErrorCode;
import com.xiaohelab.guard.server.common.exception.BizException;
import com.xiaohelab.guard.server.common.security.AuthUser;
import com.xiaohelab.guard.server.common.security.SecurityUtil;
import com.xiaohelab.guard.server.common.util.BusinessNoUtil;
import com.xiaohelab.guard.server.patient.service.GuardianAuthorizationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public AiSessionService(AiSessionRepository sessionRepository,
                            GuardianAuthorizationService authorizationService,
                            AiQuotaService quotaService) {
        this.sessionRepository = sessionRepository;
        this.authorizationService = authorizationService;
        this.quotaService = quotaService;
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
        return "[毕设桩回复] 针对患者 ID=" + s.getPatientId() + " 您的问题：" + prompt
                + "\n建议：保持冷静、优先查看患者常规轨迹点、联系监护成员协同寻回。";
    }
}
