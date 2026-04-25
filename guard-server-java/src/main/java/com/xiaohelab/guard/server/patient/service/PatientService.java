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
        // 1. 组装实体（嵌套 wire DTO → 扁平 DB 字段）
        PatientProfileEntity p = new PatientProfileEntity();
        p.setName(req.getPatientName());
        p.setGender(req.getGender());
        p.setBirthday(req.getBirthday());
        p.setAvatarUrl(req.getAvatarUrl());
        p.setChronicDiseases(req.getChronicDiseases());
        p.setMedication(req.getMedication());
        p.setAllergy(req.getAllergy());
        p.setEmergencyContactPhone(req.getEmergencyContactPhone());
        p.setLongTextProfile(req.getLongTextProfile());
        // 1.1 外观（嵌套）
        PatientCreateRequest.AppearanceBlock ap = req.getAppearance();
        if (ap != null) {
            p.setAppearanceHeightCm(ap.getHeightCm());
            p.setAppearanceWeightKg(ap.getWeightKg());
            p.setAppearanceClothing(ap.getClothing());
            p.setAppearanceFeatures(ap.getFeatures());
        }
        // 1.2 围栏（嵌套）
        PatientCreateRequest.FenceBlock fe = req.getFence();
        if (fe != null) {
            boolean enabled = Boolean.TRUE.equals(fe.getEnabled());
            p.setFenceEnabled(enabled);
            if (enabled) {
                if (fe.getCenterLat() == null || fe.getCenterLng() == null || fe.getRadiusM() == null) {
                    throw BizException.of(ErrorCode.E_PRO_4221);
                }
                p.setFenceCenterLat(fe.getCenterLat());
                p.setFenceCenterLng(fe.getCenterLng());
                p.setFenceRadiusM(fe.getRadiusM());
                p.setFenceCoordSystem(fe.getCoordSystem() != null ? fe.getCoordSystem() : "WGS84");
            }
        } else {
            p.setFenceEnabled(false);
        }
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
        if (req.getPatientName() != null) p.setName(req.getPatientName());
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
        // 外观（嵌套，整体替换语义；子字段 null 跳过）
        PatientUpdateRequest.AppearanceBlock ap = req.getAppearance();
        if (ap != null) {
            if (ap.getHeightCm() != null) p.setAppearanceHeightCm(ap.getHeightCm());
            if (ap.getWeightKg() != null) p.setAppearanceWeightKg(ap.getWeightKg());
            if (ap.getClothing() != null) p.setAppearanceClothing(ap.getClothing());
            if (ap.getFeatures() != null) p.setAppearanceFeatures(ap.getFeatures());
        }
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

    /** 更新围栏（API V2.0 §3.3.4：请求体嵌套 fence{}）。 */
    @Transactional(rollbackFor = Exception.class)
    public PatientResponse updateFence(Long patientId, FenceUpdateRequest req) {
        AuthUser user = SecurityUtil.current();
        PatientProfileEntity p = authorizationService.assertGuardian(user, patientId);
        FenceUpdateRequest.FenceBlock fe = req.getFence();
        if (fe == null) throw BizException.of(ErrorCode.E_PRO_4221);
        if (Boolean.TRUE.equals(fe.getEnabled())) {
            if (fe.getCenterLat() == null || fe.getCenterLng() == null || fe.getRadiusM() == null) {
                throw BizException.of(ErrorCode.E_PRO_4221);
            }
            p.setFenceCenterLat(fe.getCenterLat());
            p.setFenceCenterLng(fe.getCenterLng());
            p.setFenceRadiusM(fe.getRadiusM());
            p.setFenceCoordSystem(fe.getCoordSystem() != null ? fe.getCoordSystem() : "WGS84");
        }
        p.setFenceEnabled(fe.getEnabled());
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

    /**
     * 3.3.3 更新外观特征（API §3.3.3）。
     * <p>触发 profile.appearance.updated，供任务快照投影刷新。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public PatientResponse updateAppearance(Long patientId, AppearanceUpdateRequest req) {
        AuthUser user = SecurityUtil.current();
        PatientProfileEntity p = authorizationService.assertGuardian(user, patientId);
        boolean changed = false;
        if (req.getHeightCm() != null) { p.setAppearanceHeightCm(req.getHeightCm()); changed = true; }
        if (req.getWeightKg() != null) { p.setAppearanceWeightKg(req.getWeightKg()); changed = true; }
        if (req.getClothing() != null) { p.setAppearanceClothing(req.getClothing()); changed = true; }
        if (req.getFeatures() != null) { p.setAppearanceFeatures(req.getFeatures()); changed = true; }
        if (!changed) return toResponse(p);
        p.setProfileVersion(p.getProfileVersion() + 1);
        patientRepository.save(p);
        outboxService.publish(OutboxTopics.PROFILE_UPDATED, String.valueOf(p.getId()),
                String.valueOf(p.getId()),
                Map.of("patient_id", p.getId(),
                        "profile_version", p.getProfileVersion(),
                        "scope", "appearance"));
        log.info("[Patient] appearance updated patientId={} by={}", p.getId(), user.getUserId());
        return toResponse(p);
    }

    /**
     * 3.3.5 走失 / 安全确认（API §3.3.5）。
     * <ul>
     *   <li>action=CONFIRM_SAFE：MISSING_PENDING → NORMAL</li>
     *   <li>action=CONFIRM_MISSING：MISSING_PENDING → MISSING，并发布
     *       {@link OutboxTopics#PATIENT_MISSING_PENDING} 事件，供 AUTO_UPGRADE 任务创建流水线消费</li>
     * </ul>
     * @return Map 含 patient_id / lost_status / 可能的 task 信息（由后台流水填充）
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> missingPendingConfirm(Long patientId, MissingPendingConfirmRequest req) {
        AuthUser user = SecurityUtil.current();
        PatientProfileEntity p = authorizationService.assertGuardian(user, patientId);
        // 1. 仅 MISSING_PENDING 状态可确认
        if (!"MISSING_PENDING".equals(p.getLostStatus())) {
            throw BizException.of(ErrorCode.E_PRO_4092);
        }
        String action = req.getAction();
        if ("CONFIRM_SAFE".equals(action)) {
            p.setLostStatus("NORMAL");
        } else if ("CONFIRM_MISSING".equals(action)) {
            p.setLostStatus("MISSING");
        } else {
            throw BizException.of(ErrorCode.E_PRO_4015);
        }
        p.setLostStatusEventTime(OffsetDateTime.now());
        p.setProfileVersion(p.getProfileVersion() + 1);
        patientRepository.save(p);

        // 2. 发布事件
        if ("CONFIRM_SAFE".equals(action)) {
            outboxService.publish(OutboxTopics.PATIENT_CONFIRMED_SAFE, String.valueOf(p.getId()),
                    String.valueOf(p.getId()), Map.of("patient_id", p.getId(),
                            "user_id", user.getUserId(), "remark", req.getRemark() == null ? "" : req.getRemark()));
        } else {
            outboxService.publish(OutboxTopics.PATIENT_MISSING_PENDING, String.valueOf(p.getId()),
                    String.valueOf(p.getId()), Map.of("patient_id", p.getId(),
                            "confirmed_by", user.getUserId(),
                            "source", "FAMILY_CONFIRM",
                            "remark", req.getRemark() == null ? "" : req.getRemark()));
        }
        log.info("[Patient] missingPending confirm patientId={} action={} by={}",
                p.getId(), action, user.getUserId());
        return Map.of(
                "patient_id", String.valueOf(p.getId()),
                "lost_status", p.getLostStatus(),
                "action", action,
                "profile_version", p.getProfileVersion(),
                "event_time", p.getLostStatusEventTime()
        );
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
        r.setPatientName(p.getName());
        r.setGender(p.getGender());
        r.setBirthday(p.getBirthday());
        r.setShortCode(p.getShortCode());
        r.setAvatarUrl(p.getAvatarUrl());
        r.setChronicDiseases(p.getChronicDiseases());
        r.setMedication(p.getMedication());
        r.setAllergy(p.getAllergy());
        r.setEmergencyContactPhoneMasked(DesensitizeUtil.phone(p.getEmergencyContactPhone()));
        r.setLongTextProfile(p.getLongTextProfile());
        // 外观（扁平 → 嵌套）
        PatientResponse.Appearance ap = new PatientResponse.Appearance();
        ap.setHeightCm(p.getAppearanceHeightCm());
        ap.setWeightKg(p.getAppearanceWeightKg());
        ap.setClothing(p.getAppearanceClothing());
        ap.setFeatures(p.getAppearanceFeatures());
        r.setAppearance(ap);
        // 围栏（扁平 → 嵌套）
        PatientResponse.Fence fe = new PatientResponse.Fence();
        fe.setEnabled(p.getFenceEnabled());
        fe.setCenterLat(p.getFenceCenterLat());
        fe.setCenterLng(p.getFenceCenterLng());
        fe.setRadiusM(p.getFenceRadiusM());
        fe.setCoordSystem(p.getFenceCoordSystem());
        r.setFence(fe);
        r.setLostStatus(p.getLostStatus());
        r.setProfileVersion(p.getProfileVersion());
        r.setCreatedAt(p.getCreatedAt());
        r.setUpdatedAt(p.getUpdatedAt());
        return r;
    }
}
