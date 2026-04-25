package com.xiaohelab.guard.server.patient.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * 更新患者档案请求（API V2.0 §3.3.2，PUT /patients/{id}/profile）。
 * 字段全部可选，Null 表示不修改（avatar_url 禁清空）。
 * <p>对齐基线：</p>
 * <ul>
 *   <li>姓名 wire 字段为 {@code patient_name}（同时兼容历史 {@code name}）。</li>
 *   <li>外观使用嵌套对象 {@code appearance{}}（围栏请走专用 PUT /patients/{id}/fence）。</li>
 * </ul>
 */
public class PatientUpdateRequest {

    @Size(min = 1, max = 64)
    @JsonProperty("patient_name")
    @JsonAlias({"name"})
    private String patientName;

    private String gender;
    private LocalDate birthday;

    @JsonProperty("avatar_url")
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

    /** 外观特征嵌套对象（可选，整体替换语义）。 */
    @Valid
    private AppearanceBlock appearance;

    /** 外观嵌套子对象（与 PatientCreateRequest.AppearanceBlock 字段一致）。 */
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
}
