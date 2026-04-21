package com.xiaohelab.guard.server.clue.controller;

import com.xiaohelab.guard.server.clue.dto.TrackPointRequest;
import com.xiaohelab.guard.server.clue.entity.PatientTrajectoryEntity;
import com.xiaohelab.guard.server.clue.service.PatientTrajectoryService;
import com.xiaohelab.guard.server.common.annotation.Idempotent;
import com.xiaohelab.guard.server.common.dto.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;

@Tag(name = "Track", description = "患者轨迹上报与围栏")
@RestController
@RequestMapping("/api/v1/tracks")
public class PatientTrajectoryController {

    private final PatientTrajectoryService trajectoryService;

    public PatientTrajectoryController(PatientTrajectoryService trajectoryService) {
        this.trajectoryService = trajectoryService;
    }

    @PostMapping("/points")
    @Idempotent
    @Operation(summary = "上报患者轨迹点（GPS/WIFI/CELL）")
    public Result<PatientTrajectoryEntity> report(@Valid @RequestBody TrackPointRequest req) {
        return Result.ok(trajectoryService.recordPoint(req));
    }

    @GetMapping("/tasks/{taskId}")
    @Operation(summary = "查询任务内的全部轨迹点")
    public Result<List<PatientTrajectoryEntity>> listByTask(@PathVariable Long taskId) {
        return Result.ok(trajectoryService.listByTask(taskId));
    }

    @GetMapping("/patients/{patientId}")
    @Operation(summary = "按时间范围查询患者轨迹点")
    public Result<List<PatientTrajectoryEntity>> listByPatient(
            @PathVariable Long patientId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to) {
        return Result.ok(trajectoryService.listByPatient(patientId, from, to));
    }
}
