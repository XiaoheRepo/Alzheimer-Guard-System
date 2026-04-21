package com.xiaohelab.guard.server.patient.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;

public class InvitationCreateRequest {

    @NotNull
    @JsonProperty("invitee_user_id")
    private Long inviteeUserId;

    @Pattern(regexp = "GUARDIAN|PRIMARY_GUARDIAN")
    @JsonProperty("relation_role")
    private String relationRole = "GUARDIAN";

    @Size(max = 500)
    private String reason;

    @JsonProperty("expire_in_seconds")
    @Min(300) @Max(604800)
    private Integer expireInSeconds = 259200;

    public Long getInviteeUserId() { return inviteeUserId; }
    public void setInviteeUserId(Long inviteeUserId) { this.inviteeUserId = inviteeUserId; }
    public String getRelationRole() { return relationRole; }
    public void setRelationRole(String relationRole) { this.relationRole = relationRole; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public Integer getExpireInSeconds() { return expireInSeconds; }
    public void setExpireInSeconds(Integer expireInSeconds) { this.expireInSeconds = expireInSeconds; }
}
