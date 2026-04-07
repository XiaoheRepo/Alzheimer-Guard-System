package com.xiaohelab.guard.server.infrastructure.persistence.do_;

import lombok.Data;

import java.time.Instant;

/**
 * sys_user_patient 持久化对象（用户-患者关系，包含主监护转移字段）。
 * transfer_state 字段组遵循严格的 CHECK 约束，详见 DDL。
 */
@Data
public class SysUserPatientDO {

    private Long id;
    private Long userId;
    private Long patientId;
    /** PRIMARY_GUARDIAN / GUARDIAN */
    private String relationRole;
    /** PENDING / ACTIVE / REVOKED */
    private String relationStatus;
    /** NONE / PENDING_CONFIRM / ACCEPTED / REJECTED / CANCELLED / EXPIRED */
    private String transferState;
    /** 全局唯一，与转移操作绑定 */
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
}
