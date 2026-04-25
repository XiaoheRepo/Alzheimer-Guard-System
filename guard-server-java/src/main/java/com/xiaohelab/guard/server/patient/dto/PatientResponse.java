package com.xiaohelab.guard.server.patient.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * 患者档案响应（API V2.0 §3.3.1 / §3.3.12）。
 * <p>对齐基线：</p>
 * <ul>
 *   <li>姓名字段 wire 为 {@code patient_name}（API V2.0 §3.3 字段字典）；</li>
 *   <li>外观信息以 {@code appearance{}} 嵌套对象输出；</li>
 *   <li>围栏信息以 {@code fence{}} 嵌套对象输出（含 {@code coord_system}）；</li>
 *   <li>所有 ID 在 JSON 输出为 string（由 {@link com.xiaohelab.guard.server.common.config.JacksonConfig} 统一处理）。</li>
 * </ul>
 * <p>数据库底层仍以扁平字段持久化（DBD 不变），由 Service 层完成扁平 ↔ 嵌套映射。</p>
 */
public class PatientResponse {

    @JsonProperty("patient_id")
    private Long patientId;
    /** 姓名：基线 wire 字段为 patient_name（API V2.0 §3.3.1）。 */
    @JsonProperty("patient_name")
    private String patientName;
    private String gender;
    private LocalDate birthday;
    @JsonProperty("short_code")
    private String shortCode;
    @JsonProperty("avatar_url")
    private String avatarUrl;
    @JsonProperty("chronic_diseases")
    private String chronicDiseases;
    private String medication;
    private String allergy;
    @JsonProperty("emergency_contact_phone_masked")
    private String emergencyContactPhoneMasked;
    @JsonProperty("long_text_profile")
    private String longTextProfile;

    /** 外观特征嵌套对象（API V2.0 §3.3.1 appearance{}）。 */
    private Appearance appearance;

    /** 电子围栏嵌套对象（API V2.0 §3.3.1 fence{}）。 */
    private Fence fence;

    @JsonProperty("lost_status")
    private String lostStatus;
    @JsonProperty("profile_version")
    private Long profileVersion;
    @JsonProperty("created_at")
    private OffsetDateTime createdAt;
    @JsonProperty("updated_at")
    private OffsetDateTime updatedAt;

    /** 外观嵌套子对象（snake_case wire 字段）。 */
    public static class Appearance {
        @JsonProperty("height_cm")
        private Integer heightCm;
        @JsonProperty("weight_kg")
        private Integer weightKg;
        private String clothing;
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

    /** 围栏嵌套子对象（snake_case wire 字段）。 */
    public static class Fence {
        private Boolean enabled;
        @JsonProperty("center_lat")
        private Double centerLat;
        @JsonProperty("center_lng")
        private Double centerLng;
        @JsonProperty("radius_m")
        private Integer radiusM;
        @JsonProperty("coord_system")
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

    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public LocalDate getBirthday() { return birthday; }
    public void setBirthday(LocalDate birthday) { this.birthday = birthday; }
    public String getShortCode() { return shortCode; }
    public void setShortCode(String shortCode) { this.shortCode = shortCode; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public String getChronicDiseases() { return chronicDiseases; }
    public void setChronicDiseases(String chronicDiseases) { this.chronicDiseases = chronicDiseases; }
    public String getMedication() { return medication; }
    public void setMedication(String medication) { this.medication = medication; }
    public String getAllergy() { return allergy; }
    public void setAllergy(String allergy) { this.allergy = allergy; }
    public String getEmergencyContactPhoneMasked() { return emergencyContactPhoneMasked; }
    public void setEmergencyContactPhoneMasked(String v) { this.emergencyContactPhoneMasked = v; }
    public String getLongTextProfile() { return longTextProfile; }
    public void setLongTextProfile(String longTextProfile) { this.longTextProfile = longTextProfile; }
    public Appearance getAppearance() { return appearance; }
    public void setAppearance(Appearance appearance) { this.appearance = appearance; }
    public Fence getFence() { return fence; }
    public void setFence(Fence fence) { this.fence = fence; }
    public String getLostStatus() { return lostStatus; }
    public void setLostStatus(String lostStatus) { this.lostStatus = lostStatus; }
    public Long getProfileVersion() { return profileVersion; }
    public void setProfileVersion(Long profileVersion) { this.profileVersion = profileVersion; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
