package com.xiaohelab.guard.server.patient.service;

import com.xiaohelab.guard.server.common.error.ErrorCode;
import com.xiaohelab.guard.server.common.exception.BizException;
import com.xiaohelab.guard.server.common.event.OutboxTopics;
import com.xiaohelab.guard.server.common.security.AuthUser;
import com.xiaohelab.guard.server.common.security.SecurityUtil;
import com.xiaohelab.guard.server.common.util.BusinessNoUtil;
import com.xiaohelab.guard.server.common.util.DesensitizeUtil;
import com.xiaohelab.guard.server.patient.dto.*;
import com.xiaohelab.guard.server.patient.entity.GuardianRelationEntity;
import com.xiaohelab.guard.server.patient.entity.PatientProfileEntity;
import com.xiaohelab.guard.server.patient.repository.GuardianRelationRepository;
import com.xiaohelab.guard.server.patient.repository.PatientProfileRepository;
import com.xiaohelab.guard.server.outbox.service.OutboxService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 患者档案服务。
 * <p>职责：
 * <ul>
 *   <li>建档（自动生成 profile_no / short_code，并把创建者登记为 PRIMARY_GUARDIAN）；</li>
 *   <li>查询、列表、更新（HC-04 版本号 +1）、逻辑删除（含关联监护关系批量撤销）；</li>
 *   <li>围栏配置；</li>
 *   <li>失联状态恢复 confirmSafe；</li>
 *   <li>所有对外响应通过 DesensitizeUtil 完成手机号脱敏。</li>
 * </ul>
 * 所有变更均通过 {@link OutboxService} 发布 profile.* 领域事件。
 */
@Service
public class PatientService {

    private static final Logger log = LoggerFactory.getLogger(PatientService.class);

    private final PatientProfileRepository patientRepository;
    private final GuardianRelationRepository relationRepository;
    private final GuardianAuthorizationService authorizationService;
    private final OutboxService outboxService;

    public PatientService(PatientProfileRepository patientRepository,
                          GuardianRelationRepository relationRepository,
                          GuardianAuthorizationService authorizationService,
                          OutboxService outboxService) {
        this.patientRepository = patientRepository;
        this.relationRepository = relationRepository;
        this.authorizationService = authorizationService;
        this.outboxService = outboxService;
    }

    /** 创建患者档案，创建者自动成为主监护人。 */
    @Transactional(rollbackFor = Exception.class)
    public PatientResponse create(PatientCreateRequest req) {
        AuthUser user = SecurityUtil.current();
        // 1. 组装实体
        PatientProfileEntity p = new PatientProfileEntity();
        p.setName(req.getName());
        p.setGender(req.getGender());
        p.setBirthday(req.getBirthday());
        p.setAvatarUrl(req.getAvatarUrl());
        p.setChronicDiseases(req.getChronicDiseases());
        p.setMedication(req.getMedication());
        p.setAllergy(req.getAllergy());
        p.setEmergencyContactPhone(req.getEmergencyContactPhone());
        p.setLongTextProfile(req.getLongTextProfile());
        p.setAppearanceHeightCm(req.getAppearanceHeightCm());
        p.setAppearanceWeightKg(req.getAppearanceWeightKg());
        p.setAppearanceClothing(req.getAppearanceClothing());
        p.setAppearanceFeatures(req.getAppearanceFeatures());
        p.setProfileNo(BusinessNoUtil.profileNo());
        p.setShortCode(generateUniqueShortCode());
        p.setLostStatus("NORMAL");
        p.setLostStatusEventTime(OffsetDateTime.now());
        p.setProfileVersion(1L);
        patientRepository.save(p);

        // 2. 创建者自动成为主监护人
        GuardianRelationEntity relation = new GuardianRelationEntity();
        relation.setUserId(user.getUserId());
        relation.setPatientId(p.getId());
        relation.setRelationRole("PRIMARY_GUARDIAN");
        relation.setRelationStatus("ACTIVE");
        relation.setJoinedAt(OffsetDateTime.now());
        relationRepository.save(relation);

        // 3. 发布领域事件
        Map<String, Object> payload = Map.of(
                "patient_id", p.getId(),
                "creator_user_id", user.getUserId(),
                "short_code", p.getShortCode(),
                "profile_version", p.getProfileVersion()
        );
        outboxService.publish(OutboxTopics.PROFILE_CREATED, String.valueOf(p.getId()),
                String.valueOf(p.getId()), payload);

        log.info("[Patient] created patientId={} byUser={}", p.getId(), user.getUserId());
        return toResponse(p);
    }

    /** 查询患者详情。 */
    public PatientResponse get(Long patientId) {
        AuthUser user = SecurityUtil.current();
        PatientProfileEntity p = authorizationService.assertGuardian(user, patientId);
        return toResponse(p);
    }

    /** 当前用户可见的患者列表。 */
    public List<PatientResponse> listMyPatients() {
        AuthUser user = SecurityUtil.current();
        List<Long> ids = authorizationService.listAccessiblePatientIds(user.getUserId());
        if (ids.isEmpty()) return List.of();
        return patientRepository.findAllById(ids).stream()
                .filter(p -> p.getDeletedAt() == null)
                .map(this::toResponse).collect(Collectors.toList());
    }

    /** 更新患者档案。HC-04 乐观锁 + 事件版本 +1。 */
    @Transactional(rollbackFor = Exception.class)
    public PatientResponse update(Long patientId, PatientUpdateRequest req) {
        AuthUser user = SecurityUtil.current();
        PatientProfileEntity p = authorizationService.assertGuardian(user, patientId);
        // 1. 更新可变字段（null 跳过）
        if (req.getName() != null) p.setName(req.getName());
        if (req.getGender() != null) p.setGender(req.getGender());
        if (req.getBirthday() != null) p.setBirthday(req.getBirthday());
        if (req.getAvatarUrl() != null) {
            if (req.getAvatarUrl().isBlank()) throw BizException.of(ErrorCode.E_PRO_4014);
            p.setAvatarUrl(req.getAvatarUrl());
        }
        if (req.getChronicDiseases() != null) p.setChronicDiseases(req.getChronicDiseases());
        if (req.getMedication() != null) p.setMedication(req.getMedication());
        if (req.getAllergy() != null) p.setAllergy(req.getAllergy());
        if (req.getEmergencyContactPhone() != null) p.setEmergencyContactPhone(req.getEmergencyContactPhone());
        if (req.getLongTextProfile() != null) p.setLongTextProfile(req.getLongTextProfile());
        if (req.getAppearanceHeightCm() != null) p.setAppearanceHeightCm(req.getAppearanceHeightCm());
        if (req.getAppearanceWeightKg() != null) p.setAppearanceWeightKg(req.getAppearanceWeightKg());
        if (req.getAppearanceClothing() != null) p.setAppearanceClothing(req.getAppearanceClothing());
        if (req.getAppearanceFeatures() != null) p.setAppearanceFeatures(req.getAppearanceFeatures());
        p.setProfileVersion(p.getProfileVersion() + 1);
        patientRepository.save(p);

        outboxService.publish(OutboxTopics.PROFILE_UPDATED, String.valueOf(p.getId()),
                String.valueOf(p.getId()),
                Map.of("patient_id", p.getId(), "profile_version", p.getProfileVersion()));
        return toResponse(p);
    }

    /** 逻辑删除患者档案（仅主监护人）。 */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long patientId) {
        AuthUser user = SecurityUtil.current();
        PatientProfileEntity p = authorizationService.assertPrimary(user, patientId);
        p.setDeletedAt(OffsetDateTime.now());
        patientRepository.save(p);
        // 同时软撤销所有监护关系
        List<GuardianRelationEntity> relations = relationRepository.findByPatientIdAndRelationStatus(patientId, "ACTIVE");
        for (GuardianRelationEntity r : relations) {
            r.setRelationStatus("REVOKED");
            r.setRevokedAt(OffsetDateTime.now());
            relationRepository.save(r);
        }
        outboxService.publish(OutboxTopics.PROFILE_DELETED_LOGICAL, String.valueOf(p.getId()),
                String.valueOf(p.getId()), Map.of("patient_id", p.getId()));
    }

    /** 更新围栏。 */
    @Transactional(rollbackFor = Exception.class)
    public PatientResponse updateFence(Long patientId, FenceUpdateRequest req) {
        AuthUser user = SecurityUtil.current();
        PatientProfileEntity p = authorizationService.assertGuardian(user, patientId);
        if (Boolean.TRUE.equals(req.getFenceEnabled())) {
            if (req.getFenceCenterLat() == null || req.getFenceCenterLng() == null || req.getFenceRadiusM() == null) {
                throw BizException.of(ErrorCode.E_PRO_4221);
            }
            p.setFenceCenterLat(req.getFenceCenterLat());
            p.setFenceCenterLng(req.getFenceCenterLng());
            p.setFenceRadiusM(req.getFenceRadiusM());
            p.setFenceCoordSystem(req.getFenceCoordSystem() != null ? req.getFenceCoordSystem() : "WGS84");
        }
        p.setFenceEnabled(req.getFenceEnabled());
        p.setProfileVersion(p.getProfileVersion() + 1);
        patientRepository.save(p);
        return toResponse(p);
    }

    /** 确认患者安全（MISSING_PENDING -> NORMAL）。 */
    @Transactional(rollbackFor = Exception.class)
    public PatientResponse confirmSafe(Long patientId) {
        AuthUser user = SecurityUtil.current();
        PatientProfileEntity p = authorizationService.assertGuardian(user, patientId);
        if (!"MISSING_PENDING".equals(p.getLostStatus())) {
            throw BizException.of(ErrorCode.E_PRO_4092);
        }
        p.setLostStatus("NORMAL");
        p.setLostStatusEventTime(OffsetDateTime.now());
        p.setProfileVersion(p.getProfileVersion() + 1);
        patientRepository.save(p);
        outboxService.publish(OutboxTopics.PATIENT_CONFIRMED_SAFE, String.valueOf(p.getId()),
                String.valueOf(p.getId()), Map.of("patient_id", p.getId(), "user_id", user.getUserId()));
        return toResponse(p);
    }

    private String generateUniqueShortCode() {
        for (int i = 0; i < 10; i++) {
            String code = BusinessNoUtil.shortCode();
            if (!patientRepository.existsByShortCode(code)) return code;
        }
        throw BizException.of(ErrorCode.E_SYS_5000, "short_code 生成失败");
    }

    public PatientResponse toResponse(PatientProfileEntity p) {
        PatientResponse r = new PatientResponse();
        r.setPatientId(p.getId());
        r.setName(p.getName());
        r.setGender(p.getGender());
        r.setBirthday(p.getBirthday());
        r.setShortCode(p.getShortCode());
        r.setAvatarUrl(p.getAvatarUrl());
        r.setChronicDiseases(p.getChronicDiseases());
        r.setMedication(p.getMedication());
        r.setAllergy(p.getAllergy());
        r.setEmergencyContactPhoneMasked(DesensitizeUtil.phone(p.getEmergencyContactPhone()));
        r.setLongTextProfile(p.getLongTextProfile());
        r.setAppearanceHeightCm(p.getAppearanceHeightCm());
        r.setAppearanceWeightKg(p.getAppearanceWeightKg());
        r.setAppearanceClothing(p.getAppearanceClothing());
        r.setAppearanceFeatures(p.getAppearanceFeatures());
        r.setFenceEnabled(p.getFenceEnabled());
        r.setFenceCenterLat(p.getFenceCenterLat());
        r.setFenceCenterLng(p.getFenceCenterLng());
        r.setFenceRadiusM(p.getFenceRadiusM());
        r.setLostStatus(p.getLostStatus());
        r.setProfileVersion(p.getProfileVersion());
        r.setCreatedAt(p.getCreatedAt());
        r.setUpdatedAt(p.getUpdatedAt());
        return r;
    }
}
