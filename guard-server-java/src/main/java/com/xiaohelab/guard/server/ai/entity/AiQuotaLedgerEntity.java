package com.xiaohelab.guard.server.ai.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

/**
 * AI 配额账本（双账本：USER + PATIENT 均须校验）。
 * 不继承 BaseEntity，因为该表无 trace_id 列且 updated_at 在 DB 侧维护。
 */
@Entity
@Table(name = "ai_quota_ledger")
public class AiQuotaLedgerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** USER / PATIENT */
    @Column(name = "ledger_type", length = 16, nullable = false)
    private String ledgerType;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    /** 如 "2025-11-23" */
    @Column(name = "period", length = 16, nullable = false)
    private String period;

    @Column(name = "quota_limit", nullable = false)
    private Integer quotaLimit;

    @Column(name = "used", nullable = false)
    private Integer used = 0;

    @Column(name = "reserved", nullable = false)
    private Integer reserved = 0;

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

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getLedgerType() { return ledgerType; }
    public void setLedgerType(String ledgerType) { this.ledgerType = ledgerType; }
    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }
    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }
    public Integer getQuotaLimit() { return quotaLimit; }
    public void setQuotaLimit(Integer quotaLimit) { this.quotaLimit = quotaLimit; }
    public Integer getUsed() { return used; }
    public void setUsed(Integer used) { this.used = used; }
    public Integer getReserved() { return reserved; }
    public void setReserved(Integer reserved) { this.reserved = reserved; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
