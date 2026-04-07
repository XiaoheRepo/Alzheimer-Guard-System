package com.xiaohelab.guard.server.application.patient;

import com.xiaohelab.guard.server.common.exception.BizException;
import com.xiaohelab.guard.server.common.util.IdGenerator;
import com.xiaohelab.guard.server.infrastructure.persistence.do_.PatientProfileDO;
import com.xiaohelab.guard.server.infrastructure.persistence.do_.SysUserPatientDO;
import com.xiaohelab.guard.server.infrastructure.persistence.mapper.PatientProfileMapper;
import com.xiaohelab.guard.server.infrastructure.persistence.mapper.SysUserPatientMapper;
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

    private final PatientProfileMapper patientProfileMapper;
    private final SysUserPatientMapper sysUserPatientMapper;

    /**
     * 创建患者档案，并将创建者设为 PRIMARY_GUARDIAN。
     * 同事务写 patient_profile + sys_user_patient。
     */
    @Transactional
    public PatientProfileDO createPatient(Long creatorUserId,
                                           String name,
                                           String gender,
                                           LocalDate birthday,
                                           String photoUrl,
                                           String medicalHistory,
                                           String pinCode) {
        // 1. 生成档案编号与 short_code
        String profileNo = IdGenerator.profileNo();
        long seqVal = patientProfileMapper.nextShortCodeSeq();
        String shortCode = String.format("%06d", seqVal % 1_000_000);

        // 2. PIN 码哈希
        String salt = BCrypt.gensalt();
        String pinHash = BCrypt.hashpw(pinCode, salt);

        // 3. 构建并插入 patient_profile
        PatientProfileDO profile = new PatientProfileDO();
        profile.setProfileNo(profileNo);
        profile.setName(name);
        profile.setGender(gender);
        profile.setBirthday(birthday);
        profile.setShortCode(shortCode);
        profile.setPinCodeHash(pinHash);
        profile.setPinCodeSalt(salt);
        profile.setPhotoUrl(photoUrl);
        profile.setMedicalHistory(medicalHistory != null ? medicalHistory : "{}");
        profile.setFenceEnabled(false);
        profile.setLostStatus("NORMAL");
        profile.setProfileVersion(1L);
        patientProfileMapper.insert(profile);

        // 4. 建立 PRIMARY_GUARDIAN 关系
        SysUserPatientDO rel = new SysUserPatientDO();
        rel.setUserId(creatorUserId);
        rel.setPatientId(profile.getId());
        rel.setRelationRole("PRIMARY_GUARDIAN");
        rel.setRelationStatus("ACTIVE");
        rel.setTransferState("NONE");
        sysUserPatientMapper.insert(rel);

        return profile;
    }

    /**
     * 更新患者基本资料（仅主监护人或 SUPERADMIN）。
     */
    @Transactional
    public PatientProfileDO updatePatient(Long patientId, Long operatorUserId, boolean isAdmin,
                                          String name, String gender, LocalDate birthday,
                                          String photoUrl, String medicalHistory) {
        PatientProfileDO profile = requireProfile(patientId);
        if (!isAdmin) {
            requirePrimaryGuardian(patientId, operatorUserId);
        }
        profile.setName(name);
        profile.setGender(gender);
        profile.setBirthday(birthday);
        profile.setPhotoUrl(photoUrl);
        profile.setMedicalHistory(medicalHistory != null ? medicalHistory : profile.getMedicalHistory());
        patientProfileMapper.update(profile);
        return patientProfileMapper.findById(patientId);
    }

    /**
     * 查询患者档案（需关联或管理员权限）。
     */
    public PatientProfileDO getPatient(Long patientId, Long userId, boolean isAdmin) {
        PatientProfileDO profile = requireProfile(patientId);
        if (!isAdmin && sysUserPatientMapper.countActiveRelation(userId, patientId) == 0) {
            throw BizException.of("E_TASK_4030");
        }
        return profile;
    }

    /**
     * 查询用户关联的所有患者列表。
     */
    public List<PatientProfileDO> listMyPatients(Long userId) {
        return patientProfileMapper.findByUserId(userId);
    }

    /**
     * 更新围栏配置（仅主监护人或 SUPERADMIN）。
     */
    @Transactional
    public PatientProfileDO updateFence(Long patientId, Long operatorUserId, boolean isAdmin,
                                        Boolean fenceEnabled, Double lat, Double lng, Integer radiusM) {
        requireProfile(patientId);
        if (!isAdmin) {
            requirePrimaryGuardian(patientId, operatorUserId);
        }
        PatientProfileDO upd = new PatientProfileDO();
        upd.setId(patientId);
        upd.setFenceEnabled(fenceEnabled);
        upd.setFenceCenterLat(lat);
        upd.setFenceCenterLng(lng);
        upd.setFenceRadiusM(radiusM);
        patientProfileMapper.updateFence(upd);
        return patientProfileMapper.findById(patientId);
    }

    // ===== 内部工具 =====

    private PatientProfileDO requireProfile(Long patientId) {
        PatientProfileDO p = patientProfileMapper.findById(patientId);
        if (p == null) throw BizException.of("E_PAT_4041");
        return p;
    }

    private void requirePrimaryGuardian(Long patientId, Long userId) {
        SysUserPatientDO rel = sysUserPatientMapper.findPrimaryByPatientId(patientId);
        if (rel == null || !rel.getUserId().equals(userId)) {
            throw BizException.of("E_TASK_4030");
        }
    }
}
