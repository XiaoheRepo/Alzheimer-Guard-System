package com.xiaohelab.guard.server.infrastructure.persistence.do_;

import lombok.Data;

import java.time.Instant;

/**
 * notification_inbox 持久化对象（站内通知中心）。
 * 3.1.15 告警列表直接复用此表（按 type/level/related_task_id 过滤）。
 * HC-06：通知不依赖短信，仅使用站内 + 应用推送。
 */
@Data
public class NotificationInboxDO {

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
}
