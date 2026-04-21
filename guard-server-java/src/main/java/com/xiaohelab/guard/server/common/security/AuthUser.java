package com.xiaohelab.guard.server.common.security;

/**
 * 当前登录用户上下文（由 JwtAuthFilter 注入 / Controller 端通过 @AuthenticationPrincipal 消费）。
 */
public class AuthUser {

    private final Long userId;
    private final String username;
    private final String role;

    public AuthUser(Long userId, String username, String role) {
        this.userId = userId;
        this.username = username;
        this.role = role;
    }

    public Long getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getRole() { return role; }

    public boolean isSuperAdmin() { return "SUPER_ADMIN".equals(role); }
    public boolean isAdmin() { return "ADMIN".equals(role) || isSuperAdmin(); }
}
