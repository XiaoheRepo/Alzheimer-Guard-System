package com.xiaohelab.guard.server.application.patient;

import com.xiaohelab.guard.server.common.exception.BizException;
import com.xiaohelab.guard.server.common.util.IdGenerator;
import com.xiaohelab.guard.server.domain.guardian.entity.GuardianRelationEntity;
import com.xiaohelab.guard.server.domain.guardian.repository.GuardianRepository;
import com.xiaohelab.guard.server.domain.patient.entity.PatientEntity;
import com.xiaohelab.guard.server.domain.patient.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * 患者档案服务。
 * 创建档案时同事务绑定主监护人（PRIMARY_GUARDIAN）关系。
 * PIN 码明文不落库，BCrypt 单向哈希存储。
 */
@Service
@RequiredArgsConstructor
public class PatientProfileService {

    private final PatientRepository patientRepository;
    private final GuardianRepository guardianRepository;

    /**
     * 创建患者档案，并将创建者设为 PRIMARY_GUARDIAN。
     * 同事务写 patient_profile + sys_user_patient。
     */
    @Transactional
    public PatientEntity createPatient(Long creatorUserId,
                                       String name,
                                       String gender,
                                       LocalDate birthday,
                                       String photoUrl,
                                       String medicalHistory,
                                       String pinCode) {
        // 1. 生成档案编号与 short_code
        String profileNo = IdGenerator.profileNo();
        long seqVal = patientRepository.nextShortCodeSeq();
        String shortCode = String.format("%06d", seqVal % 1_000_000);

        // 2. PIN 码哈希（应用层负责，领域不依赖加密库）
        String salt = BCrypt.gensalt();
        String pinHash = BCrypt.hashpw(pinCode, salt);

        // 3. 构建聚合根并持久化
        PatientEntity patient = PatientEntity.create(profileNo, shortCode, name, gender, birthday,
                photoUrl, medicalHistory, pinHash, salt);
        patient = patientRepository.insert(patient);

        // 4. 建立 PRIMARY_GUARDIAN 关系
        GuardianRelationEntity rel = GuardianRelationEntity.create(creatorUserId, patient.getId(), "PRIMARY_GUARDIAN");
        guardianRepository.insert(rel);

        return patient;
    }

    /**
     * 更新患者基本资料（仅主监护人或 SUPERADMIN）。
     */
    @Transactional
    public PatientEntity updatePatient(Long patientId, Long operatorUserId, boolean isAdmin,
                                       String name, String gender, LocalDate birthday,
                                       String photoUrl, String medicalHistory) {
        PatientEntity patient = requirePatient(patientId);
        if (!isAdmin) {
            requirePrimaryGuardian(patientId, operatorUserId);
        }
        patient.updateProfile(name, gender, birthday, photoUrl, medicalHistory);
        return patientRepository.update(patient);
    }

    /**
     * 查询患者档案（需关联或管理员权限）。
     */
    public PatientEntity getPatient(Long patientId, Long userId, boolean isAdmin) {
        PatientEntity patient = requirePatient(patientId);
        if (!isAdmin && guardianRepository.countActiveRelation(userId, patientId) == 0) {
            throw BizException.of("E_TASK_4030");
        }
        return patient;
    }

    /**
     * 查询用户关联的所有患者列表。
     */
    public List<PatientEntity> listMyPatients(Long userId) {
        return patientRepository.findByUserId(userId);
    }

    /**
     * 更新围栏配置（仅主监护人或 SUPERADMIN）。
     */
    @Transactional
    public PatientEntity updateFence(Long patientId, Long operatorUserId, boolean isAdmin,
                                     Boolean fenceEnabled, Double lat, Double lng, Integer radiusM) {
        PatientEntity patient = requirePatient(patientId);
        if (!isAdmin) {
            requirePrimaryGuardian(patientId, operatorUserId);
        }
        patient.updateFence(fenceEnabled, lat, lng, radiusM);
        return patientRepository.updateFence(patient);
    }

    /**
     * 按 ID 获取患者信息（内部使用，调用方自行负责授权）。
     */
    public PatientEntity getPatientById(Long patientId) {
        return patientRepository.findById(patientId)
                .orElseThrow(() -> BizException.of("E_PAT_4041"));
    }

    /**
     * 按短码获取患者（供公开入口使用）。
     */
    public PatientEntity getPatientByShortCode(String shortCode) {
        return patientRepository.findByShortCode(shortCode)
                .orElseThrow(() -> BizException.of("E_PAT_4041"));
    }

    /**
     * 验证短码 + PIN 码（供手动入口使用）。
     * PIN 正确则返回患者实体；否则抛出 E_AUTH_4001。
     */
    public PatientEntity verifyShortCodePin(String shortCode, String pinCode) {
        PatientEntity patient = getPatientByShortCode(shortCode);
        if (!BCrypt.checkpw(pinCode, patient.getPinCodeHash())) {
            throw BizException.of("E_AUTH_4001");
        }
        return patient;
    }

    // ===== 内部工具 =====

    private PatientEntity requirePatient(Long patientId) {
        return patientRepository.findById(patientId)
                .orElseThrow(() -> BizException.of("E_PAT_4041"));
    }

    private void requirePrimaryGuardian(Long patientId, Long userId) {
        GuardianRelationEntity rel = guardianRepository.findPrimaryByPatientId(patientId)
                .orElseThrow(() -> BizException.of("E_TASK_4030"));
        if (!rel.getUserId().equals(userId)) throw BizException.of("E_TASK_4030");
    }
}
