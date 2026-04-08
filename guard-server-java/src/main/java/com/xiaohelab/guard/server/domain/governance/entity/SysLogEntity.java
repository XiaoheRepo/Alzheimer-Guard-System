package com.xiaohelab.guard.server.domain.governance.entity;

import java.time.Instant;

/**
 * 审计日志领域实体（治理域）。
 * 审计记录只写不改，不包含状态机。
 */
public class SysLogEntity {

    private Long id;
    private String module;
    private String action;
    private String actionId;
    private String resultCode;
    private Instant executedAt;
    private Long operatorUserId;
    private String operatorUsername;
    private String objectId;
    private String result;
    private String riskLevel;
    private String detail;
    private String actionSource;
    private String agentProfile;
    private String executionMode;
    private String confirmLevel;
    private String blockedReason;
    private String requestId;
    private String traceId;
    private Instant createdAt;

    private SysLogEntity() {}

    // ===== 工厂方法：新建审计记录 =====

    public static SysLogEntity create(String module, String action, String actionId,
                                      String objectId, String resultCode, String result,
                                      String riskLevel, Long operatorUserId,
                                      String operatorUsername, String detail,
                                      String actionSource, String executionMode,
                                      String requestId, String traceId) {
        SysLogEntity e = new SysLogEntity();
        e.module = module;
        e.action = action;
        e.actionId = actionId;
        e.objectId = objectId;
        e.resultCode = resultCode;
        e.result = result;
        e.riskLevel = riskLevel;
        e.operatorUserId = operatorUserId;
        e.operatorUsername = operatorUsername;
        e.detail = detail;
        e.actionSource = actionSource;
        e.executionMode = executionMode;
        e.requestId = requestId;
        e.traceId = traceId;
        e.executedAt = Instant.now();
        return e;
    }

    // ===== 重建方法：从持久层恢复 =====

    public static SysLogEntity reconstitute(Long id, String module, String action,
                                            String actionId, String resultCode,
                                            Instant executedAt, Long operatorUserId,
                                            String operatorUsername, String objectId,
                                            String result, String riskLevel, String detail,
                                            String actionSource, String agentProfile,
                                            String executionMode, String confirmLevel,
                                            String blockedReason, String requestId,
                                            String traceId, Instant createdAt) {
        SysLogEntity e = new SysLogEntity();
        e.id = id;
        e.module = module;
        e.action = action;
        e.actionId = actionId;
        e.resultCode = resultCode;
        e.executedAt = executedAt;
        e.operatorUserId = operatorUserId;
        e.operatorUsername = operatorUsername;
        e.objectId = objectId;
        e.result = result;
        e.riskLevel = riskLevel;
        e.detail = detail;
        e.actionSource = actionSource;
        e.agentProfile = agentProfile;
        e.executionMode = executionMode;
        e.confirmLevel = confirmLevel;
        e.blockedReason = blockedReason;
        e.requestId = requestId;
        e.traceId = traceId;
        e.createdAt = createdAt;
        return e;
    }

    // ===== Getters =====

    public Long getId() { return id; }
    public String getModule() { return module; }
    public String getAction() { return action; }
    public String getActionId() { return actionId; }
    public String getResultCode() { return resultCode; }
    public Instant getExecutedAt() { return executedAt; }
    public Long getOperatorUserId() { return operatorUserId; }
    public String getOperatorUsername() { return operatorUsername; }
    public String getObjectId() { return objectId; }
    public String getResult() { return result; }
    public String getRiskLevel() { return riskLevel; }
    public String getDetail() { return detail; }
    public String getActionSource() { return actionSource; }
    public String getAgentProfile() { return agentProfile; }
    public String getExecutionMode() { return executionMode; }
    public String getConfirmLevel() { return confirmLevel; }
    public String getBlockedReason() { return blockedReason; }
    public String getRequestId() { return requestId; }
    public String getTraceId() { return traceId; }
    public Instant getCreatedAt() { return createdAt; }
}
