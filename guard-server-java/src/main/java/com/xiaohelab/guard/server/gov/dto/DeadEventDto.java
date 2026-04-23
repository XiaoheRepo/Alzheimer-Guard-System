package com.xiaohelab.guard.server.gov.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.xiaohelab.guard.server.outbox.entity.OutboxLogEntity;

import java.time.OffsetDateTime;

/** API_V2.0.md §3.6.8 — Outbox DEAD 事件摘要，面向 SUPER_ADMIN 管理台。 */
public class DeadEventDto {

    @JsonProperty("event_id")
    private String eventId;
    private String topic;
    @JsonProperty("partition_key")
    private String partitionKey;
    @JsonProperty("fail_count")
    private Integer failCount;
    @JsonProperty("first_fail_at")
    private OffsetDateTime firstFailAt;
    @JsonProperty("last_error")
    private String lastError;
    private String payload;
    @JsonProperty("trace_id")
    private String traceId;
    /** 是否处于等待重放（phase=RETRY）状态 */
    private boolean replaying;

    public static DeadEventDto from(OutboxLogEntity e) {
        DeadEventDto dto = new DeadEventDto();
        dto.eventId    = e.getEventId();
        dto.topic      = e.getTopic();
        dto.partitionKey = e.getPartitionKey();
        dto.failCount  = e.getRetryCount();
        dto.firstFailAt = e.getCreatedAt();
        dto.lastError  = e.getLastError();
        dto.payload    = e.getPayload();
        dto.traceId    = e.getTraceId();
        dto.replaying  = "RETRY".equals(e.getPhase());
        return dto;
    }

    public String getEventId() { return eventId; }
    public String getTopic() { return topic; }
    public String getPartitionKey() { return partitionKey; }
    public Integer getFailCount() { return failCount; }
    public OffsetDateTime getFirstFailAt() { return firstFailAt; }
    public String getLastError() { return lastError; }
    public String getPayload() { return payload; }
    public String getTraceId() { return traceId; }
    public boolean isReplaying() { return replaying; }
}
