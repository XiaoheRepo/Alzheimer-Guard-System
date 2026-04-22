package com.xiaohelab.guard.server.gov.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 滑块/行为验证签发请求（V2.1 §3.8.2.2）。 */
public class CaptchaIssueRequest {

    @NotBlank
    @Pattern(regexp = "MANUAL_ENTRY|CLUE_REPORT|ANONYMOUS_REPORT|RESCUE_ENTRY",
            message = "scene 枚举非法")
    private String scene;

    @NotBlank
    @Size(max = 128)
    @JsonProperty("device_fingerprint")
    private String deviceFingerprint;

    @NotNull
    @Valid
    @JsonProperty("challenge_result")
    private CaptchaChallengeResult challengeResult;

    public String getScene() { return scene; }
    public void setScene(String scene) { this.scene = scene; }
    public String getDeviceFingerprint() { return deviceFingerprint; }
    public void setDeviceFingerprint(String deviceFingerprint) { this.deviceFingerprint = deviceFingerprint; }
    public CaptchaChallengeResult getChallengeResult() { return challengeResult; }
    public void setChallengeResult(CaptchaChallengeResult challengeResult) { this.challengeResult = challengeResult; }
}
