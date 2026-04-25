package com.xiaohelab.guard.server.patient.dto.admin;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * 管理员患者详情（V2.1，API §3.3.16）。
 */
public class AdminPatientDetailResponse {

    @JsonProperty("patient_id")
    private String patientId;
    @JsonProperty("profile_no")
    private String profileNo;
    /** 脱敏后；wire 字段为 patient_name（API V2.0 §3.3 字段字典）。 */
    @JsonProperty("patient_name")
    private String patientName;
    private String gender;
    private LocalDate birthday;
    @JsonProperty("short_code")
    private String shortCode;
    @JsonProperty("avatar_url")
    private String avatarUrl;
    @JsonProperty("lost_status")
    private String lostStatus;
    @JsonProperty("profile_version")
    private Long profileVersion;
    @JsonProperty("fence_enabled")
    private Boolean fenceEnabled;
    @JsonProperty("created_at")
    private OffsetDateTime createdAt;
    @JsonProperty("updated_at")
    private OffsetDateTime updatedAt;
    private List<AdminGuardianItem> guardians;

    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }
    public String getProfileNo() { return profileNo; }
    public void setProfileNo(String profileNo) { this.profileNo = profileNo; }
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
    public String getLostStatus() { return lostStatus; }
    public void setLostStatus(String lostStatus) { this.lostStatus = lostStatus; }
    public Long getProfileVersion() { return profileVersion; }
    public void setProfileVersion(Long profileVersion) { this.profileVersion = profileVersion; }
    public Boolean getFenceEnabled() { return fenceEnabled; }
    public void setFenceEnabled(Boolean fenceEnabled) { this.fenceEnabled = fenceEnabled; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
    public List<AdminGuardianItem> getGuardians() { return guardians; }
    public void setGuardians(List<AdminGuardianItem> guardians) { this.guardians = guardians; }
}
