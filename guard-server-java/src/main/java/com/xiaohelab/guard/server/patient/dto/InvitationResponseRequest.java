package com.xiaohelab.guard.server.patient.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class InvitationResponseRequest {

    @NotBlank
    @Pattern(regexp = "ACCEPT|REJECT")
    private String action;

    @Size(max = 256)
    private String rejectReason;

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getRejectReason() { return rejectReason; }
    public void setRejectReason(String rejectReason) { this.rejectReason = rejectReason; }
}
