package com.xiaohelab.guard.server.domain.clue.entity;

import com.xiaohelab.guard.server.infrastructure.persistence.do_.ClueRecordDO;
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

    public static ClueRecordEntity fromDO(ClueRecordDO d) {
        ClueRecordEntity e = new ClueRecordEntity();
        e.id = d.getId();
        e.clueNo = d.getClueNo();
        e.patientId = d.getPatientId();
        e.taskId = d.getTaskId();
        e.tagCode = d.getTagCode();
        e.sourceType = d.getSourceType();
        e.riskScore = d.getRiskScore();
        e.locationLat = d.getLocationLat();
        e.locationLng = d.getLocationLng();
        e.coordSystem = d.getCoordSystem();
        e.description = d.getDescription();
        e.photoUrl = d.getPhotoUrl();
        e.isValid = d.getIsValid();
        e.suspectFlag = d.getSuspectFlag();
        e.suspectReason = d.getSuspectReason();
        e.reviewStatus = d.getReviewStatus();
        e.assigneeUserId = d.getAssigneeUserId();
        e.assignedAt = d.getAssignedAt();
        e.reviewedAt = d.getReviewedAt();
        e.override = d.getOverride();
        e.overrideBy = d.getOverrideBy();
        e.overrideReason = d.getOverrideReason();
        e.rejectReason = d.getRejectReason();
        e.rejectedBy = d.getRejectedBy();
        e.createdAt = d.getCreatedAt();
        e.updatedAt = d.getUpdatedAt();
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

    public ClueRecordDO toDO() {
        ClueRecordDO d = new ClueRecordDO();
        d.setId(this.id);
        d.setClueNo(this.clueNo);
        d.setPatientId(this.patientId);
        d.setTaskId(this.taskId);
        d.setTagCode(this.tagCode);
        d.setSourceType(this.sourceType);
        d.setRiskScore(this.riskScore);
        d.setLocationLat(this.locationLat);
        d.setLocationLng(this.locationLng);
        d.setCoordSystem(this.coordSystem);
        d.setDescription(this.description);
        d.setPhotoUrl(this.photoUrl);
        d.setIsValid(this.isValid);
        d.setSuspectFlag(this.suspectFlag);
        d.setSuspectReason(this.suspectReason);
        d.setReviewStatus(this.reviewStatus);
        d.setAssigneeUserId(this.assigneeUserId);
        d.setAssignedAt(this.assignedAt);
        d.setReviewedAt(this.reviewedAt);
        d.setOverride(this.override);
        d.setOverrideBy(this.overrideBy);
        d.setOverrideReason(this.overrideReason);
        d.setRejectReason(this.rejectReason);
        d.setRejectedBy(this.rejectedBy);
        return d;
    }
}
