package com.xiaohelab.guard.server.gov.entity;

import com.xiaohelab.guard.server.common.entity.AuditOnlyEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

@Entity
@Table(name = "sys_log")
public class SysLogEntity extends AuditOnlyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "module", length = 64, nullable = false)
    private String module;

    @Column(name = "action", length = 64, nullable = false)
    private String action;

    /** USER / AI_AGENT / SYSTEM */
    @Column(name = "action_source", length = 20, nullable = false)
    private String actionSource = "USER";

    @Column(name = "operator_user_id")
    private Long operatorUserId;

    @Column(name = "operator_username", length = 64)
    private String operatorUsername;

    @Column(name = "object_id", length = 64)
    private String objectId;

    /** SUCCESS / FAIL */
    @Column(name = "result", length = 20, nullable = false)
    private String result;

    @Column(name = "result_code", length = 64)
    private String resultCode;

    @Column(name = "risk_level", length = 20)
    private String riskLevel;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "detail", columnDefinition = "jsonb")
    private String detail;

    @Column(name = "agent_profile", length = 64)
    private String agentProfile;

    @Column(name = "execution_mode", length = 20)
    private String executionMode;

    @Column(name = "confirm_level", length = 20)
    private String confirmLevel;

    @Column(name = "blocked_reason", length = 128)
    private String blockedReason;

    @Column(name = "client_ip", length = 64)
    private String clientIp;

    @Column(name = "request_id", length = 64)
    private String requestId;

    @Column(name = "executed_at")
    private OffsetDateTime executedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getModule() { return module; }
    public void setModule(String module) { this.module = module; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getActionSource() { return actionSource; }
    public void setActionSource(String actionSource) { this.actionSource = actionSource; }
    public Long getOperatorUserId() { return operatorUserId; }
    public void setOperatorUserId(Long operatorUserId) { this.operatorUserId = operatorUserId; }
    public String getOperatorUsername() { return operatorUsername; }
    public void setOperatorUsername(String operatorUsername) { this.operatorUsername = operatorUsername; }
    public String getObjectId() { return objectId; }
    public void setObjectId(String objectId) { this.objectId = objectId; }
    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }
    public String getResultCode() { return resultCode; }
    public void setResultCode(String resultCode) { this.resultCode = resultCode; }
    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
    public String getAgentProfile() { return agentProfile; }
    public void setAgentProfile(String agentProfile) { this.agentProfile = agentProfile; }
    public String getExecutionMode() { return executionMode; }
    public void setExecutionMode(String executionMode) { this.executionMode = executionMode; }
    public String getConfirmLevel() { return confirmLevel; }
    public void setConfirmLevel(String confirmLevel) { this.confirmLevel = confirmLevel; }
    public String getBlockedReason() { return blockedReason; }
    public void setBlockedReason(String blockedReason) { this.blockedReason = blockedReason; }
    public String getClientIp() { return clientIp; }
    public void setClientIp(String clientIp) { this.clientIp = clientIp; }
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public OffsetDateTime getExecutedAt() { return executedAt; }
    public void setExecutedAt(OffsetDateTime executedAt) { this.executedAt = executedAt; }
}
