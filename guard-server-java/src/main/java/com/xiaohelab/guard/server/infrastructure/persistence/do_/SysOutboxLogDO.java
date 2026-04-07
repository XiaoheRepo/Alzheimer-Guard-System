package com.xiaohelab.guard.server.infrastructure.persistence.do_;

import lombok.Data;

import java.time.Instant;

/**
 * sys_outbox_log 持久化对象（Outbox 事件调度表）。
 * HC-02：核心状态变更必须本地事务 + Outbox 同提交。
 * 状态流转：PENDING → DISPATCHING → SENT；失败 → RETRY → DEAD。
 */
@Data
public class SysOutboxLogDO {

    /** 事件 ID，与 createdAt 组成联合主键（分区表） */
    private String eventId;
    private String topic;
    private String aggregateId;
    private String partitionKey;
    /** 事件载荷 JSON */
    private String payload;
    /** 幂等键，对应 X-Request-Id */
    private String requestId;
    private String traceId;
    /** PENDING / DISPATCHING / SENT / RETRY / DEAD */
    private String phase;
    private Integer retryCount;
    private Instant nextRetryAt;
    /** 分区锁：抢占此 Outbox 分区的实例 ID */
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
}
