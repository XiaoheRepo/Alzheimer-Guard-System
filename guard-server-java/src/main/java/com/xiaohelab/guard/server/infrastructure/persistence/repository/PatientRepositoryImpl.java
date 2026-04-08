package com.xiaohelab.guard.server.infrastructure.persistence.repository;

import com.xiaohelab.guard.server.domain.patient.entity.PatientEntity;
import com.xiaohelab.guard.server.domain.patient.repository.PatientRepository;
import com.xiaohelab.guard.server.infrastructure.persistence.mapper.PatientProfileMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * PatientRepository 基础设施实现：封装 PatientProfileMapper，向上暴露领域类型。
 */
@Repository
@RequiredArgsConstructor
public class PatientRepositoryImpl implements PatientRepository {

    private final PatientProfileMapper patientProfileMapper;

    @Override
    public Optional<PatientEntity> findById(Long id) {
        var d = patientProfileMapper.findById(id);
        return Optional.ofNullable(d).map(PatientEntity::fromDO);
    }

    @Override
    public Optional<PatientEntity> findByShortCode(String shortCode) {
        var d = patientProfileMapper.findByShortCode(shortCode);
        return Optional.ofNullable(d).map(PatientEntity::fromDO);
    }

    @Override
    public Optional<PatientEntity> findByProfileNo(String profileNo) {
        var d = patientProfileMapper.findByProfileNo(profileNo);
        return Optional.ofNullable(d).map(PatientEntity::fromDO);
    }

    @Override
    public List<PatientEntity> findByUserId(Long userId) {
        return patientProfileMapper.findByUserId(userId)
                .stream().map(PatientEntity::fromDO).toList();
    }

    @Override
    public PatientEntity insert(PatientEntity entity) {
        var d = entity.toDO();
        patientProfileMapper.insert(d);
        // MyBatis useGeneratedKeys 回填 id 到 d
        return PatientEntity.fromDO(patientProfileMapper.findById(d.getId()));
    }

    @Override
    public PatientEntity update(PatientEntity entity) {
        patientProfileMapper.update(entity.toDO());
        return findById(entity.getId()).orElse(entity);
    }

    @Override
    public PatientEntity updateFence(PatientEntity entity) {
        patientProfileMapper.updateFence(entity.toDO());
        return findById(entity.getId()).orElse(entity);
    }

    @Override
    public int updateLostStatusByEvent(Long id, String lostStatus, Instant eventTime) {
        return patientProfileMapper.updateLostStatusByEvent(id, lostStatus, eventTime);
    }

    @Override
    public long nextShortCodeSeq() {
        return patientProfileMapper.nextShortCodeSeq();
    }
}
