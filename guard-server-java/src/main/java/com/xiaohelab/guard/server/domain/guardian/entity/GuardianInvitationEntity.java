package com.xiaohelab.guard.server.domain.guardian.entity;

import com.xiaohelab.guard.server.common.exception.BizException;
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

    /** 从持久化数据重建（仅 Infrastructure 层 RepositoryImpl 调用）。 */
    public static GuardianInvitationEntity reconstitute(
            Long id, String inviteId, Long patientId, Long inviterUserId, Long inviteeUserId,
            String relationRole, String status, String reason, String rejectReason,
            Instant expireAt, Instant acceptedAt, Instant rejectedAt, Instant revokedAt,
            Instant createdAt, Instant updatedAt) {
        GuardianInvitationEntity e = new GuardianInvitationEntity();
        e.id = id;
        e.inviteId = inviteId;
        e.patientId = patientId;
        e.inviterUserId = inviterUserId;
        e.inviteeUserId = inviteeUserId;
        e.relationRole = relationRole;
        e.status = status;
        e.reason = reason;
        e.rejectReason = rejectReason;
        e.expireAt = expireAt;
        e.acceptedAt = acceptedAt;
        e.rejectedAt = rejectedAt;
        e.revokedAt = revokedAt;
        e.createdAt = createdAt;
        e.updatedAt = updatedAt;
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
}
