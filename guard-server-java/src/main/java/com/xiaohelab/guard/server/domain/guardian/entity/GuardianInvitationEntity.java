package com.xiaohelab.guard.server.domain.guardian.entity;

import com.xiaohelab.guard.server.common.exception.BizException;
import com.xiaohelab.guard.server.infrastructure.persistence.do_.GuardianInvitationDO;
import lombok.Getter;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * 监护人邀请聚合根。
 * 状态机：PENDING → ACCEPTED / REJECTED / EXPIRED / REVOKED
 * ACCEPTED 后必须同事务激活 sys_user_patient。
 */
@Getter
public class GuardianInvitationEntity {

    private Long id;
    /** 全局唯一邀请号 */
    private String inviteId;
    private Long patientId;
    private Long inviterUserId;
    private Long inviteeUserId;
    /** PRIMARY_GUARDIAN / GUARDIAN */
    private String relationRole;
    /** PENDING / ACCEPTED / REJECTED / EXPIRED / REVOKED */
    private String status;
    private String reason;
    private String rejectReason;
    private Instant expireAt;
    private Instant acceptedAt;
    private Instant rejectedAt;
    private Instant revokedAt;
    private Instant createdAt;
    private Instant updatedAt;

    private GuardianInvitationEntity() {}

    public static GuardianInvitationEntity fromDO(GuardianInvitationDO d) {
        GuardianInvitationEntity e = new GuardianInvitationEntity();
        e.id = d.getId();
        e.inviteId = d.getInviteId();
        e.patientId = d.getPatientId();
        e.inviterUserId = d.getInviterUserId();
        e.inviteeUserId = d.getInviteeUserId();
        e.relationRole = d.getRelationRole();
        e.status = d.getStatus();
        e.reason = d.getReason();
        e.rejectReason = d.getRejectReason();
        e.expireAt = d.getExpireAt();
        e.acceptedAt = d.getAcceptedAt();
        e.rejectedAt = d.getRejectedAt();
        e.revokedAt = d.getRevokedAt();
        e.createdAt = d.getCreatedAt();
        e.updatedAt = d.getUpdatedAt();
        return e;
    }

    /** 创建新邀请。 */
    public static GuardianInvitationEntity create(String inviteId, Long patientId,
                                                   Long inviterUserId, Long inviteeUserId,
                                                   String relationRole, String reason) {
        GuardianInvitationEntity e = new GuardianInvitationEntity();
        e.inviteId = inviteId;
        e.patientId = patientId;
        e.inviterUserId = inviterUserId;
        e.inviteeUserId = inviteeUserId;
        e.relationRole = relationRole;
        e.status = "PENDING";
        e.reason = reason;
        e.expireAt = Instant.now().plus(7, ChronoUnit.DAYS);
        return e;
    }

    /** 接受邀请，返回 self。 */
    public GuardianInvitationEntity accept() {
        if (!"PENDING".equals(this.status)) throw BizException.of("E_INV_4093");
        this.status = "ACCEPTED";
        this.acceptedAt = Instant.now();
        return this;
    }

    /** 拒绝邀请。 */
    public GuardianInvitationEntity reject(String rejectReason) {
        if (!"PENDING".equals(this.status)) throw BizException.of("E_INV_4093");
        this.status = "REJECTED";
        this.rejectReason = rejectReason;
        this.rejectedAt = Instant.now();
        return this;
    }

    public GuardianInvitationDO toDO() {
        GuardianInvitationDO d = new GuardianInvitationDO();
        d.setId(this.id);
        d.setInviteId(this.inviteId);
        d.setPatientId(this.patientId);
        d.setInviterUserId(this.inviterUserId);
        d.setInviteeUserId(this.inviteeUserId);
        d.setRelationRole(this.relationRole);
        d.setStatus(this.status);
        d.setReason(this.reason);
        d.setRejectReason(this.rejectReason);
        d.setExpireAt(this.expireAt);
        d.setAcceptedAt(this.acceptedAt);
        d.setRejectedAt(this.rejectedAt);
        d.setRevokedAt(this.revokedAt);
        return d;
    }
}
