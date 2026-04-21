package com.xiaohelab.guard.server.clue.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class ClueReviewRequest {

    @NotBlank
    @Pattern(regexp = "OVERRIDE|REJECT")
    private String action;

    @Size(max = 256)
    private String reason;

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
