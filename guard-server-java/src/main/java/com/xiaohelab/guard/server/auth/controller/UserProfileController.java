package com.xiaohelab.guard.server.auth.controller;

import com.xiaohelab.guard.server.auth.dto.ChangePasswordRequest;
import com.xiaohelab.guard.server.auth.dto.UserInfoResponse;
import com.xiaohelab.guard.server.auth.service.AuthService;
import com.xiaohelab.guard.server.common.annotation.Idempotent;
import com.xiaohelab.guard.server.common.dto.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * 当前用户个人资料接口（API §3.6.6 / §3.6.7）。
 * <ul>
 *   <li>GET /api/v1/users/me          — 获取当前用户信息</li>
 *   <li>PUT /api/v1/users/me/password  — 修改当前用户密码</li>
 * </ul>
 */
@Tag(name = "UserProfile", description = "当前用户个人资料")
@RestController
@RequestMapping("/api/v1/users")
public class UserProfileController {

    private final AuthService authService;

    public UserProfileController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 获取当前登录用户信息（API §3.6.6）。
     * 需携带有效 Bearer Token；email/phone 字段已脱敏。
     *
     * @return 当前用户信息 VO
     */
    @Operation(summary = "获取当前用户信息")
    @GetMapping("/me")
    public Result<UserInfoResponse> me() {
        return Result.ok(authService.me());
    }

    /**
     * 修改当前用户密码（API §3.6.7）。
     * 受 {@link Idempotent} 保护，防止重复提交。
     *
     * @param req 修改密码请求（old_password / new_password）
     */
    @Operation(summary = "修改密码")
    @PutMapping("/me/password")
    @Idempotent
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordRequest req) {
        authService.changePassword(req);
        return Result.ok(null);
    }
}
