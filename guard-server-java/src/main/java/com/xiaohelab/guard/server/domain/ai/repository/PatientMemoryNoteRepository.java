package com.xiaohelab.guard.server.domain.ai.repository;

import com.xiaohelab.guard.server.domain.ai.entity.PatientMemoryNoteEntity;

import java.util.List;

/**
 * 患者记忆条目 Repository 接口（AI 域，基础设施层实现）。
 */
public interface PatientMemoryNoteRepository {

    /** 新增记忆条目，持久化后 id/createdAt 已回填 */
    PatientMemoryNoteEntity insert(PatientMemoryNoteEntity entity);

    /** 分页读取患者记忆条目（支持 kind 过滤），最新在前 */
    List<PatientMemoryNoteEntity> listByPatientId(Long patientId, String kind, int limit, int offset);

    long countByPatientId(Long patientId, String kind);
}
