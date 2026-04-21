package com.xiaohelab.guard.server.patient.service;

import com.xiaohelab.guard.server.common.error.ErrorCode;
import com.xiaohelab.guard.server.common.exception.BizException;
import com.xiaohelab.guard.server.common.security.AuthUser;
import com.xiaohelab.guard.server.patient.entity.GuardianRelationEntity;
import com.xiaohelab.guard.server.patient.entity.PatientProfileEntity;
import com.xiaohelab.guard.server.patient.repository.GuardianRelationRepository;
import com.xiaohelab.guard.server.patient.repository.PatientProfileRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 监护授权校验服务（HC-06）。所有患者域资源访问都必须经过 assert* 方法。
 */
@Service
public class GuardianAuthorizationService {

    private final GuardianRelationRepository relationRepository;
    private final PatientProfileRepository patientRepository;

    public GuardianAuthorizationService(GuardianRelationRepository relationRepository,
                                        PatientProfileRepository patientRepository) {
        this.relationRepository = relationRepository;
        this.patientRepository = patientRepository;
    }

    /** 判断用户是否对患者具备任意活跃监护关系。 */
    public boolean isGuardian(Long userId, Long patientId) {
        return relationRepository.findByUserIdAndPatientIdAndRelationStatus(userId, patientId, "ACTIVE").isPresent();
    }

    /** 判断用户是否为患者的主监护人。 */
    public boolean isPrimary(Long userId, Long patientId) {
        return relationRepository.findByUserIdAndPatientIdAndRelationStatus(userId, patientId, "ACTIVE")
                .map(r -> "PRIMARY_GUARDIAN".equals(r.getRelationRole())).orElse(false);
    }

    /** 强校验：用户对患者具有监护权限，否则抛 E_PRO_4030。 */
    public PatientProfileEntity assertGuardian(AuthUser user, Long patientId) {
        if (user.isAdmin()) {
            return patientRepository.findById(patientId)
                    .orElseThrow(() -> BizException.of(ErrorCode.E_PRO_4041));
        }
        if (!isGuardian(user.getUserId(), patientId)) {
            throw BizException.of(ErrorCode.E_PRO_4030);
        }
        PatientProfileEntity p = patientRepository.findById(patientId)
                .orElseThrow(() -> BizException.of(ErrorCode.E_PRO_4041));
        if (p.getDeletedAt() != null) {
            throw BizException.of(ErrorCode.E_PRO_4041);
        }
        return p;
    }

    /** 强校验：用户是否为主监护人（用于删除、围栏设置、主权转移等高危操作）。 */
    public PatientProfileEntity assertPrimary(AuthUser user, Long patientId) {
        PatientProfileEntity p = assertGuardian(user, patientId);
        if (!user.isAdmin() && !isPrimary(user.getUserId(), patientId)) {
            throw BizException.of(ErrorCode.E_PRO_4032);
        }
        return p;
    }

    /** 获取当前用户可见的患者 ID 列表（活跃监护）。 */
    public List<Long> listAccessiblePatientIds(Long userId) {
        return relationRepository.findByUserIdAndRelationStatus(userId, "ACTIVE").stream()
                .map(GuardianRelationEntity::getPatientId).collect(Collectors.toList());
    }

    /** 获取某患者的所有活跃监护人列表。 */
    public List<Long> listActiveGuardianUserIds(Long patientId) {
        return relationRepository.findByPatientIdAndRelationStatus(patientId, "ACTIVE").stream()
                .map(GuardianRelationEntity::getUserId).collect(Collectors.toList());
    }
}
