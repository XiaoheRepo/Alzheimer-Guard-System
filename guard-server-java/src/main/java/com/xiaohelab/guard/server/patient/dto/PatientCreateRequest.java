package com.xiaohelab.guard.server.patient.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

public class PatientCreateRequest {

    @NotBlank
    @Size(min = 1, max = 64)
    private String name;

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

    @JsonProperty("appearance_height_cm")
    @Min(50) @Max(250)
    private Integer appearanceHeightCm;

    @JsonProperty("appearance_weight_kg")
    @Min(20) @Max(250)
    private Integer appearanceWeightKg;

    @JsonProperty("appearance_clothing")
    @Size(max = 500)
    private String appearanceClothing;

    @JsonProperty("appearance_features")
    @Size(max = 500)
    private String appearanceFeatures;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
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
    public Integer getAppearanceHeightCm() { return appearanceHeightCm; }
    public void setAppearanceHeightCm(Integer appearanceHeightCm) { this.appearanceHeightCm = appearanceHeightCm; }
    public Integer getAppearanceWeightKg() { return appearanceWeightKg; }
    public void setAppearanceWeightKg(Integer appearanceWeightKg) { this.appearanceWeightKg = appearanceWeightKg; }
    public String getAppearanceClothing() { return appearanceClothing; }
    public void setAppearanceClothing(String appearanceClothing) { this.appearanceClothing = appearanceClothing; }
    public String getAppearanceFeatures() { return appearanceFeatures; }
    public void setAppearanceFeatures(String appearanceFeatures) { this.appearanceFeatures = appearanceFeatures; }
}
