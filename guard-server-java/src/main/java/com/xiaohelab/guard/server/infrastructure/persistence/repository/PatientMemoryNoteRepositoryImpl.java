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
        PatientMemoryNoteDO d = toDO(entity);
        mapper.insert(d);
        return toEntity(d);
    }

    @Override
    public List<PatientMemoryNoteEntity> listByPatientId(Long patientId, String kind,
                                                          int limit, int offset) {
        return mapper.listByPatientId(patientId, kind, limit, offset).stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
    }

    @Override
    public long countByPatientId(Long patientId, String kind) {
        return mapper.countByPatientId(patientId, kind);
    }

    /** DO → Entity 转换 */
    private PatientMemoryNoteEntity toEntity(PatientMemoryNoteDO d) {
        return PatientMemoryNoteEntity.reconstitute(
                d.getId(), d.getNoteId(), d.getPatientId(), d.getCreatedBy(),
                d.getKind(), d.getContent(), d.getTags(), d.getSourceEventId(),
                d.getCreatedAt(), d.getUpdatedAt());
    }

    /** Entity → DO 转换 */
    private PatientMemoryNoteDO toDO(PatientMemoryNoteEntity e) {
        PatientMemoryNoteDO d = new PatientMemoryNoteDO();
        d.setId(e.getId());
        d.setNoteId(e.getNoteId());
        d.setPatientId(e.getPatientId());
        d.setCreatedBy(e.getCreatedBy());
        d.setKind(e.getKind());
        d.setContent(e.getContent());
        d.setTags(e.getTags());
        d.setSourceEventId(e.getSourceEventId());
        d.setCreatedAt(e.getCreatedAt());
        d.setUpdatedAt(e.getUpdatedAt());
        return d;
    }
}
