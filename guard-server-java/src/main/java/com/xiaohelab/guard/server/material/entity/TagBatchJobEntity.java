package com.xiaohelab.guard.server.material.entity;

import com.xiaohelab.guard.server.common.entity.AuditOnlyEntity;
import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "tag_batch_job")
public class TagBatchJobEntity extends AuditOnlyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_id", length = 64, nullable = false, unique = true)
    private String jobId;

    @Column(name = "tag_type", length = 20, nullable = false)
    private String tagType;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "success_count", nullable = false)
    private Integer successCount = 0;

    @Column(name = "fail_count", nullable = false)
    private Integer failCount = 0;

    @Column(name = "status", length = 20, nullable = false)
    private String status = "PENDING";

    @Column(name = "batch_key_id", length = 64)
    private String batchKeyId;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getJobId() { return jobId; }
    public void setJobId(String jobId) { this.jobId = jobId; }
    public String getTagType() { return tagType; }
    public void setTagType(String tagType) { this.tagType = tagType; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public Integer getSuccessCount() { return successCount; }
    public void setSuccessCount(Integer successCount) { this.successCount = successCount; }
    public Integer getFailCount() { return failCount; }
    public void setFailCount(Integer failCount) { this.failCount = failCount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getBatchKeyId() { return batchKeyId; }
    public void setBatchKeyId(String batchKeyId) { this.batchKeyId = batchKeyId; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
    public OffsetDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(OffsetDateTime completedAt) { this.completedAt = completedAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
