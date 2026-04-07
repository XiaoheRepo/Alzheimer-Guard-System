package com.xiaohelab.guard.server.domain.task;

import com.xiaohelab.guard.server.common.exception.BizException;
import com.xiaohelab.guard.server.domain.event.DomainEvent;
import com.xiaohelab.guard.server.domain.event.EventTopics;
import com.xiaohelab.guard.server.infrastructure.persistence.do_.RescueTaskDO;
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
    private Long createdBy;         // 创建人（对应 DO 的 createdBy）
    private String source;          // 来源（对应 DO 的 source）
    private TaskStatus status;
    private String closeReason;
    private String remark;          // 备注（对应 DO 的 remark）
    private long eventVersion;      // 乐观锁版本（与 DO Long 类型一致）
    private Instant createdAt;
    private Instant updatedAt;
    private Instant closedAt;

    /** 领域内部使用：待发布的事件列表 */
    private final List<DomainEvent> pendingEvents = new ArrayList<>();

    private RescueTaskEntity() {}

    /**
     * 从 DO 重建聚合根（Repository 使用）。
     */
    public static RescueTaskEntity fromDO(RescueTaskDO d) {
        RescueTaskEntity e = new RescueTaskEntity();
        e.id = d.getId();
        e.taskNo = d.getTaskNo();
        e.patientId = d.getPatientId();
        e.createdBy = d.getCreatedBy();
        e.source = d.getSource();
        e.status = TaskStatus.valueOf(d.getStatus());
        e.closeReason = d.getCloseReason();
        e.remark = d.getRemark();
        e.eventVersion = d.getEventVersion() == null ? 0L : d.getEventVersion();
        e.createdAt = d.getCreatedAt();
        e.updatedAt = d.getUpdatedAt();
        e.closedAt = d.getClosedAt();
        return e;
    }

    /**
     * 创建新任务（工厂方法）。
     * 前置条件：患者已无 ACTIVE 任务（由应用服务校验）。
     */
    public static RescueTaskEntity create(String taskNo, Long patientId, Long createdBy,
                                          String source, String eventId) {
        RescueTaskEntity e = new RescueTaskEntity();
        e.taskNo = taskNo;
        e.patientId = patientId;
        e.createdBy = createdBy;
        e.source = source;
        e.status = TaskStatus.ACTIVE;
        e.eventVersion = 1L;
        // 附带 task.created 事件
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

    /**
     * 关闭任务（状态守卫：只有 ACTIVE 可关闭）。
     * HC-01：发布 task.resolved 或 task.false_alarm + task.state.changed。
     */
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

    /** 获取并清除待发布事件列表（应用服务消费后清空） */
    public List<DomainEvent> popPendingEvents() {
        List<DomainEvent> copy = new ArrayList<>(pendingEvents);
        pendingEvents.clear();
        return copy;
    }

    /** 转为 DO（持久化用，不包含 id） */
    public RescueTaskDO toDO() {
        RescueTaskDO d = new RescueTaskDO();
        d.setId(this.id);
        d.setTaskNo(this.taskNo);
        d.setPatientId(this.patientId);
        d.setCreatedBy(this.createdBy);
        d.setSource(this.source);
        d.setStatus(this.status.name());
        d.setCloseReason(this.closeReason);
        d.setRemark(this.remark);
        d.setEventVersion(this.eventVersion);
        d.setClosedAt(this.closedAt);
        return d;
    }

    // ===== 私有 Payload 构建方法 =====

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
