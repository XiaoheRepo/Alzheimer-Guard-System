package com.xiaohelab.guard.server.application.guardian;

import com.xiaohelab.guard.server.common.exception.BizException;
import com.xiaohelab.guard.server.common.util.IdGenerator;
import com.xiaohelab.guard.server.domain.governance.repository.SysUserRepository;
import com.xiaohelab.guard.server.domain.guardian.entity.GuardianInvitationEntity;
import com.xiaohelab.guard.server.domain.guardian.entity.GuardianRelationEntity;
import com.xiaohelab.guard.server.domain.guardian.repository.GuardianInvitationRepository;
import com.xiaohelab.guard.server.domain.guardian.repository.GuardianRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 监护人邀请与关系管理服务。
 * 邀请状态机：PENDING → ACCEPTED / REJECTED / EXPIRED / REVOKED
 * 主监护人转移状态机：NONE → PENDING_CONFIRM → ACCEPTED / REJECTED / CANCELLED
 */
@Service
@RequiredArgsConstructor
public class GuardianInvitationService {

    private final GuardianInvitationRepository invitationRepository;
    private final GuardianRepository guardianRepository;
    private final SysUserRepository sysUserRepository;

    /**
     * 发起邀请（仅主监护人可操作）。
     */
    /**
     * 发起邀请（仅主监护人可操作）。
     */
    @Transactional
    public GuardianInvitationEntity createInvitation(Long patientId, Long inviterUserId,
                                                      Long inviteeUserId, String relationRole,
                                                      String reason) {
        requirePrimaryGuardian(patientId, inviterUserId);

        if (sysUserRepository.findById(inviteeUserId).isEmpty()) {
            throw BizException.of("E_USER_4041");
        }

        if (invitationRepository.findPendingByPatientAndInvitee(patientId, inviteeUserId).isPresent()) {
            throw BizException.of("E_INV_4091");
        }
        if (guardianRepository.countActiveRelation(inviteeUserId, patientId) > 0) {
            throw BizException.of("E_INV_4092");
        }

        GuardianInvitationEntity inv = GuardianInvitationEntity.create(
                IdGenerator.inviteId(), patientId, inviterUserId, inviteeUserId, relationRole, reason);
        invitationRepository.insert(inv);
        return invitationRepository.findByInviteId(inv.getInviteId()).orElse(inv);
    }

    /**
     * 响应邀请（ACCEPT 或 REJECT，仅被邀请人可操作）。
     * ACCEPT 时同事务激活 sys_user_patient 关联。
     */
    @Transactional
    public GuardianInvitationEntity respondInvitation(String inviteId, Long inviteeUserId,
                                                       boolean accept, String reason) {
        GuardianInvitationEntity inv = invitationRepository.findByInviteId(inviteId)
                .orElseThrow(() -> BizException.of("E_INV_4041"));
        if (!inv.getInviteeUserId().equals(inviteeUserId)) throw BizException.of("E_TASK_4030");

        if (accept) inv.accept(); else inv.reject(reason);
        int updated = invitationRepository.respond(inv);
        if (updated == 0) throw BizException.of("E_INV_4093");

        if (accept) {
            GuardianRelationEntity rel = GuardianRelationEntity.create(
                    inv.getInviteeUserId(), inv.getPatientId(), inv.getRelationRole());
            guardianRepository.insert(rel);
        }
        return inv;
    }

    /**
     * 撤销邀请（仅邀请发起人可撤销）。
     */
    @Transactional
    public void revokeInvitation(String inviteId, Long operatorUserId) {
        GuardianInvitationEntity inv = invitationRepository.findByInviteId(inviteId)
                .orElseThrow(() -> BizException.of("E_INV_4041"));
        if (!inv.getInviterUserId().equals(operatorUserId)) throw BizException.of("E_TASK_4030");
        if (invitationRepository.revoke(inv.getId()) == 0) throw BizException.of("E_INV_4093");
    }

    /**
     * 移除监护人（仅主监护人或 SUPERADMIN 可操作）。
     */
    @Transactional
    public void removeGuardian(Long patientId, Long targetUserId, Long operatorUserId, boolean isAdmin) {
        if (!isAdmin) requirePrimaryGuardian(patientId, operatorUserId);
        GuardianRelationEntity rel = guardianRepository.findByUserIdAndPatientId(targetUserId, patientId)
                .orElseThrow(() -> BizException.of("E_MEMBER_4041"));
        if (!"ACTIVE".equals(rel.getRelationStatus())) throw BizException.of("E_MEMBER_4041");
        rel.revoke();
        guardianRepository.updateRelationStatus(rel.getId(), rel.getRelationStatus());
    }

    /**
     * 发起主监护人转移（仅当前主监护人可操作）。
     */
    @Transactional
    public GuardianRelationEntity initiateTransfer(Long patientId, Long requestedBy,
                                                    Long targetUserId, String reason) {
        GuardianRelationEntity primary = requirePrimaryGuardian(patientId, requestedBy);
        guardianRepository.findByUserIdAndPatientId(targetUserId, patientId)
                .filter(r -> "ACTIVE".equals(r.getRelationStatus()))
                .orElseThrow(() -> BizException.of("E_MEMBER_4041"));
        primary.initiateTransfer(IdGenerator.transferRequestId(), targetUserId, requestedBy, reason);
        if (guardianRepository.initiateTransfer(primary) == 0) throw BizException.of("E_TASK_4030");
        return guardianRepository.findById(primary.getId()).orElse(primary);
    }

    /**
     * 确认主监护人转移（目标用户接受或拒绝）。
     */
    @Transactional
    public GuardianRelationEntity confirmTransfer(String transferRequestId, Long confirmUserId,
                                                   boolean accept, String rejectReason) {
        GuardianRelationEntity primary = guardianRepository.findByTransferRequestId(transferRequestId)
                .orElseThrow(() -> BizException.of("E_TRANSFER_4041"));
        if (!primary.getTransferTargetUserId().equals(confirmUserId)) throw BizException.of("E_TASK_4030");

        if (accept) {
            primary.confirmTransfer();
            guardianRepository.updateTransferState(primary);
            guardianRepository.updateRole(primary.getId(), "GUARDIAN");
            GuardianRelationEntity targetRel = guardianRepository
                    .findByUserIdAndPatientId(confirmUserId, primary.getPatientId())
                    .orElseThrow(() -> BizException.of("E_MEMBER_4041"));
            guardianRepository.updateRole(targetRel.getId(), "PRIMARY_GUARDIAN");
        } else {
            primary.rejectTransfer(rejectReason);
            guardianRepository.updateTransferState(primary);
        }
        return guardianRepository.findById(primary.getId()).orElse(primary);
    }

    /**
     * 取消主监护人转移（发起人主动取消）。
     */
    @Transactional
    public void cancelTransfer(String transferRequestId, Long cancelledBy, String cancelReason) {
        GuardianRelationEntity primary = guardianRepository.findByTransferRequestId(transferRequestId)
                .orElseThrow(() -> BizException.of("E_TRANSFER_4041"));
        if (!primary.getTransferRequestedBy().equals(cancelledBy)) throw BizException.of("E_TASK_4030");
        primary.cancelTransfer(cancelledBy, cancelReason);
        guardianRepository.updateTransferState(primary);
    }

    public List<GuardianInvitationEntity> listInvitations(Long patientId, int pageNo, int pageSize) {
        return invitationRepository.listByPatient(patientId, pageSize, (pageNo - 1) * pageSize);
    }

    public long countInvitations(Long patientId) {
        return invitationRepository.countByPatient(patientId);
    }

    public List<GuardianInvitationEntity> listMyPendingInvitations(Long userId, int pageNo, int pageSize) {
        return invitationRepository.listPendingByInvitee(userId, pageSize, (pageNo - 1) * pageSize);
    }

    public List<GuardianRelationEntity> listGuardians(Long patientId) {
        return guardianRepository.listActiveByPatientId(patientId);
    }

    public GuardianInvitationEntity getInvitation(String inviteId) {
        return invitationRepository.findByInviteId(inviteId)
                .orElseThrow(() -> BizException.of("E_PRO_4043"));
    }

    public GuardianRelationEntity getTransferDetail(String transferRequestId) {
        return guardianRepository.findByTransferRequestId(transferRequestId)
                .orElseThrow(() -> BizException.of("E_PRO_4045"));
    }

    public List<GuardianRelationEntity> listTransfers(Long patientId, String state, int pageNo, int pageSize) {
        return guardianRepository.listTransfersByPatientId(patientId, state, pageSize, (pageNo - 1) * pageSize);
    }

    public long countTransfers(Long patientId, String state) {
        return guardianRepository.countTransfersByPatientId(patientId, state);
    }

    public boolean hasActiveRelation(Long userId, Long patientId) {
        return guardianRepository.countActiveRelation(userId, patientId) > 0;
    }

    // ===== 内部工具 =====

    private GuardianRelationEntity requirePrimaryGuardian(Long patientId, Long userId) {
        GuardianRelationEntity rel = guardianRepository.findPrimaryByPatientId(patientId)
                .orElseThrow(() -> BizException.of("E_TASK_4030"));
        if (!rel.getUserId().equals(userId)) throw BizException.of("E_TASK_4030");
        return rel;
    }
}
