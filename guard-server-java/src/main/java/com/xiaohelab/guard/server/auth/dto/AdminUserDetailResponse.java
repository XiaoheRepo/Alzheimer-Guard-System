package com.xiaohelab.guard.server.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;

/**
 * 管理员用户详情（V2.1 增量，对应 API §3.6.16）。
 */
public class AdminUserDetailResponse {

    @JsonProperty("user_id")
    private String userId;
    private String username;
    private String nickname;
    private String email;
    private String phone;
    private String role;
    private String status;
    @JsonProperty("email_verified")
    private Boolean emailVerified;
    @JsonProperty("last_login_at")
    private OffsetDateTime lastLoginAt;
    @JsonProperty("last_login_ip")
    private String lastLoginIp;
    @JsonProperty("deactivated_at")
    private OffsetDateTime deactivatedAt;
    @JsonProperty("created_at")
    private OffsetDateTime createdAt;
    @JsonProperty("updated_at")
    private OffsetDateTime updatedAt;
    private Stats stats;

    public static class Stats {
        @JsonProperty("primary_guardian_patient_count")
        private long primaryGuardianPatientCount;
        @JsonProperty("guardian_patient_count")
        private long guardianPatientCount;
        @JsonProperty("pending_material_order_count")
        private long pendingMaterialOrderCount;

        public long getPrimaryGuardianPatientCount() { return primaryGuardianPatientCount; }
        public void setPrimaryGuardianPatientCount(long v) { this.primaryGuardianPatientCount = v; }
        public long getGuardianPatientCount() { return guardianPatientCount; }
        public void setGuardianPatientCount(long v) { this.guardianPatientCount = v; }
        public long getPendingMaterialOrderCount() { return pendingMaterialOrderCount; }
        public void setPendingMaterialOrderCount(long v) { this.pendingMaterialOrderCount = v; }
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Boolean getEmailVerified() { return emailVerified; }
    public void setEmailVerified(Boolean emailVerified) { this.emailVerified = emailVerified; }
    public OffsetDateTime getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(OffsetDateTime lastLoginAt) { this.lastLoginAt = lastLoginAt; }
    public String getLastLoginIp() { return lastLoginIp; }
    public void setLastLoginIp(String lastLoginIp) { this.lastLoginIp = lastLoginIp; }
    public OffsetDateTime getDeactivatedAt() { return deactivatedAt; }
    public void setDeactivatedAt(OffsetDateTime deactivatedAt) { this.deactivatedAt = deactivatedAt; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
    public Stats getStats() { return stats; }
    public void setStats(Stats stats) { this.stats = stats; }
}
