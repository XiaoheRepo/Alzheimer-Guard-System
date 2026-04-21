package com.xiaohelab.guard.server.outbox.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.OffsetDateTime;

/**
 * Outbox 事件日志。HC-02：业务变更与事件发布同事务落库，由 {@code OutboxDispatcher}
 * 异步抢占并发布到 MQ（Kafka / Redis Streams）。
 * sys_outbox_log 表无 version 列，所以不继承 BaseEntity。
 */
@Entity
@Table(name = "sys_outbox_log")
@EntityListeners(AuditingEntityListener.class)
public class OutboxLogEntity {

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "trace_id", length = 64, nullable = false)
    private String traceId;

    @PrePersist
    void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", length = 64, nullable = false, unique = true)
    private String eventId;

    @Column(name = "topic", length = 128, nullable = false)
    private String topic;

    @Column(name = "aggregate_id", length = 64, nullable = false)
    private String aggregateId;

    @Column(name = "partition_key", length = 64, nullable = false)
    private String partitionKey;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private String payload;

    @Column(name = "request_id", length = 64)
    private String requestId;

    @Column(name = "phase", length = 20, nullable = false)
    private String phase = "PENDING";

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 0;

    @Column(name = "next_retry_at")
    private OffsetDateTime nextRetryAt;

    @Column(name = "lease_owner", length = 64)
    private String leaseOwner;

    @Column(name = "lease_until")
    private OffsetDateTime leaseUntil;

    @Column(name = "sent_at")
    private OffsetDateTime sentAt;

    @Column(name = "last_error", length = 512)
    private String lastError;

    @Column(name = "last_intervention_by")
    private Long lastInterventionBy;

    @Column(name = "last_intervention_at")
    private OffsetDateTime lastInterventionAt;

    @Column(name = "replay_reason", length = 256)
    private String replayReason;

    @Column(name = "replay_token", length = 64)
    private String replayToken;

    @Column(name = "replayed_at")
    private OffsetDateTime replayedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }
    public String getAggregateId() { return aggregateId; }
    public void setAggregateId(String aggregateId) { this.aggregateId = aggregateId; }
    public String getPartitionKey() { return partitionKey; }
    public void setPartitionKey(String partitionKey) { this.partitionKey = partitionKey; }
    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public String getPhase() { return phase; }
    public void setPhase(String phase) { this.phase = phase; }
    public Integer getRetryCount() { return retryCount; }
    public void setRetryCount(Integer retryCount) { this.retryCount = retryCount; }
    public OffsetDateTime getNextRetryAt() { return nextRetryAt; }
    public void setNextRetryAt(OffsetDateTime nextRetryAt) { this.nextRetryAt = nextRetryAt; }
    public String getLeaseOwner() { return leaseOwner; }
    public void setLeaseOwner(String leaseOwner) { this.leaseOwner = leaseOwner; }
    public OffsetDateTime getLeaseUntil() { return leaseUntil; }
    public void setLeaseUntil(OffsetDateTime leaseUntil) { this.leaseUntil = leaseUntil; }
    public OffsetDateTime getSentAt() { return sentAt; }
    public void setSentAt(OffsetDateTime sentAt) { this.sentAt = sentAt; }
    public String getLastError() { return lastError; }
    public void setLastError(String lastError) { this.lastError = lastError; }
    public Long getLastInterventionBy() { return lastInterventionBy; }
    public void setLastInterventionBy(Long lastInterventionBy) { this.lastInterventionBy = lastInterventionBy; }
    public OffsetDateTime getLastInterventionAt() { return lastInterventionAt; }
    public void setLastInterventionAt(OffsetDateTime lastInterventionAt) { this.lastInterventionAt = lastInterventionAt; }
    public String getReplayReason() { return replayReason; }
    public void setReplayReason(String replayReason) { this.replayReason = replayReason; }
    public String getReplayToken() { return replayToken; }
    public void setReplayToken(String replayToken) { this.replayToken = replayToken; }
    public OffsetDateTime getReplayedAt() { return replayedAt; }
    public void setReplayedAt(OffsetDateTime replayedAt) { this.replayedAt = replayedAt; }
}
