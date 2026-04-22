package com.xiaohelab.guard.server.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 确认密码重置（POST /api/v1/auth/password-reset/confirm）。 */
@Schema(description = "确认密码重置请求")
public class PasswordResetConfirmDto {

    @NotBlank
    @Schema(description = "邮件中的一次性重置 Token")
    private String token;

    @NotBlank
    @Size(min = 8, max = 128, message = "密码长度须为 8-128 位")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
        message = "密码须至少含一位大写字母、小写字母和数字"
    )
    @JsonProperty("new_password")
    @Schema(description = "新密码，8-128 位，须含大小写字母+数字")
    private String newPassword;

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
}
