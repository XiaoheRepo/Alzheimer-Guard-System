package com.xiaohelab.guard.server.clue.controller;

import com.xiaohelab.guard.server.clue.dto.ClueReportRequest;
import com.xiaohelab.guard.server.clue.dto.ClueReviewRequest;
import com.xiaohelab.guard.server.clue.entity.ClueRecordEntity;
import com.xiaohelab.guard.server.clue.service.ClueService;
import com.xiaohelab.guard.server.common.annotation.Idempotent;
import com.xiaohelab.guard.server.common.dto.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

/** 线索记录接口：家属上报 / 分页查询 / 管理员审核。 */
@Tag(name = "Clue", description = "线索记录")
@RestController
@RequestMapping("/api/v1/clues")
public class ClueController {

    private final ClueService clueService;

    public ClueController(ClueService clueService) {
        this.clueService = clueService;
    }

    @PostMapping
    @Idempotent
    public Result<ClueRecordEntity> report(@Valid @RequestBody ClueReportRequest req, HttpServletRequest http) {
        return Result.ok(clueService.familyReport(req, http.getRemoteAddr()));
    }

    @GetMapping("/{clueId}")
    public Result<ClueRecordEntity> get(@PathVariable Long clueId) {
        return Result.ok(clueService.get(clueId));
    }

    @GetMapping("/tasks/{taskId}")
    public Result<Page<ClueRecordEntity>> listByTask(@PathVariable Long taskId,
                                                     @RequestParam(defaultValue = "0") int page,
                                                     @RequestParam(defaultValue = "20") int size) {
        return Result.ok(clueService.listByTask(taskId, page, size));
    }

    @PostMapping("/{clueId}/review")
    @Idempotent
    public Result<ClueRecordEntity> review(@PathVariable Long clueId,
                                           @Valid @RequestBody ClueReviewRequest req) {
        return Result.ok(clueService.review(clueId, req));
    }
}
