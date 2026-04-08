package com.xiaohelab.guard.server.domain.notification.entity;

import com.xiaohelab.guard.server.infrastructure.persistence.do_.NotificationInboxDO;
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

    public static NotificationEntity fromDO(NotificationInboxDO d) {
        NotificationEntity e = new NotificationEntity();
        e.notificationId = d.getNotificationId();
        e.userId = d.getUserId();
        e.type = d.getType();
        e.title = d.getTitle();
        e.content = d.getContent();
        e.level = d.getLevel();
        e.relatedTaskId = d.getRelatedTaskId();
        e.relatedPatientId = d.getRelatedPatientId();
        e.readStatus = d.getReadStatus();
        e.readAt = d.getReadAt();
        e.traceId = d.getTraceId();
        e.createdAt = d.getCreatedAt();
        e.updatedAt = d.getUpdatedAt();
        return e;
    }

    public NotificationInboxDO toDO() {
        NotificationInboxDO d = new NotificationInboxDO();
        d.setNotificationId(this.notificationId);
        d.setUserId(this.userId);
        d.setType(this.type);
        d.setTitle(this.title);
        d.setContent(this.content);
        d.setLevel(this.level);
        d.setRelatedTaskId(this.relatedTaskId);
        d.setRelatedPatientId(this.relatedPatientId);
        d.setReadStatus(this.readStatus);
        d.setReadAt(this.readAt);
        d.setTraceId(this.traceId);
        d.setCreatedAt(this.createdAt);
        d.setUpdatedAt(this.updatedAt);
        return d;
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
