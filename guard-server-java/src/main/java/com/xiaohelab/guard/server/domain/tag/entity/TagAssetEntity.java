package com.xiaohelab.guard.server.domain.tag.entity;

import com.xiaohelab.guard.server.infrastructure.persistence.do_.TagAssetDO;
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

    public static TagAssetEntity fromDO(TagAssetDO d) {
        TagAssetEntity e = new TagAssetEntity();
        e.id = d.getId();
        e.tagCode = d.getTagCode();
        e.tagType = d.getTagType();
        e.status = d.getStatus();
        e.patientId = d.getPatientId();
        e.applyRecordId = d.getApplyRecordId();
        e.importBatchNo = d.getImportBatchNo();
        e.voidReason = d.getVoidReason();
        e.lostAt = d.getLostAt();
        e.voidAt = d.getVoidAt();
        e.resetAt = d.getResetAt();
        e.recoveredAt = d.getRecoveredAt();
        e.createdAt = d.getCreatedAt();
        e.updatedAt = d.getUpdatedAt();
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

    public TagAssetDO toDO() {
        TagAssetDO d = new TagAssetDO();
        d.setId(this.id);
        d.setTagCode(this.tagCode);
        d.setTagType(this.tagType);
        d.setStatus(this.status);
        d.setPatientId(this.patientId);
        d.setApplyRecordId(this.applyRecordId);
        d.setImportBatchNo(this.importBatchNo);
        d.setVoidReason(this.voidReason);
        d.setLostAt(this.lostAt);
        d.setVoidAt(this.voidAt);
        d.setResetAt(this.resetAt);
        d.setRecoveredAt(this.recoveredAt);
        return d;
    }
}
