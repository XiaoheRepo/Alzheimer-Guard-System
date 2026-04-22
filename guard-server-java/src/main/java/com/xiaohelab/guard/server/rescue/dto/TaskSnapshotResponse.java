package com.xiaohelab.guard.server.rescue.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;

/**
 * 任务快照聚合响应（API V2.0 §3.1.3）。
 * <p>聚合：任务主信息 + 患者外观快照 + 线索统计 + 轨迹统计。</p>
 * <p>字段 JSON 命名遵循全局 SNAKE_CASE 策略，敏感字段已脱敏。</p>
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
public class TaskSnapshotResponse {

    private Long taskId;
    private String taskNo;
    private Long patientId;
    private String status;
    private String source;
    private Long reportedBy;
    private String remark;
    private OffsetDateTime createdAt;
    private OffsetDateTime closedAt;
    private String closeType;
    private String closeReason;
    private OffsetDateTime sustainedAt;
    private PatientSnapshot patientSnapshot;
    private ClueSummary clueSummary;
    private TrajectorySummary trajectorySummary;
    private Long version;

    public static class PatientSnapshot {
        private String patientName;
        private String gender;
        private Integer age;
        private String avatarUrl;
        private String shortCode;
        private Appearance appearance;

        public String getPatientName() { return patientName; }
        public void setPatientName(String v) { this.patientName = v; }
        public String getGender() { return gender; }
        public void setGender(String v) { this.gender = v; }
        public Integer getAge() { return age; }
        public void setAge(Integer v) { this.age = v; }
        public String getAvatarUrl() { return avatarUrl; }
        public void setAvatarUrl(String v) { this.avatarUrl = v; }
        public String getShortCode() { return shortCode; }
        public void setShortCode(String v) { this.shortCode = v; }
        public Appearance getAppearance() { return appearance; }
        public void setAppearance(Appearance v) { this.appearance = v; }
    }

    public static class Appearance {
        private Integer heightCm;
        private Integer weightKg;
        private String clothing;
        private String features;

        public Integer getHeightCm() { return heightCm; }
        public void setHeightCm(Integer v) { this.heightCm = v; }
        public Integer getWeightKg() { return weightKg; }
        public void setWeightKg(Integer v) { this.weightKg = v; }
        public String getClothing() { return clothing; }
        public void setClothing(String v) { this.clothing = v; }
        public String getFeatures() { return features; }
        public void setFeatures(String v) { this.features = v; }
    }

    public static class ClueSummary {
        private long totalClueCount;
        private long validClueCount;
        private long suspectClueCount;
        private OffsetDateTime latestClueTime;

        public long getTotalClueCount() { return totalClueCount; }
        public void setTotalClueCount(long v) { this.totalClueCount = v; }
        public long getValidClueCount() { return validClueCount; }
        public void setValidClueCount(long v) { this.validClueCount = v; }
        public long getSuspectClueCount() { return suspectClueCount; }
        public void setSuspectClueCount(long v) { this.suspectClueCount = v; }
        public OffsetDateTime getLatestClueTime() { return latestClueTime; }
        public void setLatestClueTime(OffsetDateTime v) { this.latestClueTime = v; }
    }

    public static class TrajectorySummary {
        private long pointCount;
        private OffsetDateTime latestPointTime;
        @JsonProperty("bounding_box")
        private BoundingBox boundingBox;

        public long getPointCount() { return pointCount; }
        public void setPointCount(long v) { this.pointCount = v; }
        public OffsetDateTime getLatestPointTime() { return latestPointTime; }
        public void setLatestPointTime(OffsetDateTime v) { this.latestPointTime = v; }
        public BoundingBox getBoundingBox() { return boundingBox; }
        public void setBoundingBox(BoundingBox v) { this.boundingBox = v; }
    }

    public static class BoundingBox {
        private Double minLat;
        private Double maxLat;
        private Double minLng;
        private Double maxLng;

        public Double getMinLat() { return minLat; }
        public void setMinLat(Double v) { this.minLat = v; }
        public Double getMaxLat() { return maxLat; }
        public void setMaxLat(Double v) { this.maxLat = v; }
        public Double getMinLng() { return minLng; }
        public void setMinLng(Double v) { this.minLng = v; }
        public Double getMaxLng() { return maxLng; }
        public void setMaxLng(Double v) { this.maxLng = v; }
    }

    // --- getters/setters ---
    public Long getTaskId() { return taskId; }
    public void setTaskId(Long v) { this.taskId = v; }
    public String getTaskNo() { return taskNo; }
    public void setTaskNo(String v) { this.taskNo = v; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long v) { this.patientId = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    public String getSource() { return source; }
    public void setSource(String v) { this.source = v; }
    public Long getReportedBy() { return reportedBy; }
    public void setReportedBy(Long v) { this.reportedBy = v; }
    public String getRemark() { return remark; }
    public void setRemark(String v) { this.remark = v; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime v) { this.createdAt = v; }
    public OffsetDateTime getClosedAt() { return closedAt; }
    public void setClosedAt(OffsetDateTime v) { this.closedAt = v; }
    public String getCloseType() { return closeType; }
    public void setCloseType(String v) { this.closeType = v; }
    public String getCloseReason() { return closeReason; }
    public void setCloseReason(String v) { this.closeReason = v; }
    public OffsetDateTime getSustainedAt() { return sustainedAt; }
    public void setSustainedAt(OffsetDateTime v) { this.sustainedAt = v; }
    public PatientSnapshot getPatientSnapshot() { return patientSnapshot; }
    public void setPatientSnapshot(PatientSnapshot v) { this.patientSnapshot = v; }
    public ClueSummary getClueSummary() { return clueSummary; }
    public void setClueSummary(ClueSummary v) { this.clueSummary = v; }
    public TrajectorySummary getTrajectorySummary() { return trajectorySummary; }
    public void setTrajectorySummary(TrajectorySummary v) { this.trajectorySummary = v; }
    public Long getVersion() { return version; }
    public void setVersion(Long v) { this.version = v; }
}
