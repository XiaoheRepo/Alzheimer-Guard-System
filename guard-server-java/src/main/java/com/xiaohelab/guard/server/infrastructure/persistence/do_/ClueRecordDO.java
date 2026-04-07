package com.xiaohelab.guard.server.infrastructure.persistence.do_;

import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * clue_record 持久化对象。
 * 坐标入库前必须由网关完成 GCJ-02/BD-09 → WGS84 转换，coord_system 恒为 WGS84。
 */
@Data
public class ClueRecordDO {

    private Long id;
    /** 业务编号, 格式 CLU + 时间戳 */
    private String clueNo;
    private Long patientId;
    private Long taskId;
    private String tagCode;
    /** SCAN / MANUAL */
    private String sourceType;
    /** 风险分 0-1，AI 研判结果 */
    private BigDecimal riskScore;
    private Double locationLat;
    private Double locationLng;
    /** 固定 WGS84，防御性约束字段 */
    private String coordSystem;
    private String description;
    private String photoUrl;
    private Boolean isValid;
    /** true 时触发复核流程 */
    private Boolean suspectFlag;
    private String suspectReason;
    /** PENDING / OVERRIDDEN / REJECTED；非可疑线索必须为 null */
    private String reviewStatus;
    private Long assigneeUserId;
    private Instant assignedAt;
    private Instant reviewedAt;
    /** 管理员强制回流标记 */
    private Boolean override;
    private Long overrideBy;
    private String overrideReason;
    private String rejectReason;
    private Long rejectedBy;
    private Instant createdAt;
    private Instant updatedAt;
}
