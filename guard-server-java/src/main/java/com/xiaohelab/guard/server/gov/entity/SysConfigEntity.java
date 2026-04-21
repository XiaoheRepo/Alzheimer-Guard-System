package com.xiaohelab.guard.server.gov.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "sys_config")
public class SysConfigEntity {

    @Id
    @Column(name = "config_key", length = 128)
    private String configKey;

    @Column(name = "config_value", columnDefinition = "TEXT", nullable = false)
    private String configValue;

    /** public / ops / security / ai_policy */
    @Column(name = "scope", length = 32, nullable = false)
    private String scope = "public";

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "updated_reason", length = 256)
    private String updatedReason;

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

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

    public String getConfigKey() { return configKey; }
    public void setConfigKey(String configKey) { this.configKey = configKey; }
    public String getConfigValue() { return configValue; }
    public void setConfigValue(String configValue) { this.configValue = configValue; }
    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Long getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(Long updatedBy) { this.updatedBy = updatedBy; }
    public String getUpdatedReason() { return updatedReason; }
    public void setUpdatedReason(String updatedReason) { this.updatedReason = updatedReason; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
