package com.xiaohelab.guard.server.domain.governance.entity;

import java.time.Instant;

/**
 * Outbox 事件领域实体（治理域）。
 * 用于 DEAD 队列查询与受控重放场景。
 */
public class OutboxEventEntity {

    private String eventId;
    private String topic;
    private String aggregateId;
    private String partitionKey;
    private String payload;
    private String requestId;
    private String traceId;
    private String phase;
    private Integer retryCount;
    private Instant nextRetryAt;
    private String leaseOwner;
    private Instant leaseUntil;
    private Instant sentAt;
    private String lastError;
    private Long lastInterventionBy;
    private Instant lastInterventionAt;
    private String replayReason;
    private String replayToken;
    private Instant replayedAt;
    private Instant createdAt;
    private Instant updatedAt;

    private OutboxEventEntity() {}

    /** 从持久层恢复 */
    public static OutboxEventEntity reconstitute(
            String eventId, String topic, String aggregateId, String partitionKey,
            String payload, String requestId, String traceId, String phase,
            Integer retryCount, Instant nextRetryAt, String leaseOwner, Instant leaseUntil,
            Instant sentAt, String lastError, Long lastInterventionBy,
            Instant lastInterventionAt, String replayReason, String replayToken,
            Instant replayedAt, Instant createdAt, Instant updatedAt) {
        OutboxEventEntity e = new OutboxEventEntity();
        e.eventId = eventId;
        e.topic = topic;
        e.aggregateId = aggregateId;
        e.partitionKey = partitionKey;
        e.payload = payload;
        e.requestId = requestId;
        e.traceId = traceId;
        e.phase = phase;
        e.retryCount = retryCount;
        e.nextRetryAt = nextRetryAt;
        e.leaseOwner = leaseOwner;
        e.leaseUntil = leaseUntil;
        e.sentAt = sentAt;
        e.lastError = lastError;
        e.lastInterventionBy = lastInterventionBy;
        e.lastInterventionAt = lastInterventionAt;
        e.replayReason = replayReason;
        e.replayToken = replayToken;
        e.replayedAt = replayedAt;
        e.createdAt = createdAt;
        e.updatedAt = updatedAt;
        return e;
    }

    // ===== Getters =====

    public String getEventId() { return eventId; }
    public String getTopic() { return topic; }
    public String getAggregateId() { return aggregateId; }
    public String getPartitionKey() { return partitionKey; }
    public String getPayload() { return payload; }
    public String getRequestId() { return requestId; }
    public String getTraceId() { return traceId; }
    public String getPhase() { return phase; }
    public Integer getRetryCount() { return retryCount; }
    public Instant getNextRetryAt() { return nextRetryAt; }
    public String getLeaseOwner() { return leaseOwner; }
    public Instant getLeaseUntil() { return leaseUntil; }
    public Instant getSentAt() { return sentAt; }
    public String getLastError() { return lastError; }
    public Long getLastInterventionBy() { return lastInterventionBy; }
    public Instant getLastInterventionAt() { return lastInterventionAt; }
    public String getReplayReason() { return replayReason; }
    public String getReplayToken() { return replayToken; }
    public Instant getReplayedAt() { return replayedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
