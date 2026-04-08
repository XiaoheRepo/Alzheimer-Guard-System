package com.xiaohelab.guard.server.domain.guardian.repository;

import com.xiaohelab.guard.server.domain.guardian.entity.GuardianInvitationEntity;

import java.util.List;
import java.util.Optional;

/**
 * 监护人邀请 Repository 接口（领域层定义）。
 * 对应 guardian_invitation 表。
 */
public interface GuardianInvitationRepository {

    Optional<GuardianInvitationEntity> findById(Long id);

    Optional<GuardianInvitationEntity> findByInviteId(String inviteId);

    Optional<GuardianInvitationEntity> findPendingByPatientAndInvitee(Long patientId, Long inviteeUserId);

    List<GuardianInvitationEntity> listByPatient(Long patientId, int limit, int offset);

    long countByPatient(Long patientId);

    List<GuardianInvitationEntity> listPendingByInvitee(Long inviteeUserId, int limit, int offset);

    void insert(GuardianInvitationEntity entity);

    /** 响应邀请（ACCEPT 或 REJECT，仅 PENDING 可操作）。 */
    int respond(GuardianInvitationEntity entity);

    /** 撤销邀请（REVOKE，仅 PENDING 可操作）。 */
    int revoke(Long id);

    /** 批量过期（调度任务使用）。 */
    int expirePending();
}
