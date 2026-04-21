package com.xiaohelab.guard.server.outbox.service;

import com.xiaohelab.guard.server.common.util.BusinessNoUtil;
import com.xiaohelab.guard.server.common.util.JsonUtil;
import com.xiaohelab.guard.server.common.util.TraceIdUtil;
import com.xiaohelab.guard.server.outbox.entity.OutboxLogEntity;
import com.xiaohelab.guard.server.outbox.repository.OutboxLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Outbox 事件落库服务。HC-02：必须在业务本地事务内调用。
 */
@Service
public class OutboxService {

    private final OutboxLogRepository outboxRepository;

    public OutboxService(OutboxLogRepository outboxRepository) {
        this.outboxRepository = outboxRepository;
    }

    /**
     * 落库一条待发布事件。
     *
     * @param topic         Topic 名
     * @param aggregateId   聚合根业务 ID（如 taskId/patientId）
     * @param partitionKey  分区键（同一聚合串行）
     * @param payload       事件载荷（将被序列化为 JSON）
     */
    @Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
    public OutboxLogEntity publish(String topic, String aggregateId, String partitionKey, Object payload) {
        OutboxLogEntity e = new OutboxLogEntity();
        e.setEventId(BusinessNoUtil.eventId());
        e.setTopic(topic);
        e.setAggregateId(aggregateId);
        e.setPartitionKey(partitionKey == null ? aggregateId : partitionKey);
        Map<String, Object> envelope = Map.of(
                "event_id", e.getEventId(),
                "topic", topic,
                "aggregate_id", aggregateId,
                "trace_id", TraceIdUtil.currentTraceId(),
                "payload", payload
        );
        e.setPayload(JsonUtil.toJson(envelope));
        e.setRequestId(TraceIdUtil.currentRequestId());
        e.setTraceId(TraceIdUtil.currentTraceId());
        e.setPhase("PENDING");
        return outboxRepository.save(e);
    }
}
