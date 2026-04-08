package com.xiaohelab.guard.server.infrastructure.persistence.repository;

import com.xiaohelab.guard.server.domain.ai.entity.PatientMemoryNoteEntity;
import com.xiaohelab.guard.server.domain.ai.repository.PatientMemoryNoteRepository;
import com.xiaohelab.guard.server.infrastructure.persistence.do_.PatientMemoryNoteDO;
import com.xiaohelab.guard.server.infrastructure.persistence.mapper.PatientMemoryNoteMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

/**
 * PatientMemoryNoteRepository 基础设施层实现。
 */
@Repository
@RequiredArgsConstructor
public class PatientMemoryNoteRepositoryImpl implements PatientMemoryNoteRepository {

    private final PatientMemoryNoteMapper mapper;

    @Override
    public PatientMemoryNoteEntity insert(PatientMemoryNoteEntity entity) {
        PatientMemoryNoteDO d = entity.toDO();
        mapper.insert(d);
        // MyBatis useGeneratedKeys 将 id 回填到 d.id
        return PatientMemoryNoteEntity.fromDO(d);
    }

    @Override
    public List<PatientMemoryNoteEntity> listByPatientId(Long patientId, String kind,
                                                          int limit, int offset) {
        return mapper.listByPatientId(patientId, kind, limit, offset).stream()
                .map(PatientMemoryNoteEntity::fromDO)
                .collect(Collectors.toList());
    }

    @Override
    public long countByPatientId(Long patientId, String kind) {
        return mapper.countByPatientId(patientId, kind);
    }
}
