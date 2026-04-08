package com.xiaohelab.guard.server.domain.notification.entity;

import lombok.Getter;

import java.time.Instant;

/**
 * 通知收件箱实体（通知域）。
 * HC-06 约束：通知不依赖短信，仅使用站内通知与推送。
 */
@Getter
public class NotificationEntity {

    private Long notificationId;
    private Long userId;
    /** TASK_PROGRESS / FENCE_ALERT / TASK_CLOSED / SYSTEM */
    private String type;
    private String title;
    private String content;
    /** INFO / WARN / CRITICAL */
    private String level;
    private Long relatedTaskId;
    private Long relatedPatientId;
    /** UNREAD / READ */
    private String readStatus;
    private Instant readAt;
    private String traceId;
    private Instant createdAt;
    private Instant updatedAt;

    private NotificationEntity() {}

    /** 从持久化数据重建（仅 Infrastructure 层 RepositoryImpl 调用）。 */
    public static NotificationEntity reconstitute(
            Long notificationId, Long userId, String type, String title, String content,
            String level, Long relatedTaskId, Long relatedPatientId,
            String readStatus, Instant readAt, String traceId,
            Instant createdAt, Instant updatedAt) {
        NotificationEntity e = new NotificationEntity();
        e.notificationId = notificationId;
        e.userId = userId;
        e.type = type;
        e.title = title;
        e.content = content;
        e.level = level;
        e.relatedTaskId = relatedTaskId;
        e.relatedPatientId = relatedPatientId;
        e.readStatus = readStatus;
        e.readAt = readAt;
        e.traceId = traceId;
        e.createdAt = createdAt;
        e.updatedAt = updatedAt;
        return e;
    }

    /** 工厂方法：创建新的待写入通知（readStatus 默认 UNREAD） */
    public static NotificationEntity of(Long userId, String type, String title,
                                         String content, String level,
                                         Long relatedTaskId, Long relatedPatientId,
                                         String traceId) {
        NotificationEntity e = new NotificationEntity();
        e.userId = userId;
        e.type = type;
        e.title = title;
        e.content = content;
        e.level = level;
        e.relatedTaskId = relatedTaskId;
        e.relatedPatientId = relatedPatientId;
        e.readStatus = "UNREAD";
        e.traceId = traceId;
        return e;
    }
}
