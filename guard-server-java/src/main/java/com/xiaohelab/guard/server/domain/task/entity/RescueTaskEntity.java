package com.xiaohelab.guard.server.domain.task.entity;

import com.xiaohelab.guard.server.common.exception.BizException;
import com.xiaohelab.guard.server.domain.event.DomainEvent;
import com.xiaohelab.guard.server.domain.event.EventTopics;
import lombok.Getter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * 寻回任务聚合根。
 * HC-01 约束：任务域是 lost_status 唯一权威来源；
 * 状态机：ACTIVE → CLOSED（RESOLVED / FALSE_ALARM）。
 * 聚合根方法返回领域事件列表，应用服务负责持久化并写入 Outbox。
 */
@Getter
public class RescueTaskEntity {

    /** 任务状态枚举 */
    public enum TaskStatus { ACTIVE, CLOSED }

    /** 关闭类型 */
    public enum CloseType { RESOLVED, FALSE_ALARM }

    private Long id;
    private String taskNo;
    private Long patientId;
    private Long createdBy;
    private String source;
    private TaskStatus status;
    private String closeReason;
    private String remark;
    private long eventVersion;
    private String aiAnalysisSummary;
    private String posterUrl;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant closedAt;

    private final List<DomainEvent> pendingEvents = new ArrayList<>();

    private RescueTaskEntity() {}

    /** 从持久化数据重建（仅 Infrastructure 层 RepositoryImpl 调用）。 */
    public static RescueTaskEntity reconstitute(Long id, String taskNo, Long patientId, Long createdBy,
                                                String source, String statusStr, String closeReason,
                                                String remark, Long eventVersion,
                                                String aiAnalysisSummary, String posterUrl,
                                                Instant createdAt, Instant updatedAt, Instant closedAt) {
        RescueTaskEntity e = new RescueTaskEntity();
        e.id = id;
        e.taskNo = taskNo;
        e.patientId = patientId;
        e.createdBy = createdBy;
        e.source = source;
        e.status = TaskStatus.valueOf(statusStr);
        e.closeReason = closeReason;
        e.remark = remark;
        e.eventVersion = eventVersion == null ? 0L : eventVersion;
        e.aiAnalysisSummary = aiAnalysisSummary;
        e.posterUrl = posterUrl;
        e.createdAt = createdAt;
        e.updatedAt = updatedAt;
        e.closedAt = closedAt;
        return e;
    }

    public static RescueTaskEntity create(String taskNo, Long patientId, Long createdBy,
                                          String source, String eventId) {
        RescueTaskEntity e = new RescueTaskEntity();
        e.taskNo = taskNo;
        e.patientId = patientId;
        e.createdBy = createdBy;
        e.source = source;
        e.status = TaskStatus.ACTIVE;
        e.eventVersion = 1L;
        e.pendingEvents.add(DomainEvent.builder()
                .eventId(eventId)
                .topic(EventTopics.TOPIC_RESCUE_TASK)
                .partitionKey(String.valueOf(patientId))
                .eventType(EventTopics.TASK_CREATED)
                .occurredAt(Instant.now())
                .payload(buildCreatedPayload(taskNo, patientId))
                .build());
        return e;
    }

    public void close(CloseType closeType, String reason, String remark,
                      String resolvedEventId, String stateChangedEventId) {
        if (this.status != TaskStatus.ACTIVE) {
            throw BizException.of("E_TASK_4041");
        }
        if (closeType == CloseType.FALSE_ALARM &&
                (reason == null || reason.trim().length() < 5)) {
            throw BizException.of("E_TASK_4004");
        }
        this.status = TaskStatus.CLOSED;
        this.closeReason = reason;
        this.remark = remark;
        this.closedAt = Instant.now();
        this.eventVersion++;

        String eventType = closeType == CloseType.RESOLVED
                ? EventTopics.TASK_RESOLVED : EventTopics.TASK_FALSE_ALARM;
        this.pendingEvents.add(DomainEvent.builder()
                .eventId(resolvedEventId)
                .topic(EventTopics.TOPIC_RESCUE_TASK)
                .partitionKey(String.valueOf(this.patientId))
                .eventType(eventType)
                .occurredAt(Instant.now())
                .payload(buildClosePayload(closeType.name(), reason))
                .build());
        this.pendingEvents.add(DomainEvent.builder()
                .eventId(stateChangedEventId)
                .topic(EventTopics.TOPIC_RESCUE_TASK)
                .partitionKey(String.valueOf(this.patientId))
                .eventType(EventTopics.TASK_STATE_CHANGED)
                .occurredAt(Instant.now())
                .payload(buildStateChangedPayload())
                .build());
    }

    public List<DomainEvent> popPendingEvents() {
        List<DomainEvent> copy = new ArrayList<>(pendingEvents);
        pendingEvents.clear();
        return copy;
    }

    /** 获取状态字符串（供 Infrastructure 层转换使用）。 */
    public String getStatusName() {
        return this.status.name();
    }

    private static String buildCreatedPayload(String taskNo, Long patientId) {
        return String.format("{\"task_no\":\"%s\",\"patient_id\":%d}", taskNo, patientId);
    }

    private String buildClosePayload(String closeType, String reason) {
        return String.format(
                "{\"task_no\":\"%s\",\"patient_id\":%d,\"close_type\":\"%s\",\"reason\":\"%s\"}",
                this.taskNo, this.patientId, closeType,
                reason == null ? "" : reason.replace("\"", "\\\""));
    }

    private String buildStateChangedPayload() {
        return String.format(
                "{\"task_no\":\"%s\",\"patient_id\":%d,\"status\":\"%s\",\"event_version\":%d}",
                this.taskNo, this.patientId, this.status.name(), this.eventVersion);
    }
}
