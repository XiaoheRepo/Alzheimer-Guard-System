package com.xiaohelab.guard.server.notification.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "notification_inbox")
public class NotificationInboxEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Long notificationId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "type", length = 32, nullable = false)
    private String type;

    @Column(name = "title", length = 128, nullable = false)
    private String title;

    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "level", length = 16, nullable = false)
    private String level = "INFO";

    @Column(name = "channel", length = 32, nullable = false)
    private String channel = "INBOX";

    @Column(name = "related_task_id")
    private Long relatedTaskId;

    @Column(name = "related_patient_id")
    private Long relatedPatientId;

    @Column(name = "related_object_id", length = 64)
    private String relatedObjectId;

    /** UNREAD / READ */
    @Column(name = "read_status", length = 16, nullable = false)
    private String readStatus = "UNREAD";

    @Column(name = "read_at")
    private OffsetDateTime readAt;

    @Column(name = "trace_id", length = 64, nullable = false)
    private String traceId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() { updatedAt = OffsetDateTime.now(); }

    public Long getNotificationId() { return notificationId; }
    public void setNotificationId(Long notificationId) { this.notificationId = notificationId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
    public Long getRelatedTaskId() { return relatedTaskId; }
    public void setRelatedTaskId(Long relatedTaskId) { this.relatedTaskId = relatedTaskId; }
    public Long getRelatedPatientId() { return relatedPatientId; }
    public void setRelatedPatientId(Long relatedPatientId) { this.relatedPatientId = relatedPatientId; }
    public String getRelatedObjectId() { return relatedObjectId; }
    public void setRelatedObjectId(String relatedObjectId) { this.relatedObjectId = relatedObjectId; }
    public String getReadStatus() { return readStatus; }
    public void setReadStatus(String readStatus) { this.readStatus = readStatus; }
    public OffsetDateTime getReadAt() { return readAt; }
    public void setReadAt(OffsetDateTime readAt) { this.readAt = readAt; }
    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
