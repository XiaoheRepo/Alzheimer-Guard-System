package com.xiaohelab.guard.server.clue.entity;

import com.xiaohelab.guard.server.common.entity.BaseEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "clue_record")
public class ClueRecordEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "clue_no", length = 32, unique = true)
    private String clueNo;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "task_id")
    private Long taskId;

    @Column(name = "tag_code", length = 100)
    private String tagCode;

    /** SCAN / MANUAL / POSTER_SCAN */
    @Column(name = "source_type", length = 20, nullable = false)
    private String sourceType;

    @Column(name = "reporter_user_id")
    private Long reporterUserId;

    /** FAMILY / ANONYMOUS / ADMIN */
    @Column(name = "reporter_type", length = 20, nullable = false)
    private String reporterType;

    @Column(name = "latitude", nullable = false)
    private Double latitude;

    @Column(name = "longitude", nullable = false)
    private Double longitude;

    @Column(name = "coord_system", length = 10, nullable = false)
    private String coordSystem = "WGS84";

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "photo_urls", nullable = false, columnDefinition = "jsonb")
    private String photoUrls = "[]";

    @Column(name = "tag_only", nullable = false)
    private Boolean tagOnly = false;

    @Column(name = "risk_score", precision = 5, scale = 4)
    private BigDecimal riskScore;

    /** VALID / OVERRIDDEN / REJECTED / INVALID */
    @Column(name = "status", length = 20, nullable = false)
    private String status = "VALID";

    @Column(name = "suspect_flag", nullable = false)
    private Boolean suspectFlag = false;

    @Column(name = "suspect_reason", length = 256)
    private String suspectReason;

    @Column(name = "drift_flag", nullable = false)
    private Boolean driftFlag = false;

    /** NULL / PENDING / OVERRIDDEN / REJECTED */
    @Column(name = "review_status", length = 20)
    private String reviewStatus;

    @Column(name = "override_reason", length = 256)
    private String overrideReason;

    @Column(name = "reject_reason", length = 256)
    private String rejectReason;

    @Column(name = "reviewed_by")
    private Long reviewedBy;

    @Column(name = "reviewed_at")
    private OffsetDateTime reviewedAt;

    @Column(name = "device_fingerprint", length = 128)
    private String deviceFingerprint;

    @Column(name = "entry_token_jti", length = 64)
    private String entryTokenJti;

    @Column(name = "client_ip", length = 64)
    private String clientIp;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getClueNo() { return clueNo; }
    public void setClueNo(String clueNo) { this.clueNo = clueNo; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }
    public String getTagCode() { return tagCode; }
    public void setTagCode(String tagCode) { this.tagCode = tagCode; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public Long getReporterUserId() { return reporterUserId; }
    public void setReporterUserId(Long reporterUserId) { this.reporterUserId = reporterUserId; }
    public String getReporterType() { return reporterType; }
    public void setReporterType(String reporterType) { this.reporterType = reporterType; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public String getCoordSystem() { return coordSystem; }
    public void setCoordSystem(String coordSystem) { this.coordSystem = coordSystem; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getPhotoUrls() { return photoUrls; }
    public void setPhotoUrls(String photoUrls) { this.photoUrls = photoUrls; }
    public Boolean getTagOnly() { return tagOnly; }
    public void setTagOnly(Boolean tagOnly) { this.tagOnly = tagOnly; }
    public BigDecimal getRiskScore() { return riskScore; }
    public void setRiskScore(BigDecimal riskScore) { this.riskScore = riskScore; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Boolean getSuspectFlag() { return suspectFlag; }
    public void setSuspectFlag(Boolean suspectFlag) { this.suspectFlag = suspectFlag; }
    public String getSuspectReason() { return suspectReason; }
    public void setSuspectReason(String suspectReason) { this.suspectReason = suspectReason; }
    public Boolean getDriftFlag() { return driftFlag; }
    public void setDriftFlag(Boolean driftFlag) { this.driftFlag = driftFlag; }
    public String getReviewStatus() { return reviewStatus; }
    public void setReviewStatus(String reviewStatus) { this.reviewStatus = reviewStatus; }
    public String getOverrideReason() { return overrideReason; }
    public void setOverrideReason(String overrideReason) { this.overrideReason = overrideReason; }
    public String getRejectReason() { return rejectReason; }
    public void setRejectReason(String rejectReason) { this.rejectReason = rejectReason; }
    public Long getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(Long reviewedBy) { this.reviewedBy = reviewedBy; }
    public OffsetDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(OffsetDateTime reviewedAt) { this.reviewedAt = reviewedAt; }
    public String getDeviceFingerprint() { return deviceFingerprint; }
    public void setDeviceFingerprint(String deviceFingerprint) { this.deviceFingerprint = deviceFingerprint; }
    public String getEntryTokenJti() { return entryTokenJti; }
    public void setEntryTokenJti(String entryTokenJti) { this.entryTokenJti = entryTokenJti; }
    public String getClientIp() { return clientIp; }
    public void setClientIp(String clientIp) { this.clientIp = clientIp; }
}
