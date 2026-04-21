package com.xiaohelab.guard.server.patient.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public class FenceUpdateRequest {

    @NotNull
    @JsonProperty("fence_enabled")
    private Boolean fenceEnabled;

    @JsonProperty("fence_center_lat")
    @Min(-90) @Max(90)
    private Double fenceCenterLat;

    @JsonProperty("fence_center_lng")
    @Min(-180) @Max(180)
    private Double fenceCenterLng;

    @JsonProperty("fence_radius_m")
    @Min(100) @Max(50000)
    private Integer fenceRadiusM;

    @JsonProperty("fence_coord_system")
    @Pattern(regexp = "WGS84|GCJ-02|BD-09")
    private String fenceCoordSystem = "WGS84";

    public Boolean getFenceEnabled() { return fenceEnabled; }
    public void setFenceEnabled(Boolean fenceEnabled) { this.fenceEnabled = fenceEnabled; }
    public Double getFenceCenterLat() { return fenceCenterLat; }
    public void setFenceCenterLat(Double fenceCenterLat) { this.fenceCenterLat = fenceCenterLat; }
    public Double getFenceCenterLng() { return fenceCenterLng; }
    public void setFenceCenterLng(Double fenceCenterLng) { this.fenceCenterLng = fenceCenterLng; }
    public Integer getFenceRadiusM() { return fenceRadiusM; }
    public void setFenceRadiusM(Integer fenceRadiusM) { this.fenceRadiusM = fenceRadiusM; }
    public String getFenceCoordSystem() { return fenceCoordSystem; }
    public void setFenceCoordSystem(String fenceCoordSystem) { this.fenceCoordSystem = fenceCoordSystem; }
}
