package com.xiaohelab.guard.server.pushtoken.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

/**
 * 推送令牌实体（V2.1 DBD §2.6.7）。
 * <p>按 (user_id, device_id) 唯一；notification-service 按平台分发 FCM/HMS/MiPush/APNs/WebPush。
 */
@Entity
@Table(name = "user_push_token")
public class UserPushTokenEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "push_token_id")
    private Long pushTokenId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** ANDROID_FCM / ANDROID_HMS / ANDROID_MIPUSH / IOS_APNS / WEB_PUSH */
    @Column(name = "platform", length = 24, nullable = false)
    private String platform;

    @Column(name = "device_id", length = 128, nullable = false)
    private String deviceId;

    /** PII：@Desensitize(TOKEN) */
    @Column(name = "push_token", length = 512, nullable = false)
    private String pushToken;

    @Column(name = "app_version", length = 32, nullable = false)
    private String appVersion;

    @Column(name = "os_version", length = 64)
    private String osVersion;

    @Column(name = "device_model", length = 64)
    private String deviceModel;

    @Column(name = "locale", length = 16, nullable = false)
    private String locale = "zh-CN";

    /** ACTIVE / REVOKED */
    @Column(name = "status", length = 16, nullable = false)
    private String status = "ACTIVE";

    @Column(name = "last_active_at", nullable = false)
    private OffsetDateTime lastActiveAt;

    @Column(name = "revoked_at")
    private OffsetDateTime revokedAt;

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
        if (lastActiveAt == null) lastActiveAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() { updatedAt = OffsetDateTime.now(); }

    // Getters & Setters
    public Long getPushTokenId() { return pushTokenId; }
    public void setPushTokenId(Long pushTokenId) { this.pushTokenId = pushTokenId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }
    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    public String getPushToken() { return pushToken; }
    public void setPushToken(String pushToken) { this.pushToken = pushToken; }
    public String getAppVersion() { return appVersion; }
    public void setAppVersion(String appVersion) { this.appVersion = appVersion; }
    public String getOsVersion() { return osVersion; }
    public void setOsVersion(String osVersion) { this.osVersion = osVersion; }
    public String getDeviceModel() { return deviceModel; }
    public void setDeviceModel(String deviceModel) { this.deviceModel = deviceModel; }
    public String getLocale() { return locale; }
    public void setLocale(String locale) { this.locale = locale; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public OffsetDateTime getLastActiveAt() { return lastActiveAt; }
    public void setLastActiveAt(OffsetDateTime lastActiveAt) { this.lastActiveAt = lastActiveAt; }
    public OffsetDateTime getRevokedAt() { return revokedAt; }
    public void setRevokedAt(OffsetDateTime revokedAt) { this.revokedAt = revokedAt; }
    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
