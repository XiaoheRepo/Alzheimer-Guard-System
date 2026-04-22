package com.xiaohelab.guard.server.auth.controller;

import com.xiaohelab.guard.server.auth.dto.*;
import com.xiaohelab.guard.server.auth.service.AuthService;
import com.xiaohelab.guard.server.common.annotation.Idempotent;
import com.xiaohelab.guard.server.common.dto.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 认证与会话管理。提供注册 / 登录 / 刷新 Token / 当前用户 / 修改密码 /
 * 退出 / WebSocket 一次性 ticket 签发。除 me/changePassword/logout/wsTicket 外，
 * 其余接口均为白名单（见 SecurityConfig）。
 */
@Tag(name = "Auth", description = "认证、会话与 WebSocket Ticket")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    /** 构造注入 {@link AuthService}。 */
    public AuthController(AuthService authService) {
        this.authService = authService;
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

    /** 查询当前登录用户资料（由 JWT 解析得到 userId）。 */
    @GetMapping("/me")
    public Result<UserInfoResponse> me() {
        return Result.ok(authService.me());
    }

    /**
     * 修改当前用户密码。受 {@link Idempotent} 保护。
     * @param req 修改密码请求（old_password/new_password）
     */
    @PostMapping("/change-password")
    @Idempotent
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordRequest req) {
        authService.changePassword(req);
        return Result.ok();
    }

    /**
     * 退出登录。将当前 Bearer Token 写入 Redis 黑名单直至其原始 exp。
     * @param auth Authorization 头，可空（空时幂等返回成功）
     */
    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader(value = "Authorization", required = false) String auth) {
        authService.logout(auth);
        return Result.ok();
    }

    /**
     * 签发一次性 WebSocket 握手 ticket（30s TTL，一次性消费）。
     * @return {@code {"ticket": "Wxxx", "expires_in": 30}}
     */
    @PostMapping("/ws-ticket")
    public Result<Map<String, Object>> wsTicket() {
        String ticket = authService.issueWsTicket();
        return Result.ok(Map.of("ticket", ticket, "expires_in", 30));
    }
}
