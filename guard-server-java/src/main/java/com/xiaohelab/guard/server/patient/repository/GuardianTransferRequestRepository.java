package com.xiaohelab.guard.server.patient.repository;

import com.xiaohelab.guard.server.patient.entity.GuardianTransferRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GuardianTransferRequestRepository extends JpaRepository<GuardianTransferRequestEntity, Long> {

    Optional<GuardianTransferRequestEntity> findByRequestId(String requestId);

    Optional<GuardianTransferRequestEntity> findByPatientIdAndStatus(Long patientId, String status);

    List<GuardianTransferRequestEntity> findByPatientIdOrderByCreatedAtDesc(Long patientId);
}
