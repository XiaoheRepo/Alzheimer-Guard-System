package com.xiaohelab.guard.server.domain.ai.entity;

import com.xiaohelab.guard.server.infrastructure.persistence.do_.PatientMemoryNoteDO;
import lombok.Getter;

import java.time.Instant;

/**
 * 患者记忆条目实体（AI 域）。
 * kind: HABIT / PLACE / PREFERENCE / SAFETY_CUE
 */
@Getter
public class PatientMemoryNoteEntity {

    private Long id;
    private String noteId;
    private Long patientId;
    private Long createdBy;
    /** HABIT / PLACE / PREFERENCE / SAFETY_CUE */
    private String kind;
    private String content;
    /** 语义标签列表 JSONB 序列化文本 */
    private String tags;
    private String sourceEventId;
    private Instant createdAt;
    private Instant updatedAt;

    private PatientMemoryNoteEntity() {}

    public static PatientMemoryNoteEntity fromDO(PatientMemoryNoteDO d) {
        PatientMemoryNoteEntity e = new PatientMemoryNoteEntity();
        e.id = d.getId();
        e.noteId = d.getNoteId();
        e.patientId = d.getPatientId();
        e.createdBy = d.getCreatedBy();
        e.kind = d.getKind();
        e.content = d.getContent();
        e.tags = d.getTags();
        e.sourceEventId = d.getSourceEventId();
        e.createdAt = d.getCreatedAt();
        e.updatedAt = d.getUpdatedAt();
        return e;
    }

    public PatientMemoryNoteDO toDO() {
        PatientMemoryNoteDO d = new PatientMemoryNoteDO();
        d.setId(this.id);
        d.setNoteId(this.noteId);
        d.setPatientId(this.patientId);
        d.setCreatedBy(this.createdBy);
        d.setKind(this.kind);
        d.setContent(this.content);
        d.setTags(this.tags);
        d.setSourceEventId(this.sourceEventId);
        d.setCreatedAt(this.createdAt);
        d.setUpdatedAt(this.updatedAt);
        return d;
    }

    /** 工厂方法（插入前创建，noteId 由调用方生成） */
    public static PatientMemoryNoteEntity create(String noteId, Long patientId, Long createdBy,
                                                  String kind, String content, String tagsJson) {
        PatientMemoryNoteEntity e = new PatientMemoryNoteEntity();
        e.noteId = noteId;
        e.patientId = patientId;
        e.createdBy = createdBy;
        e.kind = kind;
        e.content = content;
        e.tags = tagsJson;
        return e;
    }
}
