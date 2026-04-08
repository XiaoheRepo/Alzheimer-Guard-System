package com.xiaohelab.guard.server.domain.ai.entity;

import lombok.Getter;

import java.time.Instant;

/**
 * AI 会话消息值对象（无独立生命周期，隶属于 AiSession 聚合）。
 * role: user / assistant / system
 */
@Getter
public class AiSessionMessageEntity {

    private Long id;
    private String sessionId;
    private Integer sequenceNo;
    /** user / assistant / system */
    private String role;
    private String content;
    private String tokenUsage;
    private Instant createdAt;

    private AiSessionMessageEntity() {}

    /** 从持久化数据重建（仅 Infrastructure 层 RepositoryImpl 调用）。 */
    public static AiSessionMessageEntity reconstitute(
            Long id, String sessionId, Integer sequenceNo,
            String role, String content, String tokenUsage, Instant createdAt) {
        AiSessionMessageEntity e = new AiSessionMessageEntity();
        e.id = id;
        e.sessionId = sessionId;
        e.sequenceNo = sequenceNo;
        e.role = role;
        e.content = content;
        e.tokenUsage = tokenUsage;
        e.createdAt = createdAt;
        return e;
    }

    public static AiSessionMessageEntity create(String sessionId, int sequenceNo,
                                                String role, String content, String tokenUsage) {
        AiSessionMessageEntity e = new AiSessionMessageEntity();
        e.sessionId = sessionId;
        e.sequenceNo = sequenceNo;
        e.role = role;
        e.content = content;
        e.tokenUsage = tokenUsage != null ? tokenUsage : "{}";
        return e;
    }
}
