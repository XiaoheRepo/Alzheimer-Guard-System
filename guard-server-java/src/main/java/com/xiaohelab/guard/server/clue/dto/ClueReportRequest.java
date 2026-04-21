package com.xiaohelab.guard.server.clue.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;

import java.util.List;

public class ClueReportRequest {

    @JsonProperty("task_id")
    private Long taskId;

    @JsonProperty("patient_id")
    private Long patientId;

    @NotBlank
    @Pattern(regexp = "SCAN|MANUAL|POSTER_SCAN")
    @JsonProperty("source_type")
    private String sourceType;

    @NotNull @Min(-90) @Max(90)
    private Double latitude;

    @NotNull @Min(-180) @Max(180)
    private Double longitude;

    @Pattern(regexp = "WGS84|GCJ-02|BD-09")
    @JsonProperty("coord_system")
    private String coordSystem = "WGS84";

    @Size(max = 2000)
    private String description;

    @JsonProperty("photo_urls")
    private List<@Size(max = 1024) String> photoUrls;

    @JsonProperty("tag_code")
    @Size(max = 100)
    private String tagCode;

    @JsonProperty("tag_only")
    private Boolean tagOnly = false;

    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public String getCoordSystem() { return coordSystem; }
    public void setCoordSystem(String coordSystem) { this.coordSystem = coordSystem; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public List<String> getPhotoUrls() { return photoUrls; }
    public void setPhotoUrls(List<String> photoUrls) { this.photoUrls = photoUrls; }
    public String getTagCode() { return tagCode; }
    public void setTagCode(String tagCode) { this.tagCode = tagCode; }
    public Boolean getTagOnly() { return tagOnly; }
    public void setTagOnly(Boolean tagOnly) { this.tagOnly = tagOnly; }
}
