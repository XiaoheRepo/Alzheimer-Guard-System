package com.xiaohelab.guard.server.ai.entity;

import com.xiaohelab.guard.server.common.entity.BaseEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

@Entity
@Table(name = "ai_session")
public class AiSessionEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", length = 64, nullable = false, unique = true)
    private String sessionId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "task_id")
    private Long taskId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "messages", nullable = false, columnDefinition = "jsonb")
    private String messages = "[]";

    @Column(name = "prompt_tokens", nullable = false)
    private Integer promptTokens = 0;

    @Column(name = "completion_tokens", nullable = false)
    private Integer completionTokens = 0;

    @Column(name = "total_tokens", nullable = false)
    private Integer totalTokens = 0;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "token_usage", nullable = false, columnDefinition = "jsonb")
    private String tokenUsage = "{}";

    @Column(name = "model_name", length = 64, nullable = false)
    private String modelName = "qwen-max-latest";

    /** ACTIVE / ARCHIVED */
    @Column(name = "status", length = 20, nullable = false)
    private String status = "ACTIVE";

    @Column(name = "feedback_rating")
    private Integer feedbackRating;

    @Column(name = "feedback_comment", length = 1000)
    private String feedbackComment;

    @Column(name = "feedback_at")
    private OffsetDateTime feedbackAt;

    @Column(name = "archived_at")
    private OffsetDateTime archivedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }
    public String getMessages() { return messages; }
    public void setMessages(String messages) { this.messages = messages; }
    public Integer getPromptTokens() { return promptTokens; }
    public void setPromptTokens(Integer promptTokens) { this.promptTokens = promptTokens; }
    public Integer getCompletionTokens() { return completionTokens; }
    public void setCompletionTokens(Integer completionTokens) { this.completionTokens = completionTokens; }
    public Integer getTotalTokens() { return totalTokens; }
    public void setTotalTokens(Integer totalTokens) { this.totalTokens = totalTokens; }
    public String getTokenUsage() { return tokenUsage; }
    public void setTokenUsage(String tokenUsage) { this.tokenUsage = tokenUsage; }
    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getFeedbackRating() { return feedbackRating; }
    public void setFeedbackRating(Integer feedbackRating) { this.feedbackRating = feedbackRating; }
    public String getFeedbackComment() { return feedbackComment; }
    public void setFeedbackComment(String feedbackComment) { this.feedbackComment = feedbackComment; }
    public OffsetDateTime getFeedbackAt() { return feedbackAt; }
    public void setFeedbackAt(OffsetDateTime feedbackAt) { this.feedbackAt = feedbackAt; }
    public OffsetDateTime getArchivedAt() { return archivedAt; }
    public void setArchivedAt(OffsetDateTime archivedAt) { this.archivedAt = archivedAt; }
}
