package com.xiaohelab.guard.server.pushtoken.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 推送令牌注册请求（V2.1 §3.8.5.1）。 */
public class PushTokenRegisterRequest {

    @NotBlank
    @Pattern(regexp = "ANDROID_FCM|ANDROID_HMS|ANDROID_MIPUSH|IOS_APNS|WEB_PUSH",
            message = "platform 枚举非法")
    private String platform;

    @NotBlank
    @Size(min = 1, max = 128)
    @JsonProperty("device_id")
    private String deviceId;

    @NotBlank
    @Size(max = 512)
    @JsonProperty("push_token")
    private String pushToken;

    @NotBlank
    @Size(max = 32)
    @JsonProperty("app_version")
    private String appVersion;

    @Size(max = 64)
    @JsonProperty("os_version")
    private String osVersion;

    @Size(max = 64)
    @JsonProperty("device_model")
    private String deviceModel;

    @Pattern(regexp = "zh-CN|en-US", message = "locale 枚举非法")
    private String locale = "zh-CN";

    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }
    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    public String getPushToken() { return pushToken; }
    public void setPushToken(String pushToken) { this.pushToken = pushToken; }
    public String getAppVersion() { return appVersion; }
    public void setAppVersion(String appVersion) { this.appVersion = appVersion; }
    public String getOsVersion() { return osVersion; }
    public void setOsVersion(String osVersion) { this.osVersion = osVersion; }
    public String getDeviceModel() { return deviceModel; }
    public void setDeviceModel(String deviceModel) { this.deviceModel = deviceModel; }
    public String getLocale() { return locale; }
    public void setLocale(String locale) { this.locale = locale; }
}
