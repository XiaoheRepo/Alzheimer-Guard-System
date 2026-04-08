package com.xiaohelab.guard.server.domain.tag.entity;

import lombok.Getter;

import java.time.Instant;

/**
 * 标签资产聚合根。
 * 状态机：UNBOUND → ALLOCATED → BOUND → LOST；高危操作（VOID/RESET/RECOVER）由管理员触发。
 * 状态变更通过 TagAssetRepository 的定向 UPDATE 操作执行，不使用 full-save。
 */
@Getter
public class TagAssetEntity {

    private Long id;
    /** 全局唯一标签码 */
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

    private TagAssetEntity() {}

    /** 从持久化数据重建（仅 Infrastructure 层 RepositoryImpl 调用）。 */
    public static TagAssetEntity reconstitute(
            Long id, String tagCode, String tagType, String status,
            Long patientId, Long applyRecordId, String importBatchNo, String voidReason,
            Instant lostAt, Instant voidAt, Instant resetAt, Instant recoveredAt,
            Instant createdAt, Instant updatedAt) {
        TagAssetEntity e = new TagAssetEntity();
        e.id = id;
        e.tagCode = tagCode;
        e.tagType = tagType;
        e.status = status;
        e.patientId = patientId;
        e.applyRecordId = applyRecordId;
        e.importBatchNo = importBatchNo;
        e.voidReason = voidReason;
        e.lostAt = lostAt;
        e.voidAt = voidAt;
        e.resetAt = resetAt;
        e.recoveredAt = recoveredAt;
        e.createdAt = createdAt;
        e.updatedAt = updatedAt;
        return e;
    }

    public static TagAssetEntity create(String tagCode, String tagType, String importBatchNo) {
        TagAssetEntity e = new TagAssetEntity();
        e.tagCode = tagCode;
        e.tagType = tagType;
        e.status = "UNBOUND";
        e.importBatchNo = importBatchNo;
        return e;
    }
}
