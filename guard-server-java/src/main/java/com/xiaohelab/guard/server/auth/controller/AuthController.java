package com.xiaohelab.guard.server.auth.controller;

import com.xiaohelab.guard.server.auth.dto.*;
import com.xiaohelab.guard.server.auth.service.AuthService;
import com.xiaohelab.guard.server.common.annotation.Idempotent;
import com.xiaohelab.guard.server.common.dto.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Idempotent
    public Result<UserInfoResponse> register(@Valid @RequestBody RegisterRequest req) {
        return Result.ok(authService.register(req));
    }

    @PostMapping("/login")
    public Result<TokenResponse> login(@Valid @RequestBody LoginRequest req, HttpServletRequest httpReq) {
        return Result.ok(authService.login(req, httpReq));
    }

    @PostMapping("/token/refresh")
    public Result<TokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest req) {
        return Result.ok(authService.refresh(req.getRefreshToken()));
    }

    @GetMapping("/me")
    public Result<UserInfoResponse> me() {
        return Result.ok(authService.me());
    }

    @PostMapping("/change-password")
    @Idempotent
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordRequest req) {
        authService.changePassword(req);
        return Result.ok();
    }

    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader(value = "Authorization", required = false) String auth) {
        authService.logout(auth);
        return Result.ok();
    }

    @PostMapping("/ws-ticket")
    public Result<Map<String, Object>> wsTicket() {
        String ticket = authService.issueWsTicket();
        return Result.ok(Map.of("ticket", ticket, "expires_in", 30));
    }
}
