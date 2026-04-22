package com.xiaohelab.guard.server.auth.controller;

import com.xiaohelab.guard.server.auth.dto.*;
import com.xiaohelab.guard.server.auth.service.AuthService;
import com.xiaohelab.guard.server.common.annotation.Idempotent;
import com.xiaohelab.guard.server.common.dto.Result;
import com.xiaohelab.guard.server.common.security.AuthUser;
import com.xiaohelab.guard.server.common.security.SecurityUtil;
import com.xiaohelab.guard.server.pushtoken.service.UserPushTokenService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * 认证与会话管理。提供注册 / 登录 / 刷新 Token / 退出 / WebSocket 一次性 ticket 签发。
 * 用户资料与改密码接口见 {@link UserProfileController}（/api/v1/users/**）。
 * login/register/token/refresh 为公开白名单（见 SecurityConfig）。
 */
@Tag(name = "Auth", description = "认证、会话与 WebSocket Ticket")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final UserPushTokenService pushTokenService;

    /** 构造注入 {@link AuthService}。 */
    public AuthController(AuthService authService, UserPushTokenService pushTokenService) {
        this.authService = authService;
        this.pushTokenService = pushTokenService;
    }

    /**
     * 注册新家属用户。受 {@link Idempotent} 保护，重复提交同一 X-Request-Id 幂等返回。
     * @param req 注册请求体（username/email/password/nickname）
     * @return 注册成功的用户摘要信息
     */
    @PostMapping("/register")
    @Idempotent
    public Result<UserInfoResponse> register(@Valid @RequestBody RegisterRequest req) {
        return Result.ok(authService.register(req));
    }

    /**
     * 账号密码登录，成功后同时签发 Access + Refresh Token。
     * @param req     登录请求（username/password）
     * @param httpReq 用于采集客户端 IP 写入 last_login_ip
     */
    @PostMapping("/login")
    public Result<TokenResponse> login(@Valid @RequestBody LoginRequest req, HttpServletRequest httpReq) {
        return Result.ok(authService.login(req, httpReq));
    }

    /**
     * 使用 Refresh Token 换取新的 Access/Refresh Token 对。
     * @param req 刷新请求（refresh_token）
     */
    @PostMapping("/token/refresh")
    public Result<TokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest req) {
        return Result.ok(authService.refresh(req.getRefreshToken()));
    }

    /**
     * 请求密码重置（§3.6.4）。无论邮箱是否注册均返回相同消息，防止用户枚举。
     */
    @PostMapping("/password-reset/request")
    public Result<Void> passwordResetRequest(@Valid @RequestBody PasswordResetRequestDto req) {
        authService.requestPasswordReset(req);
        return Result.ok(null);
    }

    /**
     * 确认密码重置（§3.6.5）。用邮件中的 Token 设置新密码；Token 一次性消费。
     */
    @PostMapping("/password-reset/confirm")
    public Result<Void> passwordResetConfirm(@Valid @RequestBody PasswordResetConfirmDto req) {
        authService.confirmPasswordReset(req);
        return Result.ok(null);
    }

    /**
     * 退出登录（V2.1 §3.8.7.1）。
     * <p>将 access token 加入 Redis 黑名单；若请求体提供 {@code refresh_token}，一并拉黑；
     * 若提供 {@code push_token_id}，联动注销推送令牌。幂等。</p>
     *
     * @param auth Authorization 头（可空；为空时仍幂等返回成功）
     * @param body 可选登出负载
     */
    @PostMapping("/logout")
    @Idempotent
    public Result<Map<String, Object>> logout(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @RequestBody(required = false) LogoutRequest body) {
        String refreshToken = body == null ? null : body.getRefreshToken();
        OffsetDateTime revokedAt = authService.logout(auth, refreshToken);
        // 联动注销推送令牌（需登录上下文；未登录时 SecurityUtil.currentOrNull 返回 null 即跳过）
        if (body != null && body.getPushTokenId() != null) {
            AuthUser u = SecurityUtil.currentOrNull();
            if (u != null) {
                pushTokenService.revokeByIdForUser(u.getUserId(), body.getPushTokenId());
            }
        }
        return Result.ok(Map.of("revoked_at", revokedAt));
    }
}
