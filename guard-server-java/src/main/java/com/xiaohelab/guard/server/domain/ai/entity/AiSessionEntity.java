package com.xiaohelab.guard.server.domain.ai.entity;

import com.xiaohelab.guard.server.infrastructure.persistence.do_.AiSessionDO;
import lombok.Getter;

import java.time.Instant;

/**
 * AI 会话聚合根。
 * 状态机：ACTIVE → ARCHIVED（不可逆）。
 * messages / tokenUsage 为 JSONB 序列化文本，应用层负责解析。
 */
@Getter
public class AiSessionEntity {

    private Long id;
    private String sessionId;
    private Long userId;
    private Long patientId;
    private Long taskId;
    private String messages;
    private Integer requestTokens;
    private Integer responseTokens;
    private String tokenUsage;
    private Integer tokenUsed;
    private String modelName;
    /** ACTIVE / ARCHIVED */
    private String status;
    private Instant archivedAt;
    /** 乐观锁版本，CAS 更新使用 */
    private Long version;
    private Instant createdAt;
    private Instant updatedAt;

    private AiSessionEntity() {}

    public static AiSessionEntity fromDO(AiSessionDO d) {
        AiSessionEntity e = new AiSessionEntity();
        e.id = d.getId();
        e.sessionId = d.getSessionId();
        e.userId = d.getUserId();
        e.patientId = d.getPatientId();
        e.taskId = d.getTaskId();
        e.messages = d.getMessages();
        e.requestTokens = d.getRequestTokens();
        e.responseTokens = d.getResponseTokens();
        e.tokenUsage = d.getTokenUsage();
        e.tokenUsed = d.getTokenUsed();
        e.modelName = d.getModelName();
        e.status = d.getStatus();
        e.archivedAt = d.getArchivedAt();
        e.version = d.getVersion();
        e.createdAt = d.getCreatedAt();
        e.updatedAt = d.getUpdatedAt();
        return e;
    }

    public static AiSessionEntity create(String sessionId, Long userId, Long patientId,
                                         Long taskId, String modelName) {
        AiSessionEntity e = new AiSessionEntity();
        e.sessionId = sessionId;
        e.userId = userId;
        e.patientId = patientId;
        e.taskId = taskId;
        e.messages = "[]";
        e.requestTokens = 0;
        e.responseTokens = 0;
        e.tokenUsage = "{}";
        e.tokenUsed = 0;
        e.modelName = modelName;
        e.status = "ACTIVE";
        e.version = 1L;
        return e;
    }

    public AiSessionDO toDO() {
        AiSessionDO d = new AiSessionDO();
        d.setId(this.id);
        d.setSessionId(this.sessionId);
        d.setUserId(this.userId);
        d.setPatientId(this.patientId);
        d.setTaskId(this.taskId);
        d.setMessages(this.messages);
        d.setRequestTokens(this.requestTokens);
        d.setResponseTokens(this.responseTokens);
        d.setTokenUsage(this.tokenUsage);
        d.setTokenUsed(this.tokenUsed);
        d.setModelName(this.modelName);
        d.setStatus(this.status);
        d.setArchivedAt(this.archivedAt);
        d.setVersion(this.version);
        return d;
    }

    /** 归档会话（ACTIVE → ARCHIVED，不可逆）。 */
    public void archive() {
        this.status = "ARCHIVED";
        this.archivedAt = Instant.now();
    }
}
