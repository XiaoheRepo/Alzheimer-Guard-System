package com.xiaohelab.guard.server.domain.guardian.entity;

import com.xiaohelab.guard.server.common.exception.BizException;
import lombok.Getter;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * 监护人关系聚合根（sys_user_patient 实体）。
 * 关系状态机：PENDING / ACTIVE / REVOKED
 * 转移状态机：NONE → PENDING_CONFIRM → ACCEPTED / REJECTED / CANCELLED / EXPIRED
 */
@Getter
public class GuardianRelationEntity {

    private Long id;
    private Long userId;
    private Long patientId;
    /** PRIMARY_GUARDIAN / GUARDIAN */
    private String relationRole;
    /** PENDING / ACTIVE / REVOKED */
    private String relationStatus;
    /** NONE / PENDING_CONFIRM / ACCEPTED / REJECTED / CANCELLED / EXPIRED */
    private String transferState;
    private String transferRequestId;
    private Long transferTargetUserId;
    private Long transferRequestedBy;
    private Instant transferRequestedAt;
    private String transferReason;
    private Long transferCancelledBy;
    private Instant transferCancelledAt;
    private String transferCancelReason;
    private Instant transferExpireAt;
    private Instant transferConfirmedAt;
    private Instant transferRejectedAt;
    private String transferRejectReason;
    private Instant createdAt;
    private Instant updatedAt;

    private GuardianRelationEntity() {}

    /** 从持久化数据重建（仅 Infrastructure 层 RepositoryImpl 调用）。 */
    public static GuardianRelationEntity reconstitute(
            Long id, Long userId, Long patientId, String relationRole, String relationStatus,
            String transferState, String transferRequestId, Long transferTargetUserId,
            Long transferRequestedBy, Instant transferRequestedAt, String transferReason,
            Long transferCancelledBy, Instant transferCancelledAt, String transferCancelReason,
            Instant transferExpireAt, Instant transferConfirmedAt,
            Instant transferRejectedAt, String transferRejectReason,
            Instant createdAt, Instant updatedAt) {
        GuardianRelationEntity e = new GuardianRelationEntity();
        e.id = id;
        e.userId = userId;
        e.patientId = patientId;
        e.relationRole = relationRole;
        e.relationStatus = relationStatus;
        e.transferState = transferState;
        e.transferRequestId = transferRequestId;
        e.transferTargetUserId = transferTargetUserId;
        e.transferRequestedBy = transferRequestedBy;
        e.transferRequestedAt = transferRequestedAt;
        e.transferReason = transferReason;
        e.transferCancelledBy = transferCancelledBy;
        e.transferCancelledAt = transferCancelledAt;
        e.transferCancelReason = transferCancelReason;
        e.transferExpireAt = transferExpireAt;
        e.transferConfirmedAt = transferConfirmedAt;
        e.transferRejectedAt = transferRejectedAt;
        e.transferRejectReason = transferRejectReason;
        e.createdAt = createdAt;
        e.updatedAt = updatedAt;
        return e;
    }

    /** 创建新关系记录（邀请接受时 / 患者创建时） */
    public static GuardianRelationEntity create(Long userId, Long patientId,
                                                String relationRole) {
        GuardianRelationEntity e = new GuardianRelationEntity();
        e.userId = userId;
        e.patientId = patientId;
        e.relationRole = relationRole;
        e.relationStatus = "ACTIVE";
        e.transferState = "NONE";
        return e;
    }

    /** 撤销关系 */
    public void revoke() {
        if (!"ACTIVE".equals(this.relationStatus)) throw BizException.of("E_MEMBER_4041");
        if ("PRIMARY_GUARDIAN".equals(this.relationRole)) throw BizException.of("E_TASK_4030");
        this.relationStatus = "REVOKED";
    }

    /**
     * 发起主监护人转移（仅 PRIMARY_GUARDIAN + ACTIVE + transferState=NONE 可操作）。
     */
    public void initiateTransfer(String transferRequestId, Long targetUserId,
                                  Long requestedBy, String reason) {
        if (!"PRIMARY_GUARDIAN".equals(this.relationRole)) throw BizException.of("E_TASK_4030");
        if (!"ACTIVE".equals(this.relationStatus)) throw BizException.of("E_MEMBER_4041");
        if (!"NONE".equals(this.transferState)) throw BizException.of("E_TRANS_4091");
        this.transferState = "PENDING_CONFIRM";
        this.transferRequestId = transferRequestId;
        this.transferTargetUserId = targetUserId;
        this.transferRequestedBy = requestedBy;
        this.transferRequestedAt = Instant.now();
        this.transferReason = reason;
        this.transferExpireAt = Instant.now().plus(24, ChronoUnit.HOURS);
    }

    /** 确认转移（被转移人接受） */
    public void confirmTransfer() {
        if (!"PENDING_CONFIRM".equals(this.transferState)) throw BizException.of("E_TRANS_4092");
        this.transferState = "ACCEPTED";
        this.transferConfirmedAt = Instant.now();
    }

    /** 拒绝转移 */
    public void rejectTransfer(String rejectReason) {
        if (!"PENDING_CONFIRM".equals(this.transferState)) throw BizException.of("E_TRANS_4092");
        this.transferState = "REJECTED";
        this.transferRejectedAt = Instant.now();
        this.transferRejectReason = rejectReason;
    }

    /** 取消转移（主监护人撤回） */
    public void cancelTransfer(Long cancelledBy, String cancelReason) {
        if (!"PENDING_CONFIRM".equals(this.transferState)) throw BizException.of("E_TRANS_4092");
        this.transferState = "CANCELLED";
        this.transferCancelledBy = cancelledBy;
        this.transferCancelledAt = Instant.now();
        this.transferCancelReason = cancelReason;
    }

    /** 转移完成后角色互换 */
    public void promoteToGuardian() {
        this.relationRole = "GUARDIAN";
        this.transferState = "NONE";
    }
}
