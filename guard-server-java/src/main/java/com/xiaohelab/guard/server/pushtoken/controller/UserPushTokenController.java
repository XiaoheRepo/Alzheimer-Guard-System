package com.xiaohelab.guard.server.pushtoken.controller;

import com.xiaohelab.guard.server.common.annotation.Idempotent;
import com.xiaohelab.guard.server.common.dto.Result;
import com.xiaohelab.guard.server.common.security.AuthUser;
import com.xiaohelab.guard.server.common.security.SecurityUtil;
import com.xiaohelab.guard.server.pushtoken.dto.PushTokenRegisterRequest;
import com.xiaohelab.guard.server.pushtoken.entity.UserPushTokenEntity;
import com.xiaohelab.guard.server.pushtoken.service.UserPushTokenService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 推送令牌接口（V2.1 §3.8.5）。
 * <ul>
 *   <li>POST   /api/v1/users/me/push-tokens         —— 注册 / 更新</li>
 *   <li>DELETE /api/v1/users/me/push-tokens/{id}    —— 注销（退出设备）</li>
 * </ul>
 */
@Tag(name = "User.PushToken", description = "用户推送令牌管理")
@RestController
@RequestMapping("/api/v1/users/me/push-tokens")
public class UserPushTokenController {

    private final UserPushTokenService service;

    public UserPushTokenController(UserPushTokenService service) {
        this.service = service;
    }

    /** 注册或更新当前用户的推送令牌。幂等：同 X-Request-Id 重放直接复用结果。 */
    @PostMapping
    @Idempotent
    public Result<Map<String, Object>> register(@Valid @RequestBody PushTokenRegisterRequest req) {
        AuthUser user = SecurityUtil.current();
        UserPushTokenEntity e = service.register(user.getUserId(), req);
        return Result.ok(Map.of(
                "push_token_id", e.getPushTokenId(),
                "status", e.getStatus(),
                "last_active_at", e.getLastActiveAt()));
    }

    /** 注销指定推送令牌（需为本人持有）。 */
    @DeleteMapping("/{pushTokenId}")
    public Result<Map<String, Object>> revoke(@PathVariable Long pushTokenId) {
        AuthUser user = SecurityUtil.current();
        UserPushTokenEntity e = service.revoke(user.getUserId(), pushTokenId);
        return Result.ok(Map.of(
                "push_token_id", e.getPushTokenId(),
                "status", e.getStatus(),
                "revoked_at", e.getRevokedAt()));
    }
}
