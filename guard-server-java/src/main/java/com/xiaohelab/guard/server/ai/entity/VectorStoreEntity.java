package com.xiaohelab.guard.server.ai.entity;

import com.xiaohelab.guard.server.common.entity.AuditOnlyEntity;
import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "vector_store")
public class VectorStoreEntity extends AuditOnlyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "source_type", length = 32, nullable = false)
    private String sourceType;

    @Column(name = "source_id", length = 64, nullable = false)
    private String sourceId;

    @Column(name = "source_version", nullable = false)
    private Long sourceVersion = 1L;

    @Column(name = "chunk_index", nullable = false)
    private Integer chunkIndex = 0;

    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    /**
     * 1024 维向量列，PostgreSQL `vector(1024)` 类型。
     * Hibernate 6 没有内置 vector 映射，故标记 {@link Transient}：
     * 写入/查询统一走 {@code VectorStoreNativeDao}（JdbcTemplate + ::vector 字面量）。
     */
    @Transient
    private float[] embedding;

    @Column(name = "valid", nullable = false)
    private Boolean valid = true;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @Column(name = "superseded_at")
    private OffsetDateTime supersededAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    @Column(name = "expired_at")
    private OffsetDateTime expiredAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public String getSourceId() { return sourceId; }
    public void setSourceId(String sourceId) { this.sourceId = sourceId; }
    public Long getSourceVersion() { return sourceVersion; }
    public void setSourceVersion(Long sourceVersion) { this.sourceVersion = sourceVersion; }
    public Integer getChunkIndex() { return chunkIndex; }
    public void setChunkIndex(Integer chunkIndex) { this.chunkIndex = chunkIndex; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public float[] getEmbedding() { return embedding; }
    public void setEmbedding(float[] embedding) { this.embedding = embedding; }
    public Boolean getValid() { return valid; }
    public void setValid(Boolean valid) { this.valid = valid; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
    public OffsetDateTime getSupersededAt() { return supersededAt; }
    public void setSupersededAt(OffsetDateTime supersededAt) { this.supersededAt = supersededAt; }
    public OffsetDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(OffsetDateTime deletedAt) { this.deletedAt = deletedAt; }
    public OffsetDateTime getExpiredAt() { return expiredAt; }
    public void setExpiredAt(OffsetDateTime expiredAt) { this.expiredAt = expiredAt; }
}
