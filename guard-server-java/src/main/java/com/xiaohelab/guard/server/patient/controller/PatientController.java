package com.xiaohelab.guard.server.patient.controller;

import com.xiaohelab.guard.server.common.annotation.Idempotent;
import com.xiaohelab.guard.server.common.dto.Result;
import com.xiaohelab.guard.server.patient.dto.*;
import com.xiaohelab.guard.server.patient.service.PatientService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 患者档案接口：创建 / 查询 / 列表 / 更新 / 逻辑删除 / 围栏配置 / 确认安全。 */
@Tag(name = "Patient", description = "患者档案")
@RestController
@RequestMapping("/api/v1/patients")
public class PatientController {

    private final PatientService patientService;

    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    @PostMapping
    @Idempotent
    public Result<PatientResponse> create(@Valid @RequestBody PatientCreateRequest req) {
        return Result.ok(patientService.create(req));
    }

    @GetMapping
    public Result<List<PatientResponse>> list() {
        return Result.ok(patientService.listMyPatients());
    }

    @GetMapping("/{patientId}")
    public Result<PatientResponse> get(@PathVariable Long patientId) {
        return Result.ok(patientService.get(patientId));
    }

    @PutMapping("/{patientId}")
    @Idempotent
    public Result<PatientResponse> update(@PathVariable Long patientId,
                                          @Valid @RequestBody PatientUpdateRequest req) {
        return Result.ok(patientService.update(patientId, req));
    }

    @DeleteMapping("/{patientId}")
    @Idempotent
    public Result<Void> delete(@PathVariable Long patientId) {
        patientService.delete(patientId);
        return Result.ok();
    }

    @PutMapping("/{patientId}/fence")
    @Idempotent
    public Result<PatientResponse> updateFence(@PathVariable Long patientId,
                                               @Valid @RequestBody FenceUpdateRequest req) {
        return Result.ok(patientService.updateFence(patientId, req));
    }

    @PostMapping("/{patientId}/confirm-safe")
    @Idempotent
    public Result<PatientResponse> confirmSafe(@PathVariable Long patientId) {
        return Result.ok(patientService.confirmSafe(patientId));
    }
}
