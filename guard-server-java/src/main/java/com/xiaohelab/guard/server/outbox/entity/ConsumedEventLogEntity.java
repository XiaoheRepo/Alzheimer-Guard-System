package com.xiaohelab.guard.server.outbox.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "consumed_event_log")
public class ConsumedEventLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "consumer_name", length = 64, nullable = false)
    private String consumerName;

    @Column(name = "topic", length = 128, nullable = false)
    private String topic;

    @Column(name = "event_id", length = 64, nullable = false)
    private String eventId;

    @Column(name = "partition_no")
    private Integer partitionNo;

    @Column(name = "msg_offset")
    private Long msgOffset;

    @Column(name = "trace_id", length = 64, nullable = false)
    private String traceId;

    @Column(name = "processed_at", nullable = false)
    private OffsetDateTime processedAt;

    @PrePersist
    void onCreate() {
        if (processedAt == null) processedAt = OffsetDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getConsumerName() { return consumerName; }
    public void setConsumerName(String consumerName) { this.consumerName = consumerName; }
    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public Integer getPartitionNo() { return partitionNo; }
    public void setPartitionNo(Integer partitionNo) { this.partitionNo = partitionNo; }
    public Long getMsgOffset() { return msgOffset; }
    public void setMsgOffset(Long msgOffset) { this.msgOffset = msgOffset; }
    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
    public OffsetDateTime getProcessedAt() { return processedAt; }
    public void setProcessedAt(OffsetDateTime processedAt) { this.processedAt = processedAt; }
}
