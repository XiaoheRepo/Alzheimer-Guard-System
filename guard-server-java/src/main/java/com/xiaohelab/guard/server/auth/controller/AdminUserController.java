package com.xiaohelab.guard.server.auth.controller;

import com.xiaohelab.guard.server.auth.dto.AdminCreateRequest;
import com.xiaohelab.guard.server.auth.dto.AdminUserActionRequest;
import com.xiaohelab.guard.server.auth.dto.AdminUserDetailResponse;
import com.xiaohelab.guard.server.auth.dto.AdminUserListItem;
import com.xiaohelab.guard.server.auth.dto.AdminUserUpdateRequest;
import com.xiaohelab.guard.server.auth.service.AdminUserService;
import com.xiaohelab.guard.server.common.annotation.Idempotent;
import com.xiaohelab.guard.server.common.dto.CursorResponse;
import com.xiaohelab.guard.server.common.dto.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 管理员用户管理 - HTTP 入口（V2.1 增量）。
 * <p>仅做参数装配与 DTO 转发，一切授权 / 业务 / Outbox / 审计全在 {@link AdminUserService}。</p>
 */
@Tag(name = "Admin.User", description = "管理员-用户管理（V2.1）")
@RestController
@RequestMapping("/api/v1/admin/users")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @PostMapping
    @Idempotent
    @Operation(summary = "3.6.21 创建管理员账号（仅 SUPER_ADMIN，CONFIRM_2）")
    public Result<Map<String, Object>> createAdmin(
            @Valid @RequestBody AdminCreateRequest req,
            @Parameter(description = "必须传 CONFIRM_2")
            @RequestHeader(value = "X-Confirm-Level", required = false) String confirmLevel) {
        return Result.ok(adminUserService.createAdmin(req, confirmLevel));
    }

    @GetMapping
    @Operation(summary = "3.6.15 用户列表（游标分页）")    public Result<CursorResponse<AdminUserListItem>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String cursor,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize) {
        return Result.ok(adminUserService.list(keyword, role, status, cursor, pageSize));
    }

    @GetMapping("/{userId}")
    @Operation(summary = "3.6.16 用户详情")
    public Result<AdminUserDetailResponse> detail(@PathVariable Long userId) {
        return Result.ok(adminUserService.detail(userId));
    }

    @PutMapping("/{userId}")
    @Idempotent
    @Operation(summary = "3.6.17 修改用户信息（含 role 变更）")
    public Result<AdminUserDetailResponse> update(
            @PathVariable Long userId,
            @Valid @RequestBody AdminUserUpdateRequest req,
            @Parameter(description = "改 role 时需 CONFIRM_2")
            @RequestHeader(value = "X-Confirm-Level", required = false) String confirmLevel) {
        return Result.ok(adminUserService.update(userId, req, confirmLevel));
    }

    @PostMapping("/{userId}/disable")
    @Idempotent
    @Operation(summary = "3.6.18 禁用用户（需 CONFIRM_2）")
    public Result<Map<String, Object>> disable(
            @PathVariable Long userId,
            @Valid @RequestBody AdminUserActionRequest req,
            @RequestHeader(value = "X-Confirm-Level", required = false) String confirmLevel) {
        return Result.ok(adminUserService.disable(userId, req, confirmLevel));
    }

    @PostMapping("/{userId}/enable")
    @Idempotent
    @Operation(summary = "3.6.19 启用用户（需 CONFIRM_1）")
    public Result<Map<String, Object>> enable(
            @PathVariable Long userId,
            @Valid @RequestBody AdminUserActionRequest req,
            @RequestHeader(value = "X-Confirm-Level", required = false) String confirmLevel) {
        return Result.ok(adminUserService.enable(userId, req, confirmLevel));
    }

    @DeleteMapping("/{userId}")
    @Idempotent
    @Operation(summary = "3.6.20 注销用户（需 CONFIRM_3）")
    public Result<Void> deactivate(
            @PathVariable Long userId,
            @Valid @RequestBody AdminUserActionRequest req,
            @RequestHeader(value = "X-Confirm-Level", required = false) String confirmLevel) {
        adminUserService.deactivate(userId, req, confirmLevel);
        return Result.ok();
    }
}
