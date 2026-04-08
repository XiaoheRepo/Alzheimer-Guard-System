package com.xiaohelab.guard.server.domain.ai.entity;

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

    /** 从持久化数据重建（仅 Infrastructure 层 RepositoryImpl 调用）。 */
    public static AiSessionEntity reconstitute(
            Long id, String sessionId, Long userId, Long patientId, Long taskId,
            String messages, Integer requestTokens, Integer responseTokens,
            String tokenUsage, Integer tokenUsed, String modelName,
            String status, Instant archivedAt, Long version,
            Instant createdAt, Instant updatedAt) {
        AiSessionEntity e = new AiSessionEntity();
        e.id = id;
        e.sessionId = sessionId;
        e.userId = userId;
        e.patientId = patientId;
        e.taskId = taskId;
        e.messages = messages;
        e.requestTokens = requestTokens;
        e.responseTokens = responseTokens;
        e.tokenUsage = tokenUsage;
        e.tokenUsed = tokenUsed;
        e.modelName = modelName;
        e.status = status;
        e.archivedAt = archivedAt;
        e.version = version;
        e.createdAt = createdAt;
        e.updatedAt = updatedAt;
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

    /** 归档会话（ACTIVE → ARCHIVED，不可逆）。 */
    public void archive() {
        this.status = "ARCHIVED";
        this.archivedAt = Instant.now();
    }
}
