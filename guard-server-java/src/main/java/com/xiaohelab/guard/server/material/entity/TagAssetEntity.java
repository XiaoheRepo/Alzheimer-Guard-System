package com.xiaohelab.guard.server.material.entity;

import com.xiaohelab.guard.server.common.entity.BaseEntity;
import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "tag_asset")
public class TagAssetEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tag_code", length = 100, nullable = false, unique = true)
    private String tagCode;

    /** QR_CODE / NFC */
    @Column(name = "tag_type", length = 20, nullable = false)
    private String tagType;

    /** UNBOUND / ALLOCATED / BOUND / SUSPECTED_LOST / LOST / VOIDED */
    @Column(name = "status", length = 20, nullable = false)
    private String status = "UNBOUND";

    @Column(name = "patient_id")
    private Long patientId;

    @Column(name = "short_code", length = 6)
    private String shortCode;

    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "resource_token", length = 512)
    private String resourceToken;

    @Column(name = "batch_no", length = 64)
    private String batchNo;

    @Column(name = "void_reason", length = 256)
    private String voidReason;

    @Column(name = "lost_reason", length = 256)
    private String lostReason;

    @Column(name = "lost_at")
    private OffsetDateTime lostAt;

    @Column(name = "void_at")
    private OffsetDateTime voidAt;

    @Column(name = "suspected_lost_at")
    private OffsetDateTime suspectedLostAt;

    @Column(name = "bound_at")
    private OffsetDateTime boundAt;

    @Column(name = "allocated_at")
    private OffsetDateTime allocatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTagCode() { return tagCode; }
    public void setTagCode(String tagCode) { this.tagCode = tagCode; }
    public String getTagType() { return tagType; }
    public void setTagType(String tagType) { this.tagType = tagType; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public String getShortCode() { return shortCode; }
    public void setShortCode(String shortCode) { this.shortCode = shortCode; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public String getResourceToken() { return resourceToken; }
    public void setResourceToken(String resourceToken) { this.resourceToken = resourceToken; }
    public String getBatchNo() { return batchNo; }
    public void setBatchNo(String batchNo) { this.batchNo = batchNo; }
    public String getVoidReason() { return voidReason; }
    public void setVoidReason(String voidReason) { this.voidReason = voidReason; }
    public String getLostReason() { return lostReason; }
    public void setLostReason(String lostReason) { this.lostReason = lostReason; }
    public OffsetDateTime getLostAt() { return lostAt; }
    public void setLostAt(OffsetDateTime lostAt) { this.lostAt = lostAt; }
    public OffsetDateTime getVoidAt() { return voidAt; }
    public void setVoidAt(OffsetDateTime voidAt) { this.voidAt = voidAt; }
    public OffsetDateTime getSuspectedLostAt() { return suspectedLostAt; }
    public void setSuspectedLostAt(OffsetDateTime suspectedLostAt) { this.suspectedLostAt = suspectedLostAt; }
    public OffsetDateTime getBoundAt() { return boundAt; }
    public void setBoundAt(OffsetDateTime boundAt) { this.boundAt = boundAt; }
    public OffsetDateTime getAllocatedAt() { return allocatedAt; }
    public void setAllocatedAt(OffsetDateTime allocatedAt) { this.allocatedAt = allocatedAt; }
}
