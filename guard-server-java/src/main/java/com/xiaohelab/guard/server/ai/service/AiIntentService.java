package com.xiaohelab.guard.server.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaohelab.guard.server.ai.entity.AiIntentEntity;
import com.xiaohelab.guard.server.ai.repository.AiIntentRepository;
import com.xiaohelab.guard.server.common.error.ErrorCode;
import com.xiaohelab.guard.server.common.exception.BizException;
import com.xiaohelab.guard.server.common.security.AuthUser;
import com.xiaohelab.guard.server.common.security.SecurityUtil;
import com.xiaohelab.guard.server.common.util.BusinessNoUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Map;

/** AI 意图双重确认：Agent 提出意图 → 用户 APPROVE/REJECT。 */
@Service
public class AiIntentService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final AiIntentRepository intentRepository;

    public AiIntentService(AiIntentRepository intentRepository) {
        this.intentRepository = intentRepository;
    }

    @Transactional(rollbackFor = Exception.class)
    public AiIntentEntity propose(String sessionId, String action, String description,
                                  Map<String, Object> parameters, String executionLevel) {
        AuthUser user = SecurityUtil.current();
        AiIntentEntity i = new AiIntentEntity();
        i.setIntentId(BusinessNoUtil.intentId());
        i.setSessionId(sessionId);
        i.setUserId(user.getUserId());
        i.setAction(action);
        i.setDescription(description);
        i.setExecutionLevel(executionLevel != null ? executionLevel : "CONFIRM_1");
        i.setRequiresConfirm(!"READ_ONLY".equals(i.getExecutionLevel()));
        i.setStatus("PENDING");
        i.setExpireAt(OffsetDateTime.now().plusMinutes(10));
        try {
            i.setParameters(parameters != null ? MAPPER.writeValueAsString(parameters) : "{}");
        } catch (JsonProcessingException e) {
            throw BizException.of(ErrorCode.E_SYS_5000, "parameters 序列化失败");
        }
        intentRepository.save(i);
        return i;
    }

    @Transactional(rollbackFor = Exception.class)
    public AiIntentEntity confirm(String intentId, String decision, Map<String, Object> executionResult) {
        AuthUser user = SecurityUtil.current();
        AiIntentEntity i = intentRepository.findByIntentId(intentId)
                .orElseThrow(() -> BizException.of(ErrorCode.E_AI_4041));
        if (!i.getUserId().equals(user.getUserId())) {
            throw BizException.of(ErrorCode.E_AI_4033);
        }
        if (!"PENDING".equals(i.getStatus())) {
            throw BizException.of(ErrorCode.E_AI_4091);
        }
        if (i.getExpireAt().isBefore(OffsetDateTime.now())) {
            i.setStatus("EXPIRED");
            intentRepository.save(i);
            throw BizException.of(ErrorCode.E_AI_4091);
        }
        if ("APPROVE".equalsIgnoreCase(decision)) {
            i.setStatus("APPROVED");
            if (executionResult != null) {
                try { i.setExecutionResult(MAPPER.writeValueAsString(executionResult)); }
                catch (JsonProcessingException e) { throw BizException.of(ErrorCode.E_SYS_5000, "result 序列化失败"); }
            }
        } else if ("REJECT".equalsIgnoreCase(decision)) {
            i.setStatus("REJECTED");
        } else {
            throw BizException.of(ErrorCode.E_REQ_4220);
        }
        i.setProcessedAt(OffsetDateTime.now());
        intentRepository.save(i);
        return i;
    }

    public AiIntentEntity get(String intentId) {
        AuthUser user = SecurityUtil.current();
        AiIntentEntity i = intentRepository.findByIntentId(intentId)
                .orElseThrow(() -> BizException.of(ErrorCode.E_AI_4041));
        if (!user.isAdmin() && !i.getUserId().equals(user.getUserId())) {
            throw BizException.of(ErrorCode.E_AI_4033);
        }
        return i;
    }
}
