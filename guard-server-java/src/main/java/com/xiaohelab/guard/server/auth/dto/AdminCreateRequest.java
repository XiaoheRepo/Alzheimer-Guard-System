package com.xiaohelab.guard.server.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 超管直接创建管理员账号请求体（API §3.6.21）。
 * <p>角色固定为 ADMIN；初始密码由服务端自动生成并一次性返回。</p>
 */
@Schema(description = "创建管理员请求")
public class AdminCreateRequest {

    @Schema(description = "用户名（英文+数字，4-32位）", example = "admin_zhangwei")
    @NotBlank(message = "用户名不能为空")
    @Size(min = 4, max = 32, message = "用户名长度 4-32")
    @Pattern(regexp = "^[a-zA-Z0-9_]{4,32}$", message = "用户名只允许英文、数字和下划线")
    private String username;

    @Schema(description = "邮箱（用于接收初始密码通知）", example = "admin@org.com")
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不合法")
    @Size(max = 128)
    private String email;

    @Schema(description = "昵称（可选，默认同用户名）", example = "张威")
    @Size(max = 64)
    private String nickname;

    @Schema(description = "备注/创建理由，不少于5字", example = "新增客服运营管理员")
    @NotBlank(message = "reason 不能为空")
    @Size(min = 5, max = 200, message = "reason 长度 5-200")
    private String reason;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    @JsonProperty("username")
    public void setUsernameJson(String v) { this.username = v; }
    @JsonProperty("email")
    public void setEmailJson(String v) { this.email = v; }
    @JsonProperty("nickname")
    public void setNicknameJson(String v) { this.nickname = v; }
    @JsonProperty("reason")
    public void setReasonJson(String v) { this.reason = v; }
}
