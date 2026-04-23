package com.xiaohelab.guard.server.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

/**
 * 管理员禁用 / 启用 / 注销请求体（可携带行政理由）。
 * <p>注销场景 reason 必须 ≥ 10 字符（Service 层校验）；禁用 / 启用场景 reason 可选。</p>
 * <p>注销场景需同时携带 {@code X-Confirm-Level=CONFIRM_3} 请求头。</p>
 */
public class AdminUserActionRequest {

    @Size(max = 256, message = "reason 不得超过 256 字符")
    @Schema(description = "行政理由（注销必填 ≥ 10 字符；禁用/启用可选）", example = "账号长期未登录且无法联系本人")
    private String reason;

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
