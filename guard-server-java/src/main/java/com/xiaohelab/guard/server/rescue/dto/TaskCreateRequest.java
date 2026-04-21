package com.xiaohelab.guard.server.rescue.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;

public class TaskCreateRequest {

    @NotNull
    @JsonProperty("patient_id")
    private Long patientId;

    @NotBlank
    @Pattern(regexp = "APP|MINI_PROGRAM|ADMIN_PORTAL|AUTO_UPGRADE")
    private String source;

    @Size(max = 500)
    private String remark;

    @JsonProperty("daily_appearance")
    @Size(max = 2000)
    private String dailyAppearance;

    @JsonProperty("daily_photo_url")
    @Size(max = 1024)
    private String dailyPhotoUrl;

    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public String getDailyAppearance() { return dailyAppearance; }
    public void setDailyAppearance(String dailyAppearance) { this.dailyAppearance = dailyAppearance; }
    public String getDailyPhotoUrl() { return dailyPhotoUrl; }
    public void setDailyPhotoUrl(String dailyPhotoUrl) { this.dailyPhotoUrl = dailyPhotoUrl; }
}
