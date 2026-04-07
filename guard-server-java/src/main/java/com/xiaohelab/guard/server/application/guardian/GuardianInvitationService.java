package com.xiaohelab.guard.server.application.guardian;

import com.xiaohelab.guard.server.common.exception.BizException;
import com.xiaohelab.guard.server.common.util.IdGenerator;
import com.xiaohelab.guard.server.infrastructure.persistence.do_.GuardianInvitationDO;
import com.xiaohelab.guard.server.infrastructure.persistence.do_.SysUserPatientDO;
import com.xiaohelab.guard.server.infrastructure.persistence.mapper.GuardianInvitationMapper;
import com.xiaohelab.guard.server.infrastructure.persistence.mapper.SysUserMapper;
import com.xiaohelab.guard.server.infrastructure.persistence.mapper.SysUserPatientMapper;
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

    private final GuardianInvitationMapper invitationMapper;
    private final SysUserPatientMapper sysUserPatientMapper;
    private final SysUserMapper sysUserMapper;

    /**
     * 发起邀请（仅主监护人可操作）。
     */
    @Transactional
    public GuardianInvitationDO createInvitation(Long patientId, Long inviterUserId,
                                                   Long inviteeUserId, String relationRole,
                                                   String reason) {
        // 校验：调用方必须是主监护人
        requirePrimaryGuardian(patientId, inviterUserId);

        // 校验：被邀请人存在
        if (sysUserMapper.findById(inviteeUserId) == null) {
            throw BizException.of("E_USER_4041");
        }

        // 防重复：对同一被邀请人当前已有 PENDING 邀请
        GuardianInvitationDO existing = invitationMapper.findPendingByPatientAndInvitee(patientId, inviteeUserId);
        if (existing != null) throw BizException.of("E_INV_4091");

        // 校验：被邀请人是否已是该患者的监护人
        if (sysUserPatientMapper.countActiveRelation(inviteeUserId, patientId) > 0) {
            throw BizException.of("E_INV_4092");
        }

        GuardianInvitationDO inv = new GuardianInvitationDO();
        inv.setInviteId(IdGenerator.inviteId());
        inv.setPatientId(patientId);
        inv.setInviterUserId(inviterUserId);
        inv.setInviteeUserId(inviteeUserId);
        inv.setRelationRole(relationRole);
        inv.setStatus("PENDING");
        inv.setReason(reason);
        inv.setExpireAt(Instant.now().plus(7, ChronoUnit.DAYS));
        invitationMapper.insert(inv);
        return inv;
    }

    /**
     * 响应邀请（ACCEPT 或 REJECT，仅被邀请人可操作）。
     * ACCEPT 时同事务激活 sys_user_patient 关联。
     */
    @Transactional
    public GuardianInvitationDO respondInvitation(String inviteId, Long inviteeUserId, boolean accept, String reason) {
        GuardianInvitationDO inv = invitationMapper.findByInviteId(inviteId);
        if (inv == null) throw BizException.of("E_INV_4041");
        if (!"PENDING".equals(inv.getStatus())) throw BizException.of("E_INV_4093");
        if (!inv.getInviteeUserId().equals(inviteeUserId)) throw BizException.of("E_TASK_4030");

        inv.setStatus(accept ? "ACCEPTED" : "REJECTED");
        inv.setRejectReason(accept ? null : reason);
        int updated = invitationMapper.respond(inv);
        if (updated == 0) throw BizException.of("E_INV_4093");

        if (accept) {
            SysUserPatientDO rel = new SysUserPatientDO();
            rel.setUserId(inv.getInviteeUserId());
            rel.setPatientId(inv.getPatientId());
            rel.setRelationRole(inv.getRelationRole());
            rel.setRelationStatus("ACTIVE");
            rel.setTransferState("NONE");
            sysUserPatientMapper.insert(rel);
        }
        return inv;
    }

    /**
     * 撤销邀请（仅邀请发起人可撤销）。
     */
    @Transactional
    public void revokeInvitation(String inviteId, Long operatorUserId) {
        GuardianInvitationDO inv = invitationMapper.findByInviteId(inviteId);
        if (inv == null) throw BizException.of("E_INV_4041");
        if (!inv.getInviterUserId().equals(operatorUserId)) throw BizException.of("E_TASK_4030");
        if (invitationMapper.revoke(inv.getId()) == 0) throw BizException.of("E_INV_4093");
    }

    /**
     * 移除监护人（仅主监护人或 SUPERADMIN 可操作）。
     */
    @Transactional
    public void removeGuardian(Long patientId, Long targetUserId, Long operatorUserId, boolean isAdmin) {
        if (!isAdmin) {
            requirePrimaryGuardian(patientId, operatorUserId);
        }
        SysUserPatientDO rel = sysUserPatientMapper.findByUserIdAndPatientId(targetUserId, patientId);
        if (rel == null || !"ACTIVE".equals(rel.getRelationStatus())) {
            throw BizException.of("E_MEMBER_4041");
        }
        if ("PRIMARY_GUARDIAN".equals(rel.getRelationRole())) {
            throw BizException.of("E_TASK_4030");  // 不允许直接移除主监护人
        }
        sysUserPatientMapper.updateRelationStatus(rel.getId(), "REVOKED");
    }

    /**
     * 发起主监护人转移（仅当前主监护人可操作）。
     */
    @Transactional
    public SysUserPatientDO initiateTransfer(Long patientId, Long requestedBy,
                                              Long targetUserId, String reason) {
        SysUserPatientDO primary = requirePrimaryGuardian(patientId, requestedBy);
        // 目标用户必须是 ACTIVE 普通监护人
        SysUserPatientDO targetRel = sysUserPatientMapper.findByUserIdAndPatientId(targetUserId, patientId);
        if (targetRel == null || !"ACTIVE".equals(targetRel.getRelationStatus())) {
            throw BizException.of("E_MEMBER_4041");
        }
        primary.setTransferRequestId(IdGenerator.transferRequestId());
        primary.setTransferTargetUserId(targetUserId);
        primary.setTransferRequestedBy(requestedBy);
        primary.setTransferReason(reason);
        primary.setTransferExpireAt(Instant.now().plus(3, ChronoUnit.DAYS));
        if (sysUserPatientMapper.initiateTransfer(primary) == 0) {
            throw BizException.of("E_TASK_4030");
        }
        return sysUserPatientMapper.findById(primary.getId());
    }

    /**
     * 确认主监护人转移（目标用户接受或拒绝）。
     * ACCEPT：互换角色，清空转移字段。
     * REJECT：重置 transfer_state=NONE。
     */
    @Transactional
    public SysUserPatientDO confirmTransfer(String transferRequestId, Long confirmUserId, boolean accept, String rejectReason) {
        SysUserPatientDO primary = sysUserPatientMapper.findByTransferRequestId(transferRequestId);
        if (primary == null) throw BizException.of("E_TRANSFER_4041");
        if (!"PENDING_CONFIRM".equals(primary.getTransferState())) throw BizException.of("E_TRANSFER_4093");
        if (!primary.getTransferTargetUserId().equals(confirmUserId)) throw BizException.of("E_TASK_4030");

        if (accept) {
            // 1. 原主监护人角色降级
            sysUserPatientMapper.updateRole(primary.getId(), "GUARDIAN");
            // 2. 新主监护人角色升级
            SysUserPatientDO targetRel = sysUserPatientMapper.findByUserIdAndPatientId(confirmUserId, primary.getPatientId());
            sysUserPatientMapper.updateRole(targetRel.getId(), "PRIMARY_GUARDIAN");
            // 3. 记录确认时间
            SysUserPatientDO updRecord = new SysUserPatientDO();
            updRecord.setId(primary.getId());
            updRecord.setTransferState("ACCEPTED");
            updRecord.setTransferConfirmedAt(Instant.now());
            sysUserPatientMapper.updateTransferState(updRecord);
        } else {
            SysUserPatientDO updRecord = new SysUserPatientDO();
            updRecord.setId(primary.getId());
            updRecord.setTransferState("REJECTED");
            updRecord.setTransferRejectedAt(Instant.now());
            updRecord.setTransferRejectReason(rejectReason);
            sysUserPatientMapper.updateTransferState(updRecord);
        }
        return sysUserPatientMapper.findById(primary.getId());
    }

    /**
     * 取消主监护人转移（发起人主动取消）。
     */
    @Transactional
    public void cancelTransfer(String transferRequestId, Long cancelledBy, String cancelReason) {
        SysUserPatientDO primary = sysUserPatientMapper.findByTransferRequestId(transferRequestId);
        if (primary == null) throw BizException.of("E_TRANSFER_4041");
        if (!"PENDING_CONFIRM".equals(primary.getTransferState())) throw BizException.of("E_TRANSFER_4093");
        if (!primary.getTransferRequestedBy().equals(cancelledBy)) throw BizException.of("E_TASK_4030");

        SysUserPatientDO upd = new SysUserPatientDO();
        upd.setId(primary.getId());
        upd.setTransferState("CANCELLED");
        upd.setTransferCancelledBy(cancelledBy);
        upd.setTransferCancelledAt(Instant.now());
        upd.setTransferCancelReason(cancelReason);
        sysUserPatientMapper.updateTransferState(upd);
    }

    /** 查询患者的邀请列表 */
    public List<GuardianInvitationDO> listInvitations(Long patientId, int pageNo, int pageSize) {
        return invitationMapper.listByPatient(patientId, pageSize, (pageNo - 1) * pageSize);
    }

    public long countInvitations(Long patientId) {
        return invitationMapper.countByPatient(patientId);
    }

    /** 查询用户收到的 PENDING 邀请 */
    public List<GuardianInvitationDO> listMyPendingInvitations(Long userId, int pageNo, int pageSize) {
        return invitationMapper.listPendingByInvitee(userId, pageSize, (pageNo - 1) * pageSize);
    }

    /** 查询患者的活跃监护人列表 */
    public List<SysUserPatientDO> listGuardians(Long patientId) {
        return sysUserPatientMapper.listActiveByPatientId(patientId);
    }

    // ===== 内部工具 =====

    private SysUserPatientDO requirePrimaryGuardian(Long patientId, Long userId) {
        SysUserPatientDO rel = sysUserPatientMapper.findPrimaryByPatientId(patientId);
        if (rel == null || !rel.getUserId().equals(userId)) {
            throw BizException.of("E_TASK_4030");
        }
        return rel;
    }
}
