package com.xiaohelab.guard.server.common.security;

import com.xiaohelab.guard.server.common.error.ErrorCode;
import com.xiaohelab.guard.server.common.exception.BizException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 当前登录上下文工具。
 */
public final class SecurityUtil {

    private SecurityUtil() {}

    public static AuthUser current() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof AuthUser au)) {
            throw BizException.of(ErrorCode.E_GOV_4011);
        }
        return au;
    }

    public static AuthUser currentOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AuthUser au) return au;
        return null;
    }

    public static Long currentUserId() {
        return current().getUserId();
    }
}
