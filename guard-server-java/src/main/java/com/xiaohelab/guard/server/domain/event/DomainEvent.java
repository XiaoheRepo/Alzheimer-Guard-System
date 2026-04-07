package com.xiaohelab.guard.server.domain.event;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

/**
 * 领域事件基类。
 * 所有领域状态事件须走 Outbox（HC-02 约束）。
 * eventType 嵌入 payload 的 "event_type" 字段，同时作为 Kafka Header 透传。
 */
@Getter
@Builder
public class DomainEvent {

    /** 事件唯一 ID（UUID-like，由 IdGenerator.eventId() 生成） */
    private final String eventId;

    /** Kafka Topic 名称 */
    private final String topic;

    /** 聚合根 ID（如 taskId、patientId，对应 sys_outbox_log.aggregate_id） */
    private final String aggregateId;

    /** 分区键（保证同患者事件顺序消费，通常为 patient_id） */
    private final String partitionKey;

    /** 事件类型（如 task.created / clue.reported.raw，嵌入 payload） */
    private final String eventType;

    /** 触发此事件的业务请求 ID（HC-03 幂等追踪） */
    @Builder.Default
    private final String requestId = "";

    /** 全链路追踪 ID（HC-04） */
    @Builder.Default
    private final String traceId = "";

    /** 事件发生时间 */
    private final Instant occurredAt;

    /**
     * 事件 Payload（JSON 字符串，序列化后写入 Outbox）。
     * 格式：包含 event_type 字段使消费端无需读 Header 即可判断事件类型。
     */
    private final String payload;
}
