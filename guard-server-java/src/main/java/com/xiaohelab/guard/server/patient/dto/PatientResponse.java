package com.xiaohelab.guard.server.patient.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public class PatientResponse {

    @JsonProperty("patient_id")
    private Long patientId;
    private String name;
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
    @JsonProperty("appearance_height_cm")
    private Integer appearanceHeightCm;
    @JsonProperty("appearance_weight_kg")
    private Integer appearanceWeightKg;
    @JsonProperty("appearance_clothing")
    private String appearanceClothing;
    @JsonProperty("appearance_features")
    private String appearanceFeatures;
    @JsonProperty("fence_enabled")
    private Boolean fenceEnabled;
    @JsonProperty("fence_center_lat")
    private Double fenceCenterLat;
    @JsonProperty("fence_center_lng")
    private Double fenceCenterLng;
    @JsonProperty("fence_radius_m")
    private Integer fenceRadiusM;
    @JsonProperty("lost_status")
    private String lostStatus;
    @JsonProperty("profile_version")
    private Long profileVersion;
    @JsonProperty("created_at")
    private OffsetDateTime createdAt;
    @JsonProperty("updated_at")
    private OffsetDateTime updatedAt;

    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
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
    public Integer getAppearanceHeightCm() { return appearanceHeightCm; }
    public void setAppearanceHeightCm(Integer appearanceHeightCm) { this.appearanceHeightCm = appearanceHeightCm; }
    public Integer getAppearanceWeightKg() { return appearanceWeightKg; }
    public void setAppearanceWeightKg(Integer appearanceWeightKg) { this.appearanceWeightKg = appearanceWeightKg; }
    public String getAppearanceClothing() { return appearanceClothing; }
    public void setAppearanceClothing(String appearanceClothing) { this.appearanceClothing = appearanceClothing; }
    public String getAppearanceFeatures() { return appearanceFeatures; }
    public void setAppearanceFeatures(String appearanceFeatures) { this.appearanceFeatures = appearanceFeatures; }
    public Boolean getFenceEnabled() { return fenceEnabled; }
    public void setFenceEnabled(Boolean fenceEnabled) { this.fenceEnabled = fenceEnabled; }
    public Double getFenceCenterLat() { return fenceCenterLat; }
    public void setFenceCenterLat(Double fenceCenterLat) { this.fenceCenterLat = fenceCenterLat; }
    public Double getFenceCenterLng() { return fenceCenterLng; }
    public void setFenceCenterLng(Double fenceCenterLng) { this.fenceCenterLng = fenceCenterLng; }
    public Integer getFenceRadiusM() { return fenceRadiusM; }
    public void setFenceRadiusM(Integer fenceRadiusM) { this.fenceRadiusM = fenceRadiusM; }
    public String getLostStatus() { return lostStatus; }
    public void setLostStatus(String lostStatus) { this.lostStatus = lostStatus; }
    public Long getProfileVersion() { return profileVersion; }
    public void setProfileVersion(Long profileVersion) { this.profileVersion = profileVersion; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
