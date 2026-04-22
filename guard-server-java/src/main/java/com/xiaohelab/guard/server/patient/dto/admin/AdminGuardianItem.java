package com.xiaohelab.guard.server.patient.dto.admin;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;

/**
 * 管理员视角下的监护人条目（PII 已脱敏）。
 */
public class AdminGuardianItem {

    @JsonProperty("user_id")
    private String userId;
    /** 脱敏 */
    private String username;
    /** 脱敏 */
    private String nickname;
    /** 脱敏 */
    private String phone;
    @JsonProperty("relation_role")
    private String relationRole;
    @JsonProperty("relation_status")
    private String relationStatus;
    @JsonProperty("joined_at")
    private OffsetDateTime joinedAt;

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getRelationRole() { return relationRole; }
    public void setRelationRole(String relationRole) { this.relationRole = relationRole; }
    public String getRelationStatus() { return relationStatus; }
    public void setRelationStatus(String relationStatus) { this.relationStatus = relationStatus; }
    public OffsetDateTime getJoinedAt() { return joinedAt; }
    public void setJoinedAt(OffsetDateTime joinedAt) { this.joinedAt = joinedAt; }
}
