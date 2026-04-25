package com.xiaohelab.guard.server.patient.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * 设置电子围栏请求（API V2.0 §3.3.4，PUT /patients/{id}/fence）。
 * <p>wire 结构：嵌套 {@code fence{ enabled, center_lat, center_lng, radius_m, coord_system }}。</p>
 * <p>启用时 lat/lng/radius_m 三者必须同时给出（Service 层校验）。</p>
 */
public class FenceUpdateRequest {

    @NotNull
    @Valid
    private FenceBlock fence;

    public static class FenceBlock {
        @NotNull
        private Boolean enabled;

        @JsonProperty("center_lat")
        @DecimalMin("-90") @DecimalMax("90")
        private Double centerLat;

        @JsonProperty("center_lng")
        @DecimalMin("-180") @DecimalMax("180")
        private Double centerLng;

        @JsonProperty("radius_m")
        @Min(100) @Max(50000)
        private Integer radiusM;

        @JsonProperty("coord_system")
        @Pattern(regexp = "WGS84|GCJ-02|BD-09")
        private String coordSystem = "WGS84";

        public Boolean getEnabled() { return enabled; }
        public void setEnabled(Boolean enabled) { this.enabled = enabled; }
        public Double getCenterLat() { return centerLat; }
        public void setCenterLat(Double centerLat) { this.centerLat = centerLat; }
        public Double getCenterLng() { return centerLng; }
        public void setCenterLng(Double centerLng) { this.centerLng = centerLng; }
        public Integer getRadiusM() { return radiusM; }
        public void setRadiusM(Integer radiusM) { this.radiusM = radiusM; }
        public String getCoordSystem() { return coordSystem; }
        public void setCoordSystem(String coordSystem) { this.coordSystem = coordSystem; }
    }

    public FenceBlock getFence() { return fence; }
    public void setFence(FenceBlock fence) { this.fence = fence; }
}
