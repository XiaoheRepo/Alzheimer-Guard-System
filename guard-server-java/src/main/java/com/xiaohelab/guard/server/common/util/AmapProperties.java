package com.xiaohelab.guard.server.common.util;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** 高德地图配置（backend_handbook §19.7）。 */
@ConfigurationProperties(prefix = "amap")
public class AmapProperties {

    private String apiKey;
    private String baseUrl = "https://restapi.amap.com/v3";
    private int timeoutMs = 3000;
    private int dailyLimit = 5000;

    public boolean isEnabled() {
        return apiKey != null && !apiKey.isBlank();
    }

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public int getTimeoutMs() { return timeoutMs; }
    public void setTimeoutMs(int timeoutMs) { this.timeoutMs = timeoutMs; }
    public int getDailyLimit() { return dailyLimit; }
    public void setDailyLimit(int dailyLimit) { this.dailyLimit = dailyLimit; }
}
