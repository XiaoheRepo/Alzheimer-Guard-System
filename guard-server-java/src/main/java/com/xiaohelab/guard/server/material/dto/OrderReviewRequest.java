package com.xiaohelab.guard.server.material.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class OrderReviewRequest {

    @NotBlank
    @Pattern(regexp = "APPROVE|REJECT")
    private String action;

    @Size(max = 256)
    private String reason;

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
