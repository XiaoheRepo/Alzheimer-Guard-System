package com.xiaohelab.guard.server.infrastructure.persistence.do_;

import lombok.Data;

import java.time.Instant;

/**
 * patient_memory_note 持久化对象。
 * kind: HABIT / PLACE / PREFERENCE / SAFETY_CUE
 * tags 为 JSONB 字段，以 text 形式映射。
 */
@Data
public class PatientMemoryNoteDO {

    private Long id;
    /** 业务条目 ID，全局唯一，格式 mn_ + 时间戳 */
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
}
