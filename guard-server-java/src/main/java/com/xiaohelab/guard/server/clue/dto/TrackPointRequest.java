package com.xiaohelab.guard.server.clue.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;

public class TrackPointRequest {

    @NotNull
    @JsonProperty("patient_id")
    private Long patientId;

    @JsonProperty("task_id")
    private Long taskId;

    @NotNull @Min(-90) @Max(90)
    private Double latitude;

    @NotNull @Min(-180) @Max(180)
    private Double longitude;

    @Pattern(regexp = "WGS84|GCJ-02|BD-09")
    @JsonProperty("coord_system")
    private String coordSystem = "WGS84";

    /** GPS / WIFI / CELL / MANUAL / CLUE */
    @Pattern(regexp = "GPS|WIFI|CELL|MANUAL|CLUE")
    @JsonProperty("source_type")
    private String sourceType = "GPS";

    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public String getCoordSystem() { return coordSystem; }
    public void setCoordSystem(String coordSystem) { this.coordSystem = coordSystem; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
}
