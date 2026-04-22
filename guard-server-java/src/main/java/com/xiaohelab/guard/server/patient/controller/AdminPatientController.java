package com.xiaohelab.guard.server.patient.controller;

import com.xiaohelab.guard.server.common.annotation.Idempotent;
import com.xiaohelab.guard.server.common.dto.CursorResponse;
import com.xiaohelab.guard.server.common.dto.Result;
import com.xiaohelab.guard.server.patient.dto.admin.AdminPatientDetailResponse;
import com.xiaohelab.guard.server.patient.dto.admin.AdminPatientListItem;
import com.xiaohelab.guard.server.patient.dto.admin.ForceTransferPrimaryRequest;
import com.xiaohelab.guard.server.patient.service.AdminPatientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 管理员患者全局访问（V2.1 增量）。
 * <p>仅 ADMIN / SUPER_ADMIN 可访问；强制转移仅 SUPER_ADMIN。</p>
 */
@Tag(name = "Admin.Patient", description = "管理员-患者治理（V2.1）")
@RestController
@RequestMapping("/api/v1/admin/patients")
public class AdminPatientController {

    private final AdminPatientService adminPatientService;

    public AdminPatientController(AdminPatientService adminPatientService) {
        this.adminPatientService = adminPatientService;
    }

    @GetMapping
    @Operation(summary = "3.3.15 患者列表（游标分页 + 主监护过滤）")
    public Result<CursorResponse<AdminPatientListItem>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String gender,
            @RequestParam(name = "primary_guardian_user_id", required = false) Long primaryGuardianUserId,
            @RequestParam(required = false) String cursor,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize) {
        return Result.ok(adminPatientService.list(
                keyword, status, gender, primaryGuardianUserId, cursor, pageSize));
    }

    @GetMapping("/{patientId}")
    @Operation(summary = "3.3.16 患者详情")
    public Result<AdminPatientDetailResponse> detail(@PathVariable Long patientId) {
        return Result.ok(adminPatientService.detail(patientId));
    }

    @PostMapping("/{patientId}/guardians/force-transfer")
    @Idempotent
    @Operation(summary = "3.3.17 强制转移主监护（仅 SUPER_ADMIN + CONFIRM_3）")
    public Result<Map<String, Object>> forceTransferPrimary(
            @PathVariable Long patientId,
            @Valid @RequestBody ForceTransferPrimaryRequest req,
            @RequestHeader(value = "X-Confirm-Level", required = false) String confirmLevel) {
        return Result.ok(adminPatientService.forceTransferPrimary(patientId, req, confirmLevel));
    }
}
