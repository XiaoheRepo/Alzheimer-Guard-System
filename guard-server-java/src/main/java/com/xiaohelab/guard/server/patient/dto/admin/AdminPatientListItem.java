package com.xiaohelab.guard.server.patient.dto.admin;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;

/**
 * 管理员患者列表项（V2.1，API §3.3.15）。
 */
public class AdminPatientListItem {

    @JsonProperty("patient_id")
    private String patientId;
    @JsonProperty("profile_no")
    private String profileNo;
    /** 脱敏后 */
    private String name;
    @JsonProperty("short_code")
    private String shortCode;
    private String gender;
    @JsonProperty("lost_status")
    private String lostStatus;
    @JsonProperty("primary_guardian_user_id")
    private String primaryGuardianUserId;
    @JsonProperty("guardian_count")
    private int guardianCount;
    @JsonProperty("created_at")
    private OffsetDateTime createdAt;

    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }
    public String getProfileNo() { return profileNo; }
    public void setProfileNo(String profileNo) { this.profileNo = profileNo; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getShortCode() { return shortCode; }
    public void setShortCode(String shortCode) { this.shortCode = shortCode; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public String getLostStatus() { return lostStatus; }
    public void setLostStatus(String lostStatus) { this.lostStatus = lostStatus; }
    public String getPrimaryGuardianUserId() { return primaryGuardianUserId; }
    public void setPrimaryGuardianUserId(String primaryGuardianUserId) { this.primaryGuardianUserId = primaryGuardianUserId; }
    public int getGuardianCount() { return guardianCount; }
    public void setGuardianCount(int guardianCount) { this.guardianCount = guardianCount; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
