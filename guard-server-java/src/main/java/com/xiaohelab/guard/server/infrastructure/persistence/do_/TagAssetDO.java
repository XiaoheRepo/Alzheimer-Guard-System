package com.xiaohelab.guard.server.infrastructure.persistence.do_;

import lombok.Data;

import java.time.Instant;

/**
 * tag_asset 持久化对象（标签资产）。
 * 状态机：UNBOUND → ALLOCATED → BOUND → LOST；高危操作需 ADMIN/SUPERADMIN 权限。
 */
@Data
public class TagAssetDO {

    private Long id;
    /** 全局唯一标签码，对应二维码/NFC内容 */
    private String tagCode;
    /** QR_CODE / NFC */
    private String tagType;
    /** UNBOUND / ALLOCATED / BOUND / LOST / VOID */
    private String status;
    private Long patientId;
    private Long applyRecordId;
    private String importBatchNo;
    private String voidReason;
    private Instant lostAt;
    private Instant voidAt;
    private Instant resetAt;
    private Instant recoveredAt;
    private Instant createdAt;
    private Instant updatedAt;
}
