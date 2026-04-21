package com.xiaohelab.guard.server.rescue.controller;

import com.xiaohelab.guard.server.common.annotation.Idempotent;
import com.xiaohelab.guard.server.common.dto.Result;
import com.xiaohelab.guard.server.rescue.dto.TaskCloseRequest;
import com.xiaohelab.guard.server.rescue.dto.TaskCreateRequest;
import com.xiaohelab.guard.server.rescue.entity.RescueTaskEntity;
import com.xiaohelab.guard.server.rescue.service.RescueTaskService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/rescue/tasks")
public class RescueTaskController {

    private final RescueTaskService taskService;

    public RescueTaskController(RescueTaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping
    @Idempotent
    public Result<RescueTaskEntity> create(@Valid @RequestBody TaskCreateRequest req) {
        return Result.ok(taskService.create(req));
    }

    @GetMapping("/{taskId}")
    public Result<RescueTaskEntity> get(@PathVariable Long taskId) {
        return Result.ok(taskService.get(taskId));
    }

    @GetMapping
    public Result<Page<RescueTaskEntity>> list(@RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "20") int size) {
        return Result.ok(taskService.listMine(page, size));
    }

    @PostMapping("/{taskId}/close")
    @Idempotent
    public Result<RescueTaskEntity> close(@PathVariable Long taskId,
                                          @Valid @RequestBody TaskCloseRequest req) {
        return Result.ok(taskService.close(taskId, req));
    }

    @PostMapping("/{taskId}/sustain")
    @Idempotent
    public Result<RescueTaskEntity> sustain(@PathVariable Long taskId) {
        return Result.ok(taskService.sustain(taskId));
    }
}
