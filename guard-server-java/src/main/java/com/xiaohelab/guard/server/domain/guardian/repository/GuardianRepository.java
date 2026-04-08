package com.xiaohelab.guard.server.domain.guardian.repository;

import com.xiaohelab.guard.server.domain.guardian.entity.GuardianRelationEntity;

import java.util.List;
import java.util.Optional;

/**
 * 监护人关系 Repository 接口（领域层定义）。
 * 对应 sys_user_patient 表。
 */
public interface GuardianRepository {

    Optional<GuardianRelationEntity> findById(Long id);

    Optional<GuardianRelationEntity> findByUserIdAndPatientId(Long userId, Long patientId);

    Optional<GuardianRelationEntity> findPrimaryByPatientId(Long patientId);

    Optional<GuardianRelationEntity> findByTransferRequestId(String transferRequestId);

    List<GuardianRelationEntity> listActiveByPatientId(Long patientId);

    List<GuardianRelationEntity> listByUserId(Long userId, int limit, int offset);

    long countByUserId(Long userId);

    long countActiveRelation(Long userId, Long patientId);

    void insert(GuardianRelationEntity entity);

    int updateRelationStatus(Long id, String relationStatus);

    int initiateTransfer(GuardianRelationEntity entity);

    int updateTransferState(GuardianRelationEntity entity);

    int updateRole(Long id, String relationRole);

    List<GuardianRelationEntity> listTransfersByPatientId(Long patientId, String state, int limit, int offset);

    long countTransfersByPatientId(Long patientId, String state);
}
