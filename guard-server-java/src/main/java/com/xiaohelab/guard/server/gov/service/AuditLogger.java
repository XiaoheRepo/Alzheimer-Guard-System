package com.xiaohelab.guard.server.gov.service;

import com.xiaohelab.guard.server.common.security.AuthUser;
import com.xiaohelab.guard.server.common.security.SecurityUtil;
import com.xiaohelab.guard.server.common.util.JsonUtil;
import com.xiaohelab.guard.server.common.util.TraceIdUtil;
import com.xiaohelab.guard.server.gov.entity.SysLogEntity;
import com.xiaohelab.guard.server.gov.repository.SysLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * 审计日志落库服务（HC-03）。
 * <p>使用 {@link Propagation#REQUIRES_NEW} 开独立写事务，避免调用方只读事务导致 INSERT 失败。</p>
 *
 * <p>审计字段规范见 {@code doc/backend_handbook_v2.md §12.5 / §24.7}：</p>
 * <ul>
 *   <li>{@code module}  — 领域名（GOV / PROFILE / MAT / TASK / CLUE / AI）</li>
 *   <li>{@code action}  — 动作白名单（如 admin.user.disable）</li>
 *   <li>{@code riskLevel} — LOW / MEDIUM / HIGH / CRITICAL</li>
 *   <li>{@code confirmLevel} — CONFIRM_1 / CONFIRM_2 / CONFIRM_3</li>
 *   <li>{@code detail}  — JSON 扩展（操作原因、前后快照、目标 id 等）</li>
 * </ul>
 */
@Service
public class AuditLogger {

    private static final Logger log = LoggerFactory.getLogger(AuditLogger.class);

    private final SysLogRepository sysLogRepository;

    public AuditLogger(SysLogRepository sysLogRepository) {
        this.sysLogRepository = sysLogRepository;
    }

    /**
     * 成功操作审计。
     *
     * @param module       领域模块
     * @param action       动作白名单编码
     * @param objectId     被操作的聚合根业务 id（可空）
     * @param riskLevel    风险级别（LOW/MEDIUM/HIGH/CRITICAL）
     * @param confirmLevel 确认等级（CONFIRM_1/2/3，可空）
     * @param detail       扩展详情（自动序列化为 JSON）
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void logSuccess(String module,
                           String action,
                           String objectId,
                           String riskLevel,
                           String confirmLevel,
                           Map<String, Object> detail) {
        SysLogEntity e = new SysLogEntity();
        e.setModule(module);
        e.setAction(action);
        e.setActionSource("USER");
        e.setResult("SUCCESS");
        e.setObjectId(objectId);
        e.setRiskLevel(riskLevel);
        e.setConfirmLevel(confirmLevel);
        e.setExecutedAt(OffsetDateTime.now());

        AuthUser au = SecurityUtil.currentOrNull();
        if (au != null) {
            e.setOperatorUserId(au.getUserId());
            e.setOperatorUsername(au.getUsername());
        }

        // trace_id 来自 MDC（TraceIdFilter 注入）
        e.setTraceId(TraceIdUtil.currentTraceId());

        // request_id：优先从 MDC 取（前端传了 X-Request-Id 时有值），否则自动生成一个
        String requestId = TraceIdUtil.currentRequestId();
        if (requestId == null || requestId.isBlank()) {
            requestId = "req_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        }
        e.setRequestId(requestId);

        // 客户端 IP：从当前请求上下文取，兼容 X-Forwarded-For 代理透传
        e.setClientIp(resolveClientIp());

        if (detail != null && !detail.isEmpty()) {
            try {
                e.setDetail(JsonUtil.toJson(detail));
            } catch (Exception jsonErr) {
                log.warn("[Audit] detail 序列化失败 action={} err={}", action, jsonErr.getMessage());
            }
        }
        sysLogRepository.save(e);
    }

    /** 解析客户端真实 IP，支持 X-Forwarded-For / X-Real-IP 代理头。 */
    private String resolveClientIp() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) return null;
            HttpServletRequest req = attrs.getRequest();
            String xff = req.getHeader("X-Forwarded-For");
            if (xff != null && !xff.isBlank()) {
                return xff.split(",")[0].trim();
            }
            String realIp = req.getHeader("X-Real-IP");
            if (realIp != null && !realIp.isBlank()) {
                return realIp.trim();
            }
            return req.getRemoteAddr();
        } catch (Exception ex) {
            return null;
        }
    }
}

