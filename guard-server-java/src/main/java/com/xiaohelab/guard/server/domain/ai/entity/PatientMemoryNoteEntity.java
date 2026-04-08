package com.xiaohelab.guard.server.domain.ai.entity;

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

    /** 从持久化数据重建（仅 Infrastructure 层 RepositoryImpl 调用）。 */
    public static PatientMemoryNoteEntity reconstitute(
            Long id, String noteId, Long patientId, Long createdBy,
            String kind, String content, String tags, String sourceEventId,
            Instant createdAt, Instant updatedAt) {
        PatientMemoryNoteEntity e = new PatientMemoryNoteEntity();
        e.id = id;
        e.noteId = noteId;
        e.patientId = patientId;
        e.createdBy = createdBy;
        e.kind = kind;
        e.content = content;
        e.tags = tags;
        e.sourceEventId = sourceEventId;
        e.createdAt = createdAt;
        e.updatedAt = updatedAt;
        return e;
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
