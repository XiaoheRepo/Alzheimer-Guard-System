package com.xiaohelab.guard.server.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class RegisterRequest {

    @NotBlank
    @Pattern(regexp = "^[a-zA-Z0-9_]{4,32}$", message = "username 格式非法")
    private String username;

    @NotBlank
    @Email
    @Size(max = 128)
    private String email;

    /**
     * 家属注册必填（API V2.0 §3.6.1）。中国大陆手机号。
     * 服务端以唯一约束 + partial unique index 保证全局唯一。
     */
    @NotBlank
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "phone 格式非法")
    private String phone;

    @NotBlank
    @Size(min = 8, max = 64)
    private String password;

    @Size(max = 64)
    private String nickname;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
}
