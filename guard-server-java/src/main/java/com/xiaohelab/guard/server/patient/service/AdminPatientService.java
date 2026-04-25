package com.xiaohelab.guard.server.patient.service;

import com.xiaohelab.guard.server.common.dto.CursorResponse;
import com.xiaohelab.guard.server.common.error.ErrorCode;
import com.xiaohelab.guard.server.common.event.OutboxTopics;
import com.xiaohelab.guard.server.common.exception.BizException;
import com.xiaohelab.guard.server.common.security.AuthUser;
import com.xiaohelab.guard.server.common.security.SecurityUtil;
import com.xiaohelab.guard.server.common.util.CursorUtil;
import com.xiaohelab.guard.server.common.util.DesensitizeUtil;
import com.xiaohelab.guard.server.gov.service.AuditLogger;
import com.xiaohelab.guard.server.outbox.service.OutboxService;
import com.xiaohelab.guard.server.patient.dto.admin.AdminGuardianItem;
import com.xiaohelab.guard.server.patient.dto.admin.AdminPatientDetailResponse;
import com.xiaohelab.guard.server.patient.dto.admin.AdminPatientListItem;
import com.xiaohelab.guard.server.patient.dto.admin.ForceTransferPrimaryRequest;
import com.xiaohelab.guard.server.patient.entity.GuardianRelationEntity;
import com.xiaohelab.guard.server.patient.entity.PatientProfileEntity;
import com.xiaohelab.guard.server.patient.repository.GuardianRelationRepository;
import com.xiaohelab.guard.server.patient.repository.PatientProfileRepository;
import com.xiaohelab.guard.server.user.entity.UserEntity;
import com.xiaohelab.guard.server.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 管理员治理 - 患者全局访问服务（V2.1 增量）。
 * <p>对应：API §3.3.15~3.3.17、LLD §5.3.8~5.3.10、FR-PRO-011~012、BDD §24。</p>
 */
@Service
public class AdminPatientService {

    private static final Logger log = LoggerFactory.getLogger(AdminPatientService.class);

    private final PatientProfileRepository patientProfileRepository;
    private final GuardianRelationRepository guardianRelationRepository;
    private final UserRepository userRepository;
    private final OutboxService outboxService;
    private final AuditLogger auditLogger;

    public AdminPatientService(PatientProfileRepository patientProfileRepository,
                               GuardianRelationRepository guardianRelationRepository,
                               UserRepository userRepository,
                               OutboxService outboxService,
                               AuditLogger auditLogger) {
        this.patientProfileRepository = patientProfileRepository;
        this.guardianRelationRepository = guardianRelationRepository;
        this.userRepository = userRepository;
        this.outboxService = outboxService;
        this.auditLogger = auditLogger;
    }

    // =========================================================
    // 1. 列表（GET /api/v1/admin/patients）—— MEDIUM 风险
    // =========================================================
    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public CursorResponse<AdminPatientListItem> list(String keyword,
                                                     String status,
                                                     String gender,
                                                     Long primaryGuardianUserId,
                                                     String cursor,
                                                     int pageSize) {
        AuthUser me = SecurityUtil.current();
        assertAdmin(me);
        int size = normalizePageSize(pageSize);
        Long cursorId = CursorUtil.decodeId(cursor);
        // 在 Java 侧转小写，避免 Hibernate 6 将 :kw 推断为 bytea 导致 lower(bytea) 错误
        String kw = (keyword == null || keyword.isBlank()) ? null : keyword.trim().toLowerCase();

        List<PatientProfileEntity> raw = patientProfileRepository.findForAdmin(
                kw, status, gender, primaryGuardianUserId, cursorId, size + 1);
        boolean hasNext = raw.size() > size;
        if (hasNext) raw = raw.subList(0, size);

        List<AdminPatientListItem> items = raw.stream()
                .map(this::toListItem)
                .collect(Collectors.toList());

        String nextCursor = hasNext && !items.isEmpty()
                ? CursorUtil.encode(raw.get(raw.size() - 1).getId())
                : null;

        auditLogger.logSuccess("PROFILE", "admin.patient.list", null, "MEDIUM", null,
                Map.of("keyword", kw == null ? "" : kw,
                        "status", status == null ? "" : status,
                        "result_size", items.size()));
        return CursorResponse.of(items, size, nextCursor, hasNext);
    }

    // =========================================================
    // 2. 详情（GET /api/v1/admin/patients/{id}）—— MEDIUM
    // =========================================================
    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public AdminPatientDetailResponse detail(Long patientId) {
        AuthUser me = SecurityUtil.current();
        assertAdmin(me);

        PatientProfileEntity p = loadActivePatient(patientId);

        AdminPatientDetailResponse r = new AdminPatientDetailResponse();
        r.setPatientId(String.valueOf(p.getId()));
        r.setProfileNo(p.getProfileNo());
        r.setPatientName(DesensitizeUtil.chineseName(p.getName()));
        r.setGender(p.getGender());
        r.setBirthday(p.getBirthday());
        r.setShortCode(p.getShortCode());
        r.setAvatarUrl(p.getAvatarUrl());
        r.setLostStatus(p.getLostStatus());
        r.setProfileVersion(p.getProfileVersion());
        r.setFenceEnabled(p.getFenceEnabled());
        r.setCreatedAt(p.getCreatedAt());
        r.setUpdatedAt(p.getUpdatedAt());

        List<GuardianRelationEntity> relations = guardianRelationRepository
                .findByPatientIdAndRelationStatus(patientId, "ACTIVE");
        List<AdminGuardianItem> gi = new ArrayList<>(relations.size());
        for (GuardianRelationEntity g : relations) {
            Optional<UserEntity> uOpt = userRepository.findById(g.getUserId());
            if (uOpt.isEmpty()) continue;
            UserEntity u = uOpt.get();
            AdminGuardianItem it = new AdminGuardianItem();
            it.setUserId(String.valueOf(u.getId()));
            it.setUsername(u.getUsername());
            it.setNickname(DesensitizeUtil.chineseName(u.getNickname()));
            it.setPhone(DesensitizeUtil.phone(u.getPhone()));
            it.setRelationRole(g.getRelationRole());
            it.setRelationStatus(g.getRelationStatus());
            it.setJoinedAt(g.getJoinedAt());
            gi.add(it);
        }
        r.setGuardians(gi);

        auditLogger.logSuccess("PROFILE", "admin.patient.read", String.valueOf(patientId),
                "MEDIUM", null,
                Map.of("target_patient_id", patientId,
                        "guardian_count", gi.size()));
        return r;
    }

    // =========================================================
    // 3. 强制转移主监护（POST /api/v1/admin/patients/{id}/guardians/force-transfer）
    //    CRITICAL + CONFIRM_3 + SUPER_ADMIN ONLY
    // =========================================================
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> forceTransferPrimary(Long patientId,
                                                    ForceTransferPrimaryRequest req,
                                                    String confirmLevel) {
        AuthUser me = SecurityUtil.current();
        // 规则：高危操作仅限 SUPER_ADMIN
        if (!me.isSuperAdmin()) throw BizException.of(ErrorCode.E_GOV_4032);
        // CONFIRM_3 强校验
        if (!"CONFIRM_3".equals(confirmLevel)) {
            throw BizException.of(ErrorCode.E_AUTH_4031, "该操作需要 X-Confirm-Level=CONFIRM_3");
        }

        PatientProfileEntity p = loadActivePatient(patientId);

        // 1. 当前主监护
        GuardianRelationEntity current = guardianRelationRepository
                .findByPatientIdAndRelationRoleAndRelationStatus(patientId, "PRIMARY_GUARDIAN", "ACTIVE")
                .orElseThrow(() -> BizException.of(ErrorCode.E_PRO_4046));

        // 2. 目标必须是该患者当前 ACTIVE 监护人
        GuardianRelationEntity target = guardianRelationRepository
                .findByUserIdAndPatientIdAndRelationStatus(req.getTargetUserId(), patientId, "ACTIVE")
                .orElseThrow(() -> BizException.of(ErrorCode.E_PRO_4044));

        // 3. 自己转给自己 → 冲突
        if (current.getId().equals(target.getId())) {
            throw BizException.of(ErrorCode.E_PRO_4097, "target_user_id 已是当前主监护");
        }

        // 4. 角色互换（乐观锁由 @Version 兜底）
        String fromUserId = String.valueOf(current.getUserId());
        String toUserId = String.valueOf(target.getUserId());
        current.setRelationRole("GUARDIAN");
        target.setRelationRole("PRIMARY_GUARDIAN");
        guardianRelationRepository.save(current);
        guardianRelationRepository.save(target);

        // 5. 档案版本 + profile.updated 时间戳
        p.setProfileVersion(p.getProfileVersion() + 1);
        patientProfileRepository.save(p);

        // 6. Outbox 广播（专用强制转移事件 + 通用 transfer.completed）
        Map<String, Object> payload = new HashMap<>();
        payload.put("patient_id", String.valueOf(patientId));
        payload.put("from_user_id", fromUserId);
        payload.put("to_user_id", toUserId);
        payload.put("operator_user_id", me.getUserId());
        payload.put("reason", req.getReason());
        payload.put("evidence_url", req.getEvidenceUrl());
        payload.put("occurred_at", OffsetDateTime.now().toString());
        outboxService.publish(OutboxTopics.PATIENT_PRIMARY_GUARDIAN_FORCE_TRANSFERRED,
                String.valueOf(patientId), String.valueOf(patientId), payload);
        outboxService.publish(OutboxTopics.GUARDIAN_TRANSFER_DONE,
                String.valueOf(patientId), String.valueOf(patientId),
                Map.of(
                        "patient_id", String.valueOf(patientId),
                        "from_user_id", fromUserId,
                        "to_user_id", toUserId,
                        "source", "ADMIN_FORCE"
                ));

        // 7. CRITICAL 审计
        auditLogger.logSuccess("PROFILE", "admin.patient.force_transfer_primary",
                String.valueOf(patientId), "CRITICAL", "CONFIRM_3",
                Map.of(
                        "patient_id", patientId,
                        "from_user_id", fromUserId,
                        "to_user_id", toUserId,
                        "reason", req.getReason(),
                        "evidence_url", req.getEvidenceUrl() == null ? "" : req.getEvidenceUrl()
                ));

        log.warn("[Admin] 强制转移主监护 patientId={} {} -> {} operator={} reason={}",
                patientId, fromUserId, toUserId, me.getUserId(), req.getReason());

        return Map.of(
                "patient_id", String.valueOf(patientId),
                "from_user_id", fromUserId,
                "to_user_id", toUserId,
                "profile_version", p.getProfileVersion()
        );
    }

    // =========================
    // 辅助
    // =========================

    private void assertAdmin(AuthUser me) {
        if (!me.isAdmin()) throw BizException.of(ErrorCode.E_AUTH_4031);
    }

    private int normalizePageSize(int pageSize) {
        if (pageSize <= 0) return 20;
        return Math.min(pageSize, 100);
    }

    private PatientProfileEntity loadActivePatient(Long patientId) {
        PatientProfileEntity p = patientProfileRepository.findById(patientId)
                .orElseThrow(() -> BizException.of(ErrorCode.E_PRO_4041));
        if (p.getDeletedAt() != null) throw BizException.of(ErrorCode.E_PRO_4041);
        return p;
    }

    private AdminPatientListItem toListItem(PatientProfileEntity p) {
        AdminPatientListItem i = new AdminPatientListItem();
        i.setPatientId(String.valueOf(p.getId()));
        i.setProfileNo(p.getProfileNo());
        i.setPatientName(DesensitizeUtil.chineseName(p.getName()));
        i.setShortCode(p.getShortCode());
        i.setGender(p.getGender());
        i.setLostStatus(p.getLostStatus());
        i.setCreatedAt(p.getCreatedAt());
        // 主监护 + 监护总数
        List<GuardianRelationEntity> actives = guardianRelationRepository
                .findByPatientIdAndRelationStatus(p.getId(), "ACTIVE");
        i.setGuardianCount(actives.size());
        actives.stream()
                .filter(g -> "PRIMARY_GUARDIAN".equals(g.getRelationRole()))
                .findFirst()
                .ifPresent(g -> i.setPrimaryGuardianUserId(String.valueOf(g.getUserId())));
        return i;
    }
}
