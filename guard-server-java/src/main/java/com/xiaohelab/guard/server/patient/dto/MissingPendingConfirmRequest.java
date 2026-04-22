package com.xiaohelab.guard.server.patient.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 确认走失 / 确认安全请求（API §3.3.5）。
 * <p>对应 POST /api/v1/patients/{id}/missing-pending/confirm。</p>
 * <ul>
 *   <li>CONFIRM_MISSING：确认走失 → 自动创建 AUTO_UPGRADE 寻回任务</li>
 *   <li>CONFIRM_SAFE   ：确认安全 → 患者回归 NORMAL</li>
 * </ul>
 */
public class MissingPendingConfirmRequest {

    @NotBlank
    @Pattern(regexp = "CONFIRM_MISSING|CONFIRM_SAFE", message = "action 必须为 CONFIRM_MISSING / CONFIRM_SAFE")
    private String action;

    @Size(max = 500)
    private String remark;

    @JsonProperty("request_time")
    private String requestTime;

    public String getAction() { return action; }
    public void setAction(String v) { this.action = v; }
    public String getRemark() { return remark; }
    public void setRemark(String v) { this.remark = v; }
    public String getRequestTime() { return requestTime; }
    public void setRequestTime(String v) { this.requestTime = v; }
}
