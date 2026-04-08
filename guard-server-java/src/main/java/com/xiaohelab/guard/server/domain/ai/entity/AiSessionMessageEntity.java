package com.xiaohelab.guard.server.domain.ai.entity;

import com.xiaohelab.guard.server.infrastructure.persistence.do_.AiSessionMessageDO;
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

    public static AiSessionMessageEntity fromDO(AiSessionMessageDO d) {
        AiSessionMessageEntity e = new AiSessionMessageEntity();
        e.id = d.getId();
        e.sessionId = d.getSessionId();
        e.sequenceNo = d.getSequenceNo();
        e.role = d.getRole();
        e.content = d.getContent();
        e.tokenUsage = d.getTokenUsage();
        e.createdAt = d.getCreatedAt();
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

    public AiSessionMessageDO toDO() {
        AiSessionMessageDO d = new AiSessionMessageDO();
        d.setId(this.id);
        d.setSessionId(this.sessionId);
        d.setSequenceNo(this.sequenceNo);
        d.setRole(this.role);
        d.setContent(this.content);
        d.setTokenUsage(this.tokenUsage);
        return d;
    }
}
