package com.xiaohelab.guard.server.patient.entity;

import com.xiaohelab.guard.server.common.entity.BaseEntity;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "patient_profile")
public class PatientProfileEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "profile_no", length = 32, unique = true)
    private String profileNo;

    @Column(name = "name", length = 64, nullable = false)
    private String name;

    @Column(name = "gender", length = 16, nullable = false)
    private String gender;

    @Column(name = "birthday", nullable = false)
    private LocalDate birthday;

    @Column(name = "short_code", length = 6, nullable = false, unique = true)
    private String shortCode;

    @Column(name = "id_card_hash", length = 128)
    private String idCardHash;

    @Column(name = "chronic_diseases", length = 500)
    private String chronicDiseases;

    @Column(name = "medication", length = 500)
    private String medication;

    @Column(name = "allergy", length = 500)
    private String allergy;

    @Column(name = "emergency_contact_phone", length = 32)
    private String emergencyContactPhone;

    @Column(name = "avatar_url", length = 1024, nullable = false)
    private String avatarUrl;

    @Column(name = "long_text_profile", columnDefinition = "TEXT")
    private String longTextProfile;

    @Column(name = "appearance_height_cm")
    private Integer appearanceHeightCm;

    @Column(name = "appearance_weight_kg")
    private Integer appearanceWeightKg;

    @Column(name = "appearance_clothing", length = 500)
    private String appearanceClothing;

    @Column(name = "appearance_features", length = 500)
    private String appearanceFeatures;

    @Column(name = "fence_enabled", nullable = false)
    private Boolean fenceEnabled = false;

    @Column(name = "fence_center_lat")
    private Double fenceCenterLat;

    @Column(name = "fence_center_lng")
    private Double fenceCenterLng;

    @Column(name = "fence_radius_m")
    private Integer fenceRadiusM;

    @Column(name = "fence_coord_system", length = 10)
    private String fenceCoordSystem = "WGS84";

    @Column(name = "lost_status", length = 20, nullable = false)
    private String lostStatus = "NORMAL";

    @Column(name = "lost_status_event_time", nullable = false)
    private OffsetDateTime lostStatusEventTime = OffsetDateTime.now();

    @Column(name = "profile_version", nullable = false)
    private Long profileVersion = 0L;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getProfileNo() { return profileNo; }
    public void setProfileNo(String profileNo) { this.profileNo = profileNo; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public LocalDate getBirthday() { return birthday; }
    public void setBirthday(LocalDate birthday) { this.birthday = birthday; }
    public String getShortCode() { return shortCode; }
    public void setShortCode(String shortCode) { this.shortCode = shortCode; }
    public String getIdCardHash() { return idCardHash; }
    public void setIdCardHash(String idCardHash) { this.idCardHash = idCardHash; }
    public String getChronicDiseases() { return chronicDiseases; }
    public void setChronicDiseases(String chronicDiseases) { this.chronicDiseases = chronicDiseases; }
    public String getMedication() { return medication; }
    public void setMedication(String medication) { this.medication = medication; }
    public String getAllergy() { return allergy; }
    public void setAllergy(String allergy) { this.allergy = allergy; }
    public String getEmergencyContactPhone() { return emergencyContactPhone; }
    public void setEmergencyContactPhone(String emergencyContactPhone) { this.emergencyContactPhone = emergencyContactPhone; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
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
    public String getFenceCoordSystem() { return fenceCoordSystem; }
    public void setFenceCoordSystem(String fenceCoordSystem) { this.fenceCoordSystem = fenceCoordSystem; }
    public String getLostStatus() { return lostStatus; }
    public void setLostStatus(String lostStatus) { this.lostStatus = lostStatus; }
    public OffsetDateTime getLostStatusEventTime() { return lostStatusEventTime; }
    public void setLostStatusEventTime(OffsetDateTime lostStatusEventTime) { this.lostStatusEventTime = lostStatusEventTime; }
    public Long getProfileVersion() { return profileVersion; }
    public void setProfileVersion(Long profileVersion) { this.profileVersion = profileVersion; }
    public OffsetDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(OffsetDateTime deletedAt) { this.deletedAt = deletedAt; }
}
