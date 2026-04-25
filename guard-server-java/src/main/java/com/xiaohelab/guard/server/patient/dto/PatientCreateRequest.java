package com.xiaohelab.guard.server.patient.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

/**
 * 创建患者档案请求（API V2.0 §3.3.1）。
 * <p>wire 结构：</p>
 * <ul>
 *   <li>姓名：{@code patient_name}（必填，2-64）；为兼容旧客户端同时接收 {@code name}（{@link JsonAlias}）。</li>
 *   <li>外观：嵌套对象 {@code appearance{}}（可选）。</li>
 *   <li>围栏：嵌套对象 {@code fence{}}（可选；启用时 lat/lng/radius_m 必填）。</li>
 * </ul>
 */
public class PatientCreateRequest {

    @NotBlank
    @Size(min = 1, max = 64)
    @JsonProperty("patient_name")
    @JsonAlias({"name"})
    private String patientName;

    @NotBlank
    @Pattern(regexp = "MALE|FEMALE|UNKNOWN")
    private String gender;

    @NotNull
    private LocalDate birthday;

    @NotBlank
    @JsonProperty("avatar_url")
    @Size(max = 1024)
    private String avatarUrl;

    @JsonProperty("chronic_diseases")
    @Size(max = 500)
    private String chronicDiseases;

    @Size(max = 500)
    private String medication;

    @Size(max = 500)
    private String allergy;

    @JsonProperty("emergency_contact_phone")
    @Size(max = 32)
    private String emergencyContactPhone;

    @JsonProperty("long_text_profile")
    @Size(max = 5000)
    private String longTextProfile;

    /** 外观特征嵌套对象。 */
    @Valid
    private AppearanceBlock appearance;

    /** 围栏配置嵌套对象。 */
    @Valid
    private FenceBlock fence;

    /** 外观嵌套子对象。 */
    public static class AppearanceBlock {
        @JsonProperty("height_cm")
        @Min(50) @Max(250)
        private Integer heightCm;

        @JsonProperty("weight_kg")
        @Min(10) @Max(300)
        private Integer weightKg;

        @Size(max = 500)
        private String clothing;

        @Size(max = 500)
        private String features;

        public Integer getHeightCm() { return heightCm; }
        public void setHeightCm(Integer heightCm) { this.heightCm = heightCm; }
        public Integer getWeightKg() { return weightKg; }
        public void setWeightKg(Integer weightKg) { this.weightKg = weightKg; }
        public String getClothing() { return clothing; }
        public void setClothing(String clothing) { this.clothing = clothing; }
        public String getFeatures() { return features; }
        public void setFeatures(String features) { this.features = features; }
    }

    /** 围栏嵌套子对象。 */
    public static class FenceBlock {
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
        private String coordSystem;

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

    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public LocalDate getBirthday() { return birthday; }
    public void setBirthday(LocalDate birthday) { this.birthday = birthday; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public String getChronicDiseases() { return chronicDiseases; }
    public void setChronicDiseases(String chronicDiseases) { this.chronicDiseases = chronicDiseases; }
    public String getMedication() { return medication; }
    public void setMedication(String medication) { this.medication = medication; }
    public String getAllergy() { return allergy; }
    public void setAllergy(String allergy) { this.allergy = allergy; }
    public String getEmergencyContactPhone() { return emergencyContactPhone; }
    public void setEmergencyContactPhone(String emergencyContactPhone) { this.emergencyContactPhone = emergencyContactPhone; }
    public String getLongTextProfile() { return longTextProfile; }
    public void setLongTextProfile(String longTextProfile) { this.longTextProfile = longTextProfile; }
    public AppearanceBlock getAppearance() { return appearance; }
    public void setAppearance(AppearanceBlock appearance) { this.appearance = appearance; }
    public FenceBlock getFence() { return fence; }
    public void setFence(FenceBlock fence) { this.fence = fence; }
}
