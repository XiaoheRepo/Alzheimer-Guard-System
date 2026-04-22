package com.xiaohelab.guard.server.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/** 登出请求体（V2.1 §3.8.7.1）。所有字段可选：只传 X-Request-Id 也幂等成立。 */
public class LogoutRequest {

    @JsonProperty("refresh_token")
    private String refreshToken;

    @JsonProperty("push_token_id")
    private Long pushTokenId;

    @JsonProperty("request_time")
    private String requestTime;

    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
    public Long getPushTokenId() { return pushTokenId; }
    public void setPushTokenId(Long pushTokenId) { this.pushTokenId = pushTokenId; }
    public String getRequestTime() { return requestTime; }
    public void setRequestTime(String requestTime) { this.requestTime = requestTime; }
}
