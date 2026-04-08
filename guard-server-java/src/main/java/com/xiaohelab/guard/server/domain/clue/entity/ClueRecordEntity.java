package com.xiaohelab.guard.server.domain.clue.entity;

import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * 线索记录聚合根。
 * 坐标系固定 WGS84，risk_score [0,1] 由 AI 填入。
 * 审核状态：PENDING / OVERRIDDEN / REJECTED（非可疑线索 reviewStatus 为 null）。
 */
@Getter
public class ClueRecordEntity {

    private Long id;
    private String clueNo;
    private Long patientId;
    private Long taskId;
    private String tagCode;
    /** SCAN / MANUAL */
    private String sourceType;
    private BigDecimal riskScore;
    private Double locationLat;
    private Double locationLng;
    /** 固定 WGS84 */
    private String coordSystem;
    private String description;
    private String photoUrl;
    private Boolean isValid;
    private Boolean suspectFlag;
    private String suspectReason;
    /** PENDING / OVERRIDDEN / REJECTED */
    private String reviewStatus;
    private Long assigneeUserId;
    private Instant assignedAt;
    private Instant reviewedAt;
    private Boolean override;
    private Long overrideBy;
    private String overrideReason;
    private String rejectReason;
    private Long rejectedBy;
    private Instant createdAt;
    private Instant updatedAt;

    private ClueRecordEntity() {}

    /** 从持久化数据重建（仅 Infrastructure 层 RepositoryImpl 调用）。 */
    public static ClueRecordEntity reconstitute(
            Long id, String clueNo, Long patientId, Long taskId, String tagCode,
            String sourceType, BigDecimal riskScore, Double locationLat, Double locationLng,
            String coordSystem, String description, String photoUrl,
            Boolean isValid, Boolean suspectFlag, String suspectReason, String reviewStatus,
            Long assigneeUserId, Instant assignedAt, Instant reviewedAt,
            Boolean override, Long overrideBy, String overrideReason,
            String rejectReason, Long rejectedBy, Instant createdAt, Instant updatedAt) {
        ClueRecordEntity e = new ClueRecordEntity();
        e.id = id;
        e.clueNo = clueNo;
        e.patientId = patientId;
        e.taskId = taskId;
        e.tagCode = tagCode;
        e.sourceType = sourceType;
        e.riskScore = riskScore;
        e.locationLat = locationLat;
        e.locationLng = locationLng;
        e.coordSystem = coordSystem;
        e.description = description;
        e.photoUrl = photoUrl;
        e.isValid = isValid;
        e.suspectFlag = suspectFlag;
        e.suspectReason = suspectReason;
        e.reviewStatus = reviewStatus;
        e.assigneeUserId = assigneeUserId;
        e.assignedAt = assignedAt;
        e.reviewedAt = reviewedAt;
        e.override = override;
        e.overrideBy = overrideBy;
        e.overrideReason = overrideReason;
        e.rejectReason = rejectReason;
        e.rejectedBy = rejectedBy;
        e.createdAt = createdAt;
        e.updatedAt = updatedAt;
        return e;
    }

    /** 创建新线索（初始状态 PENDING，risk_score 为 0 等待 AI 填入）。 */
    public static ClueRecordEntity create(String clueNo, Long taskId, Long patientId,
                                          String sourceType, Double lat, Double lng,
                                          String description, String photoUrl) {
        ClueRecordEntity e = new ClueRecordEntity();
        e.clueNo = clueNo;
        e.taskId = taskId;
        e.patientId = patientId;
        e.sourceType = sourceType;
        e.locationLat = lat;
        e.locationLng = lng;
        e.coordSystem = "WGS84";
        e.description = description;
        e.photoUrl = photoUrl;
        e.riskScore = BigDecimal.ZERO;
        e.reviewStatus = "PENDING";
        return e;
    }
}
