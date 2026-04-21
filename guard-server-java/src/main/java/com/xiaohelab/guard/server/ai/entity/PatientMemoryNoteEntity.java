package com.xiaohelab.guard.server.ai.entity;

import com.xiaohelab.guard.server.common.entity.BaseEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "patient_memory_note")
public class PatientMemoryNoteEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "note_id", length = 64, nullable = false, unique = true)
    private String noteId;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    /** HABIT / PLACE / PREFERENCE / SAFETY_CUE / RESCUE_CASE */
    @Column(name = "kind", length = 32, nullable = false)
    private String kind;

    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tags", columnDefinition = "jsonb")
    private String tags;

    @Column(name = "source_event_id", length = 64)
    private String sourceEventId;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNoteId() { return noteId; }
    public void setNoteId(String noteId) { this.noteId = noteId; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
    public String getKind() { return kind; }
    public void setKind(String kind) { this.kind = kind; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
    public String getSourceEventId() { return sourceEventId; }
    public void setSourceEventId(String sourceEventId) { this.sourceEventId = sourceEventId; }
}
