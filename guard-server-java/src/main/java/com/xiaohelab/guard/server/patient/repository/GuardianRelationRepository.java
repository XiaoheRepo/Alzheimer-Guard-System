package com.xiaohelab.guard.server.patient.repository;

import com.xiaohelab.guard.server.patient.entity.GuardianRelationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GuardianRelationRepository extends JpaRepository<GuardianRelationEntity, Long> {

    List<GuardianRelationEntity> findByPatientIdAndRelationStatus(Long patientId, String relationStatus);

    List<GuardianRelationEntity> findByUserIdAndRelationStatus(Long userId, String relationStatus);

    Optional<GuardianRelationEntity> findByUserIdAndPatientIdAndRelationStatus(Long userId, Long patientId, String relationStatus);

    Optional<GuardianRelationEntity> findByPatientIdAndRelationRoleAndRelationStatus(
            Long patientId, String role, String status);
}
