package com.xiaohelab.guard.server.gov.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/** 行为挑战结果（V2.1 §3.8.2.2 嵌套对象）。 */
public class CaptchaChallengeResult {

    /** SLIDER / ROTATE / BEHAVIOR */
    private String type = "SLIDER";

    /** 滑动轨迹加密串；仅做长度 & 摘要校验。 */
    private String trace;

    @JsonProperty("duration_ms")
    private Integer durationMs;

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getTrace() { return trace; }
    public void setTrace(String trace) { this.trace = trace; }
    public Integer getDurationMs() { return durationMs; }
    public void setDurationMs(Integer durationMs) { this.durationMs = durationMs; }
}
