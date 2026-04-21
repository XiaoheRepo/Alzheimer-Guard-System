package com.xiaohelab.guard.server.ai.entity;

import com.xiaohelab.guard.server.common.entity.BaseEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

@Entity
@Table(name = "ai_intent")
public class AiIntentEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "intent_id", length = 64, nullable = false, unique = true)
    private String intentId;

    @Column(name = "session_id", length = 64, nullable = false)
    private String sessionId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "action", length = 64, nullable = false)
    private String action;

    @Column(name = "description", length = 500)
    private String description;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "parameters", columnDefinition = "jsonb")
    private String parameters;

    /** READ_ONLY / CONFIRM_1 / CONFIRM_2 */
    @Column(name = "execution_level", length = 20, nullable = false)
    private String executionLevel = "CONFIRM_1";

    @Column(name = "requires_confirm", nullable = false)
    private Boolean requiresConfirm = true;

    /** PENDING / APPROVED / REJECTED / EXPIRED */
    @Column(name = "status", length = 20, nullable = false)
    private String status = "PENDING";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "execution_result", columnDefinition = "jsonb")
    private String executionResult;

    @Column(name = "expire_at", nullable = false)
    private OffsetDateTime expireAt;

    @Column(name = "processed_at")
    private OffsetDateTime processedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getIntentId() { return intentId; }
    public void setIntentId(String intentId) { this.intentId = intentId; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getParameters() { return parameters; }
    public void setParameters(String parameters) { this.parameters = parameters; }
    public String getExecutionLevel() { return executionLevel; }
    public void setExecutionLevel(String executionLevel) { this.executionLevel = executionLevel; }
    public Boolean getRequiresConfirm() { return requiresConfirm; }
    public void setRequiresConfirm(Boolean requiresConfirm) { this.requiresConfirm = requiresConfirm; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getExecutionResult() { return executionResult; }
    public void setExecutionResult(String executionResult) { this.executionResult = executionResult; }
    public OffsetDateTime getExpireAt() { return expireAt; }
    public void setExpireAt(OffsetDateTime expireAt) { this.expireAt = expireAt; }
    public OffsetDateTime getProcessedAt() { return processedAt; }
    public void setProcessedAt(OffsetDateTime processedAt) { this.processedAt = processedAt; }
}
