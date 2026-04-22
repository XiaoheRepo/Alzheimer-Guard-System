package com.xiaohelab.guard.server.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;

/**
 * 管理员列表响应项（V2.1 增量，对应 API §3.6.15）。
 * <p>PII 字段统一在服务层通过 {@link com.xiaohelab.guard.server.common.util.DesensitizeUtil} 脱敏后填入。</p>
 */
public class AdminUserListItem {

    @JsonProperty("user_id")
    private String userId;
    private String username;
    private String nickname;
    /** 已脱敏 */
    private String email;
    /** 已脱敏 */
    private String phone;
    private String role;
    private String status;
    @JsonProperty("email_verified")
    private Boolean emailVerified;
    @JsonProperty("last_login_at")
    private OffsetDateTime lastLoginAt;
    @JsonProperty("created_at")
    private OffsetDateTime createdAt;

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
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
