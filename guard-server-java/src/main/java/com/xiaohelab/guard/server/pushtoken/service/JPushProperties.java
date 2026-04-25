package com.xiaohelab.guard.server.pushtoken.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** 极光推送配置（backend_handbook §19.5）。 */
@ConfigurationProperties(prefix = "notification.jpush")
public class JPushProperties {

    private String appKey;
    private String masterSecret;
    private int retryMax = 3;
    private long liveTime = 86400L;
    private boolean apnsProduction = false;

    public boolean isEnabled() {
        return appKey != null && !appKey.isBlank()
                && masterSecret != null && !masterSecret.isBlank();
    }

    public String getAppKey() { return appKey; }
    public void setAppKey(String appKey) { this.appKey = appKey; }
    public String getMasterSecret() { return masterSecret; }
    public void setMasterSecret(String masterSecret) { this.masterSecret = masterSecret; }
    public int getRetryMax() { return retryMax; }
    public void setRetryMax(int retryMax) { this.retryMax = retryMax; }
    public long getLiveTime() { return liveTime; }
    public void setLiveTime(long liveTime) { this.liveTime = liveTime; }
    public boolean isApnsProduction() { return apnsProduction; }
    public void setApnsProduction(boolean apnsProduction) { this.apnsProduction = apnsProduction; }
}
