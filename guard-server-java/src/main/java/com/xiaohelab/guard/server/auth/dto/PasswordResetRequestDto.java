package com.xiaohelab.guard.server.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** 请求密码重置（POST /api/v1/auth/password-reset/request）。 */
@Schema(description = "密码重置请求")
public class PasswordResetRequestDto {

    @NotBlank
    @Email
    @Schema(description = "注册邮箱", example = "user@example.com")
    private String email;

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
