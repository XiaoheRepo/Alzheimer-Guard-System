package com.xiaohelab.guard.server.domain.guardian.entity;

import com.xiaohelab.guard.server.common.exception.BizException;
import com.xiaohelab.guard.server.infrastructure.persistence.do_.SysUserPatientDO;
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

    public static GuardianRelationEntity fromDO(SysUserPatientDO d) {
        GuardianRelationEntity e = new GuardianRelationEntity();
        e.id = d.getId();
        e.userId = d.getUserId();
        e.patientId = d.getPatientId();
        e.relationRole = d.getRelationRole();
        e.relationStatus = d.getRelationStatus();
        e.transferState = d.getTransferState();
        e.transferRequestId = d.getTransferRequestId();
        e.transferTargetUserId = d.getTransferTargetUserId();
        e.transferRequestedBy = d.getTransferRequestedBy();
        e.transferRequestedAt = d.getTransferRequestedAt();
        e.transferReason = d.getTransferReason();
        e.transferCancelledBy = d.getTransferCancelledBy();
        e.transferCancelledAt = d.getTransferCancelledAt();
        e.transferCancelReason = d.getTransferCancelReason();
        e.transferExpireAt = d.getTransferExpireAt();
        e.transferConfirmedAt = d.getTransferConfirmedAt();
        e.transferRejectedAt = d.getTransferRejectedAt();
        e.transferRejectReason = d.getTransferRejectReason();
        e.createdAt = d.getCreatedAt();
        e.updatedAt = d.getUpdatedAt();
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

    public SysUserPatientDO toDO() {
        SysUserPatientDO d = new SysUserPatientDO();
        d.setId(this.id);
        d.setUserId(this.userId);
        d.setPatientId(this.patientId);
        d.setRelationRole(this.relationRole);
        d.setRelationStatus(this.relationStatus);
        d.setTransferState(this.transferState);
        d.setTransferRequestId(this.transferRequestId);
        d.setTransferTargetUserId(this.transferTargetUserId);
        d.setTransferRequestedBy(this.transferRequestedBy);
        d.setTransferRequestedAt(this.transferRequestedAt);
        d.setTransferReason(this.transferReason);
        d.setTransferCancelledBy(this.transferCancelledBy);
        d.setTransferCancelledAt(this.transferCancelledAt);
        d.setTransferCancelReason(this.transferCancelReason);
        d.setTransferExpireAt(this.transferExpireAt);
        d.setTransferConfirmedAt(this.transferConfirmedAt);
        d.setTransferRejectedAt(this.transferRejectedAt);
        d.setTransferRejectReason(this.transferRejectReason);
        return d;
    }
}
