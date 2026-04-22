package com.xiaohelab.guard.server.patient.repository;

import com.xiaohelab.guard.server.patient.entity.GuardianRelationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GuardianRelationRepository extends JpaRepository<GuardianRelationEntity, Long> {

    List<GuardianRelationEntity> findByPatientIdAndRelationStatus(Long patientId, String relationStatus);

    List<GuardianRelationEntity> findByUserIdAndRelationStatus(Long userId, String relationStatus);

    Optional<GuardianRelationEntity> findByUserIdAndPatientIdAndRelationStatus(Long userId, Long patientId, String relationStatus);

    Optional<GuardianRelationEntity> findByPatientIdAndRelationRoleAndRelationStatus(
            Long patientId, String role, String status);

    // ================= V2.1 管理员治理 =================

    /**
     * 查询目标用户当前作为 PRIMARY_GUARDIAN 且关系 ACTIVE 的患者 id 列表。
     * <p>用于禁用/注销前置校验与 user.disabled 事件 payload.primary_patient_ids。</p>
     */
    @Query("select g.patientId from GuardianRelationEntity g " +
            "where g.userId = :userId and g.relationRole = 'PRIMARY_GUARDIAN' and g.relationStatus = 'ACTIVE'")
    List<Long> findPrimaryActivePatientIds(@Param("userId") Long userId);

    /**
     * 注销用户时批量撤销其非主监护的 ACTIVE 关系。
     */
    @Modifying
    @Query("update GuardianRelationEntity g set g.relationStatus = 'REVOKED', g.revokedAt = CURRENT_TIMESTAMP " +
            "where g.userId = :userId and g.relationRole <> 'PRIMARY_GUARDIAN' and g.relationStatus = 'ACTIVE'")
    int revokeNonPrimaryForUser(@Param("userId") Long userId);
}
