package com.xiaohelab.guard.server.infrastructure.persistence.do_;

import lombok.Data;

import java.time.Instant;

/**
 * rescue_task 持久化对象。
 * 状态变更必须通过条件更新（WHERE status='ACTIVE'），禁止无条件 UPDATE。
 */
@Data
public class RescueTaskDO {

    private Long id;
    /** 业务编号，格式 TSK + 时间戳 */
    private String taskNo;
    private Long patientId;
    /** ACTIVE / RESOLVED / FALSE_ALARM */
    private String status;
    /** APP / MINI_PROGRAM / ADMIN_PORTAL */
    private String source;
    /** 发起备注，<=500 字 */
    private String remark;
    private String aiAnalysisSummary;
    private String posterUrl;
    /** 关闭原因，终态写入 */
    private String closeReason;
    /** 乐观锁版本，每次状态变更 +1 */
    private Long eventVersion;
    private Long createdBy;
    private Instant createdAt;
    /** 终态关闭时间，ACTIVE 时必须为 null */
    private Instant closedAt;
    private Instant updatedAt;
}
