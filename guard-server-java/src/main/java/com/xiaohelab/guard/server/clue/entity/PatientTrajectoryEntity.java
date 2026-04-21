package com.xiaohelab.guard.server.clue.entity;

import com.xiaohelab.guard.server.common.entity.AuditOnlyEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

@Entity
@Table(name = "patient_trajectory")
public class PatientTrajectoryEntity extends AuditOnlyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "task_id")
    private Long taskId;

    @Column(name = "clue_id")
    private Long clueId;

    @Column(name = "window_start", nullable = false)
    private OffsetDateTime windowStart;

    @Column(name = "window_end", nullable = false)
    private OffsetDateTime windowEnd;

    @Column(name = "point_count", nullable = false)
    private Integer pointCount;

    /** LINESTRING / SPARSE_POINT / EMPTY_WINDOW / POINT */
    @Column(name = "geometry_type", length = 32, nullable = false)
    private String geometryType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "geometry_data", columnDefinition = "jsonb")
    private String geometryData;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "source_type", length = 32)
    private String sourceType;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }
    public Long getClueId() { return clueId; }
    public void setClueId(Long clueId) { this.clueId = clueId; }
    public OffsetDateTime getWindowStart() { return windowStart; }
    public void setWindowStart(OffsetDateTime windowStart) { this.windowStart = windowStart; }
    public OffsetDateTime getWindowEnd() { return windowEnd; }
    public void setWindowEnd(OffsetDateTime windowEnd) { this.windowEnd = windowEnd; }
    public Integer getPointCount() { return pointCount; }
    public void setPointCount(Integer pointCount) { this.pointCount = pointCount; }
    public String getGeometryType() { return geometryType; }
    public void setGeometryType(String geometryType) { this.geometryType = geometryType; }
    public String getGeometryData() { return geometryData; }
    public void setGeometryData(String geometryData) { this.geometryData = geometryData; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
}
