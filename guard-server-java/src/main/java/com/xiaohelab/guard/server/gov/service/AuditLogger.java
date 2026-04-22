package com.xiaohelab.guard.server.gov.service;

import com.xiaohelab.guard.server.common.security.AuthUser;
import com.xiaohelab.guard.server.common.security.SecurityUtil;
import com.xiaohelab.guard.server.common.util.JsonUtil;
import com.xiaohelab.guard.server.common.util.TraceIdUtil;
import com.xiaohelab.guard.server.gov.entity.SysLogEntity;
import com.xiaohelab.guard.server.gov.repository.SysLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * 审计日志落库服务（HC-03）。
 * <p>必须在调用方业务事务内使用（{@link Propagation#MANDATORY}），以保证状态变更与审计写入的强一致。</p>
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
    @Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
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
        e.setRequestId(TraceIdUtil.currentRequestId());
        e.setTraceId(TraceIdUtil.currentTraceId());
        if (detail != null && !detail.isEmpty()) {
            try {
                e.setDetail(JsonUtil.toJson(detail));
            } catch (Exception jsonErr) {
                log.warn("[Audit] detail 序列化失败 action={} err={}", action, jsonErr.getMessage());
            }
        }
        sysLogRepository.save(e);
    }
}
