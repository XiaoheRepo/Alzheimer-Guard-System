package com.xiaohelab.guard.server.security.config;

import com.xiaohelab.guard.server.security.filter.JwtAuthFilter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * 当前请求身份上下文工具。
 * Controller 与 Service 层通过此类获取当前操作人信息，
 * 避免直接依赖 SecurityContextHolder 增加耦合。
 */
@Component
public class SecurityContext {

    /**
     * 获取当前登录用户 ID（未登录返回 null）
     */
    public Long currentUserId() {
        return details() != null ? details().userId() : null;
    }

    /**
     * 获取当前登录用户名（未登录返回 null）
     */
    public String currentUsername() {
        return details() != null ? details().username() : null;
    }

    /**
     * 获取当前角色（未登录返回 null）
     */
    public String currentRole() {
        return details() != null ? details().role() : null;
    }

    /** 是否超级管理员 */
    public boolean isSuperAdmin() {
        return "SUPERADMIN".equals(currentRole());
    }

    /** 是否管理员及以上 */
    public boolean isAdmin() {
        String role = currentRole();
        return "ADMIN".equals(role) || "SUPERADMIN".equals(role);
    }

    private JwtAuthFilter.AuthDetails details() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getDetails() instanceof JwtAuthFilter.AuthDetails d) {
            return d;
        }
        return null;
    }
}
