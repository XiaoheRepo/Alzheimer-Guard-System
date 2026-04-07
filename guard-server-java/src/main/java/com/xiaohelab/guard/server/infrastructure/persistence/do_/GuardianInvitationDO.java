package com.xiaohelab.guard.server.infrastructure.persistence.do_;

import lombok.Data;

import java.time.Instant;

/**
 * guardian_invitation 持久化对象（监护邀请生命周期）。
 * 状态流转：PENDING → ACCEPTED / REJECTED / EXPIRED / REVOKED。
 * ACCEPTED 后必须同事务激活 sys_user_patient。
 */
@Data
public class GuardianInvitationDO {

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
    /** REJECTED / REVOKED 时必填 */
    private String rejectReason;
    private Instant expireAt;
    private Instant acceptedAt;
    private Instant rejectedAt;
    private Instant revokedAt;
    private Instant createdAt;
    private Instant updatedAt;
}
