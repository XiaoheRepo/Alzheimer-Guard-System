package com.xiaohelab.guard.server.infrastructure.outbox;

import com.xiaohelab.guard.server.domain.event.DomainEvent;
import com.xiaohelab.guard.server.infrastructure.persistence.do_.SysOutboxLogDO;
import com.xiaohelab.guard.server.infrastructure.persistence.mapper.SysOutboxLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Outbox 写入器（应用层调用）。
 * HC-02 约束：必须在业务事务内调用，与业务写操作同事务提交。
 * 调度器（OutboxDispatcher）异步将 PENDING 记录投递到 Kafka。
 */
@Component
@RequiredArgsConstructor
public class OutboxWriter {

    private final SysOutboxLogMapper sysOutboxLogMapper;

    /**
     * 将单条领域事件写入 Outbox 表。
     * 调用方须处于已开启的事务上下文中（@Transactional）。
     */
    public void write(DomainEvent event) {
        SysOutboxLogDO log = new SysOutboxLogDO();
        log.setEventId(event.getEventId());
        log.setTopic(event.getTopic());
        // aggregateId 优先取业务 ID，降级使用 partitionKey
        log.setAggregateId(event.getAggregateId() != null
                ? event.getAggregateId() : event.getPartitionKey());
        log.setPartitionKey(event.getPartitionKey());
        // payload 中嵌入 event_type，消费端无需读 Header
        String enrichedPayload = enrichPayload(event.getPayload(), event.getEventType());
        log.setPayload(enrichedPayload);
        log.setRequestId(event.getRequestId() != null ? event.getRequestId() : "");
        log.setTraceId(event.getTraceId() != null ? event.getTraceId() : "");
        log.setPhase("PENDING");
        log.setRetryCount(0);
        sysOutboxLogMapper.insert(log);
    }

    /**
     * 批量写入事件列表（一次事务内多条事件，如 task.created + task.state.changed）。
     */
    public void writeAll(List<DomainEvent> events) {
        for (DomainEvent event : events) {
            write(event);
        }
    }

    /** 将 event_type 嵌入 payload JSON 的 "event_type" 字段。 */
    private String enrichPayload(String payload, String eventType) {
        if (payload == null || !payload.startsWith("{")) {
            return String.format("{\"event_type\":\"%s\"}", eventType);
        }
        // 在 JSON 开头插入 event_type 字段
        return "{\"event_type\":\"" + eventType + "\"," + payload.substring(1);
    }
}
