package com.xiaohelab.guard.server.rescue.entity;

import com.xiaohelab.guard.server.common.entity.BaseEntity;
import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "rescue_task")
public class RescueTaskEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_no", length = 32, unique = true)
    private String taskNo;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    /** CREATED / ACTIVE / SUSTAINED / CLOSED_FOUND / CLOSED_FALSE_ALARM */
    @Column(name = "status", length = 32, nullable = false)
    private String status;

    /** APP / MINI_PROGRAM / ADMIN_PORTAL / AUTO_UPGRADE */
    @Column(name = "source", length = 32, nullable = false)
    private String source;

    @Column(name = "remark", length = 500)
    private String remark;

    @Column(name = "daily_appearance", columnDefinition = "TEXT")
    private String dailyAppearance;

    @Column(name = "daily_photo_url", length = 1024)
    private String dailyPhotoUrl;

    @Column(name = "ai_analysis_summary", columnDefinition = "TEXT")
    private String aiAnalysisSummary;

    @Column(name = "poster_url", length = 1024)
    private String posterUrl;

    /** FOUND / FALSE_ALARM */
    @Column(name = "close_type", length = 20)
    private String closeType;

    @Column(name = "close_reason", length = 256)
    private String closeReason;

    @Column(name = "found_location_lat")
    private Double foundLocationLat;

    @Column(name = "found_location_lng")
    private Double foundLocationLng;

    @Column(name = "event_version", nullable = false)
    private Long eventVersion = 0L;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "closed_by")
    private Long closedBy;

    @Column(name = "closed_at")
    private OffsetDateTime closedAt;

    @Column(name = "sustained_at")
    private OffsetDateTime sustainedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTaskNo() { return taskNo; }
    public void setTaskNo(String taskNo) { this.taskNo = taskNo; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public String getDailyAppearance() { return dailyAppearance; }
    public void setDailyAppearance(String dailyAppearance) { this.dailyAppearance = dailyAppearance; }
    public String getDailyPhotoUrl() { return dailyPhotoUrl; }
    public void setDailyPhotoUrl(String dailyPhotoUrl) { this.dailyPhotoUrl = dailyPhotoUrl; }
    public String getAiAnalysisSummary() { return aiAnalysisSummary; }
    public void setAiAnalysisSummary(String aiAnalysisSummary) { this.aiAnalysisSummary = aiAnalysisSummary; }
    public String getPosterUrl() { return posterUrl; }
    public void setPosterUrl(String posterUrl) { this.posterUrl = posterUrl; }
    public String getCloseType() { return closeType; }
    public void setCloseType(String closeType) { this.closeType = closeType; }
    public String getCloseReason() { return closeReason; }
    public void setCloseReason(String closeReason) { this.closeReason = closeReason; }
    public Double getFoundLocationLat() { return foundLocationLat; }
    public void setFoundLocationLat(Double foundLocationLat) { this.foundLocationLat = foundLocationLat; }
    public Double getFoundLocationLng() { return foundLocationLng; }
    public void setFoundLocationLng(Double foundLocationLng) { this.foundLocationLng = foundLocationLng; }
    public Long getEventVersion() { return eventVersion; }
    public void setEventVersion(Long eventVersion) { this.eventVersion = eventVersion; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
    public Long getClosedBy() { return closedBy; }
    public void setClosedBy(Long closedBy) { this.closedBy = closedBy; }
    public OffsetDateTime getClosedAt() { return closedAt; }
    public void setClosedAt(OffsetDateTime closedAt) { this.closedAt = closedAt; }
    public OffsetDateTime getSustainedAt() { return sustainedAt; }
    public void setSustainedAt(OffsetDateTime sustainedAt) { this.sustainedAt = sustainedAt; }
}
