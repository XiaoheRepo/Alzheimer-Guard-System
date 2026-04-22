package com.xiaohelab.guard.server.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 管理员修改用户信息请求体（V2.1 增量，对应 API §3.6.17）。
 * <p>所有字段均为可选；服务端按当前角色执行授权矩阵（ADMIN 不得传 {@code role}）。</p>
 */
public class AdminUserUpdateRequest {

    @Size(max = 64, message = "nickname 长度不得超过 64")
    @Schema(description = "昵称，最大 64 字符", example = "张三")
    private String nickname;

    @Email(message = "email 格式非法")
    @Size(max = 128, message = "email 长度不得超过 128")
    @Schema(description = "邮箱，修改后 email_verified 将置为 false", example = "zhang@example.com")
    private String email;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "phone 格式非法（需为中国大陆 11 位手机号）")
    @Schema(description = "中国大陆手机号", example = "13812345678")
    private String phone;

    @Pattern(regexp = "^(FAMILY|ADMIN|SUPER_ADMIN)$", message = "role 枚举非法")
    @Schema(description = "角色，仅 SUPER_ADMIN 可传；ADMIN 传入即报错", example = "ADMIN")
    private String role;

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
