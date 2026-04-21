package com.xiaohelab.guard.server.patient.repository;

import com.xiaohelab.guard.server.patient.entity.GuardianInvitationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GuardianInvitationRepository extends JpaRepository<GuardianInvitationEntity, Long> {

    Optional<GuardianInvitationEntity> findByInviteId(String inviteId);

    Optional<GuardianInvitationEntity> findByPatientIdAndInviteeUserIdAndStatus(Long patientId, Long inviteeUserId, String status);

    List<GuardianInvitationEntity> findByPatientIdOrderByCreatedAtDesc(Long patientId);
}
