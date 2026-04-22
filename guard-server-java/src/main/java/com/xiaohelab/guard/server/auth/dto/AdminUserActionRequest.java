package com.xiaohelab.guard.server.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 管理员禁用 / 注销请求体（携带行政理由）。
 * <p>注销场景需同时携带 {@code X-Confirm-Level=CONFIRM_3} 请求头。</p>
 */
public class AdminUserActionRequest {

    @NotBlank(message = "reason 不得为空")
    @Size(min = 10, max = 256, message = "reason 长度需在 10-256 之间")
    @Schema(description = "行政理由（10-256）", example = "账号长期未登录且无法联系本人")
    private String reason;

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
