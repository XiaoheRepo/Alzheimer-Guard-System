package com.xiaohelab.guard.server.interfaces.auth;

import com.xiaohelab.guard.server.application.auth.LoginUseCase;
import com.xiaohelab.guard.server.application.auth.RegisterUseCase;
import com.xiaohelab.guard.server.common.response.ApiResponse;
import com.xiaohelab.guard.server.infrastructure.persistence.do_.SysUserDO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 认证接口（注册 / 登录）。
 * 所有接口均无需 Bearer Token（SecurityConfig 中已配置为公开路由）。
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final RegisterUseCase registerUseCase;
    private final LoginUseCase loginUseCase;

    /** 用户注册 */
    @PostMapping("/register")
    public ApiResponse<Map<String, Object>> register(
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId,
            @Valid @RequestBody RegisterRequest req) {

        SysUserDO user = registerUseCase.execute(
                req.getUsername(), req.getPassword(), req.getPhone(), req.getRealName());

        return ApiResponse.ok(Map.of(
                "user_id", String.valueOf(user.getId()),
                "username", user.getUsername(),
                "role", user.getRole()
        ), traceId);
    }

    /** 用户登录 */
    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId,
            @Valid @RequestBody LoginRequest req) {

        LoginUseCase.LoginResult result = loginUseCase.execute(req.getUsername(), req.getPassword());

        return ApiResponse.ok(Map.of(
                "access_token", result.getAccessToken(),
                "token_type", "Bearer",
                "user_id", String.valueOf(result.getUserId()),
                "username", result.getUsername(),
                "role", result.getRole()
        ), traceId);
    }

    // ===== 请求体 DTO =====

    @Data
    public static class RegisterRequest {
        @NotBlank
        @Size(min = 2, max = 32)
        @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "用户名只允许字母、数字和下划线")
        private String username;

        @NotBlank
        @Size(min = 8, max = 64)
        private String password;

        @NotBlank
        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
        private String phone;

        @NotBlank
        @Size(min = 2, max = 32)
        private String realName;
    }

    @Data
    public static class LoginRequest {
        @NotBlank
        private String username;

        @NotBlank
        private String password;
    }
}
