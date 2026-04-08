package com.xiaohelab.guard.server.infrastructure.persistence.repository;

import com.xiaohelab.guard.server.domain.patient.entity.PatientEntity;
import com.xiaohelab.guard.server.domain.patient.repository.PatientRepository;
import com.xiaohelab.guard.server.domain.profile.repository.ProfileRepository;
import com.xiaohelab.guard.server.infrastructure.persistence.do_.PatientProfileDO;
import com.xiaohelab.guard.server.infrastructure.persistence.mapper.PatientProfileMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * PatientRepository 基础设施实现：封装 PatientProfileMapper，向上暴露领域类型。
 * 同时实现 ProfileRepository（供公共扫码入口使用）。
 */
@Repository
@RequiredArgsConstructor
public class PatientRepositoryImpl implements PatientRepository, ProfileRepository {

    private final PatientProfileMapper patientProfileMapper;

    @Override
    public Optional<PatientEntity> findById(Long id) {
        var d = patientProfileMapper.findById(id);
        return Optional.ofNullable(d).map(this::toEntity);
    }

    @Override
    public Optional<PatientEntity> findByShortCode(String shortCode) {
        var d = patientProfileMapper.findByShortCode(shortCode);
        return Optional.ofNullable(d).map(this::toEntity);
    }

    @Override
    public Optional<PatientEntity> findByProfileNo(String profileNo) {
        var d = patientProfileMapper.findByProfileNo(profileNo);
        return Optional.ofNullable(d).map(this::toEntity);
    }

    @Override
    public List<PatientEntity> findByUserId(Long userId) {
        return patientProfileMapper.findByUserId(userId)
                .stream().map(this::toEntity).toList();
    }

    @Override
    public PatientEntity insert(PatientEntity entity) {
        var d = toDO(entity);
        patientProfileMapper.insert(d);
        return toEntity(patientProfileMapper.findById(d.getId()));
    }

    @Override
    public PatientEntity update(PatientEntity entity) {
        patientProfileMapper.update(toDO(entity));
        return findById(entity.getId()).orElse(entity);
    }

    @Override
    public PatientEntity updateFence(PatientEntity entity) {
        patientProfileMapper.updateFence(toDO(entity));
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

    /** DO → Entity 转换 */
    private PatientEntity toEntity(PatientProfileDO d) {
        return PatientEntity.reconstitute(
                d.getId(), d.getProfileNo(), d.getName(), d.getGender(), d.getBirthday(),
                d.getShortCode(), d.getPinCodeHash(), d.getPinCodeSalt(),
                d.getPhotoUrl(), d.getMedicalHistory(),
                d.getFenceEnabled(), d.getFenceCenterLat(), d.getFenceCenterLng(), d.getFenceRadiusM(),
                d.getLostStatus(), d.getLostStatusEventTime(), d.getProfileVersion(),
                d.getCreatedAt(), d.getUpdatedAt());
    }

    /** Entity → DO 转换 */
    private PatientProfileDO toDO(PatientEntity e) {
        PatientProfileDO d = new PatientProfileDO();
        d.setId(e.getId());
        d.setProfileNo(e.getProfileNo());
        d.setName(e.getName());
        d.setGender(e.getGender());
        d.setBirthday(e.getBirthday());
        d.setShortCode(e.getShortCode());
        d.setPinCodeHash(e.getPinCodeHash());
        d.setPinCodeSalt(e.getPinCodeSalt());
        d.setPhotoUrl(e.getPhotoUrl());
        d.setMedicalHistory(e.getMedicalHistory());
        d.setFenceEnabled(e.getFenceEnabled());
        d.setFenceCenterLat(e.getFenceCenterLat());
        d.setFenceCenterLng(e.getFenceCenterLng());
        d.setFenceRadiusM(e.getFenceRadiusM());
        d.setLostStatus(e.getLostStatus());
        d.setLostStatusEventTime(e.getLostStatusEventTime());
        d.setProfileVersion(e.getProfileVersion());
        return d;
    }
}
