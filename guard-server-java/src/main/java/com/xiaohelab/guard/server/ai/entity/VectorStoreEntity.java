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

    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "embedding", columnDefinition = "TEXT")
    private String embedding;

    @Column(name = "valid", nullable = false)
    private Boolean valid = true;

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
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getEmbedding() { return embedding; }
    public void setEmbedding(String embedding) { this.embedding = embedding; }
    public Boolean getValid() { return valid; }
    public void setValid(Boolean valid) { this.valid = valid; }
    public OffsetDateTime getSupersededAt() { return supersededAt; }
    public void setSupersededAt(OffsetDateTime supersededAt) { this.supersededAt = supersededAt; }
    public OffsetDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(OffsetDateTime deletedAt) { this.deletedAt = deletedAt; }
    public OffsetDateTime getExpiredAt() { return expiredAt; }
    public void setExpiredAt(OffsetDateTime expiredAt) { this.expiredAt = expiredAt; }
}
