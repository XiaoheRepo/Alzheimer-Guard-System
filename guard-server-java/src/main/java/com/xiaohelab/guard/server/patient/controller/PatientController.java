package com.xiaohelab.guard.server.patient.controller;

import com.xiaohelab.guard.server.common.annotation.Idempotent;
import com.xiaohelab.guard.server.common.dto.Result;
import com.xiaohelab.guard.server.patient.dto.*;
import com.xiaohelab.guard.server.patient.service.PatientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 患者档案接口（API V2.0 §3.3）：
 * 创建 / 查询 / 列表 / 更新（含 profile 路径契约） / 外观特征 / 围栏 / 逻辑删除 /
 * 走失或安全确认。
 */
@Tag(name = "Patient", description = "患者档案（API §3.3）")
@RestController
@RequestMapping("/api/v1/patients")
public class PatientController {

    private final PatientService patientService;

    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    /** 3.3.1 创建患者档案。 */
    @PostMapping
    @Idempotent
    @Operation(summary = "3.3.1 创建患者档案")
    public Result<PatientResponse> create(@Valid @RequestBody PatientCreateRequest req) {
        return Result.ok(patientService.create(req));
    }

    /** 3.3.13 患者列表查询。 */
    @GetMapping
    @Operation(summary = "3.3.13 患者列表查询")
    public Result<List<PatientResponse>> list() {
        return Result.ok(patientService.listMyPatients());
    }

    /** 3.3.12 查询患者档案。 */
    @GetMapping("/{patientId}")
    @Operation(summary = "3.3.12 查询患者档案")
    public Result<PatientResponse> get(@PathVariable Long patientId) {
        return Result.ok(patientService.get(patientId));
    }

    /**
     * 3.3.2 更新患者基础档案（canonical 路径：PUT /{id}/profile）。
     * 同时保留旧路径 PUT /{id} 以向后兼容，但新客户端必须走 /profile。
     */
    @PutMapping({"/{patientId}/profile", "/{patientId}"})
    @Idempotent
    @Operation(summary = "3.3.2 更新患者基础档案")
    public Result<PatientResponse> updateProfile(@PathVariable Long patientId,
                                                 @Valid @RequestBody PatientUpdateRequest req) {
        return Result.ok(patientService.update(patientId, req));
    }

    /** 3.3.3 更新外观特征（任务进行中实时着装更新）。 */
    @PutMapping("/{patientId}/appearance")
    @Idempotent
    @Operation(summary = "3.3.3 更新外观特征")
    public Result<PatientResponse> updateAppearance(@PathVariable Long patientId,
                                                    @Valid @RequestBody AppearanceUpdateRequest req) {
        return Result.ok(patientService.updateAppearance(patientId, req));
    }

    /** 3.3.14 逻辑删除患者档案。 */
    @DeleteMapping("/{patientId}")
    @Idempotent
    @Operation(summary = "3.3.14 逻辑删除患者档案")
    public Result<Void> delete(@PathVariable Long patientId) {
        patientService.delete(patientId);
        return Result.ok();
    }

    /** 3.3.4 设置电子围栏。 */
    @PutMapping("/{patientId}/fence")
    @Idempotent
    @Operation(summary = "3.3.4 设置电子围栏")
    public Result<PatientResponse> updateFence(@PathVariable Long patientId,
                                               @Valid @RequestBody FenceUpdateRequest req) {
        return Result.ok(patientService.updateFence(patientId, req));
    }

    /**
     * 3.3.5 走失确认 / 安全确认（canonical：POST /{id}/missing-pending/confirm）。
     * <p>action=CONFIRM_MISSING 将触发 AUTO_UPGRADE 任务创建；
     * action=CONFIRM_SAFE 仅使患者状态回归 NORMAL。</p>
     */
    @PostMapping("/{patientId}/missing-pending/confirm")
    @Idempotent
    @Operation(summary = "3.3.5 走失或安全确认")
    public Result<Map<String, Object>> missingPendingConfirm(@PathVariable Long patientId,
                                                             @Valid @RequestBody MissingPendingConfirmRequest req) {
        return Result.ok(patientService.missingPendingConfirm(patientId, req));
    }

    /** 兼容路径：POST /{id}/confirm-safe → action=CONFIRM_SAFE。 */
    @PostMapping("/{patientId}/confirm-safe")
    @Idempotent
    @Operation(summary = "3.3.5 兼容：确认安全（内部转 action=CONFIRM_SAFE）")
    public Result<PatientResponse> confirmSafe(@PathVariable Long patientId) {
        return Result.ok(patientService.confirmSafe(patientId));
    }
}
