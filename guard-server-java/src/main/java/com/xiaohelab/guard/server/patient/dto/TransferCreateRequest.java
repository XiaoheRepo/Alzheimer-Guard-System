package com.xiaohelab.guard.server.patient.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;

public class TransferCreateRequest {

    @NotNull
    @JsonProperty("to_user_id")
    private Long toUserId;

    @NotBlank
    @Size(max = 500)
    private String reason;

    @JsonProperty("expire_in_seconds")
    @Min(300) @Max(604800)
    private Integer expireInSeconds = 259200;

    public Long getToUserId() { return toUserId; }
    public void setToUserId(Long toUserId) { this.toUserId = toUserId; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public Integer getExpireInSeconds() { return expireInSeconds; }
    public void setExpireInSeconds(Integer expireInSeconds) { this.expireInSeconds = expireInSeconds; }
}
