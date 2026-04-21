package com.xiaohelab.guard.server.rescue.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;

public class TaskCloseRequest {

    @NotBlank
    @Pattern(regexp = "FOUND|FALSE_ALARM")
    @JsonProperty("close_type")
    private String closeType;

    @Size(max = 256)
    @JsonProperty("close_reason")
    private String closeReason;

    @Min(-90) @Max(90)
    @JsonProperty("found_location_lat")
    private Double foundLocationLat;

    @Min(-180) @Max(180)
    @JsonProperty("found_location_lng")
    private Double foundLocationLng;

    public String getCloseType() { return closeType; }
    public void setCloseType(String closeType) { this.closeType = closeType; }
    public String getCloseReason() { return closeReason; }
    public void setCloseReason(String closeReason) { this.closeReason = closeReason; }
    public Double getFoundLocationLat() { return foundLocationLat; }
    public void setFoundLocationLat(Double foundLocationLat) { this.foundLocationLat = foundLocationLat; }
    public Double getFoundLocationLng() { return foundLocationLng; }
    public void setFoundLocationLng(Double foundLocationLng) { this.foundLocationLng = foundLocationLng; }
}
