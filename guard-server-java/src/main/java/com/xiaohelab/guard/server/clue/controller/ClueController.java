package com.xiaohelab.guard.server.clue.controller;

import com.xiaohelab.guard.server.clue.dto.ClueReportRequest;
import com.xiaohelab.guard.server.clue.dto.ClueReviewRequest;
import com.xiaohelab.guard.server.clue.entity.ClueRecordEntity;
import com.xiaohelab.guard.server.clue.service.ClueService;
import com.xiaohelab.guard.server.common.annotation.Idempotent;
import com.xiaohelab.guard.server.common.dto.PagedResponse;
import com.xiaohelab.guard.server.common.dto.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 线索记录接口（API V2.0 §3.2）：
 * <ul>
 *   <li>3.2.3 POST /api/v1/clues/report - 家属或匿名好心人上报线索（canonical）</li>
 *   <li>3.2.4 POST /api/v1/clues/{id}/override - 复核覆写</li>
 *   <li>3.2.5 POST /api/v1/clues/{id}/reject - 复核驳回</li>
 *   <li>3.2.6 GET /api/v1/clues - 线索列表查询</li>
 * </ul>
 */
@Tag(name = "Clue", description = "线索记录（API §3.2）")
@RestController
@RequestMapping("/api/v1/clues")
public class ClueController {

    private final ClueService clueService;

    public ClueController(ClueService clueService) {
        this.clueService = clueService;
    }

    /** 3.2.3 canonical: POST /api/v1/clues/report。同时保留根路径向后兼容。 */
    @PostMapping({"/report", ""})
    @Idempotent
    @Operation(summary = "3.2.3 上报线索（家属 JWT 或匿名 entry_token）")
    public Result<ClueRecordEntity> report(@Valid @RequestBody ClueReportRequest req, HttpServletRequest http) {
        return Result.ok(clueService.familyReport(req, http.getRemoteAddr()));
    }

    @GetMapping("/{clueId}")
    @Operation(summary = "查询单条线索")
    public Result<ClueRecordEntity> get(@PathVariable Long clueId) {
        return Result.ok(clueService.get(clueId));
    }

    /**
     * 3.2.6 线索列表查询（Offset 分页 + 多条件筛选）。
     */
    @GetMapping
    @Operation(summary = "3.2.6 线索列表查询")
    public Result<PagedResponse<ClueRecordEntity>> list(
            @RequestParam(name = "task_id", required = false) Long taskId,
            @RequestParam(name = "patient_id", required = false) Long patientId,
            @RequestParam(required = false) String status,
            @RequestParam(name = "suspect_flag", required = false) Boolean suspectFlag,
            @RequestParam(name = "page_no", defaultValue = "1") int pageNo,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize) {
        Page<ClueRecordEntity> page = clueService.list(taskId, patientId, status, suspectFlag, pageNo, pageSize);
        return Result.ok(PagedResponse.fromPage(page, pageNo, pageSize));
    }

    /** 兼容路径：GET /clues/tasks/{taskId} → 等价于 list(task_id=xxx)。 */
    @GetMapping("/tasks/{taskId}")
    @Operation(summary = "兼容：按任务查询线索列表")
    public Result<PagedResponse<ClueRecordEntity>> listByTask(@PathVariable Long taskId,
                                                              @RequestParam(defaultValue = "1") int pageNo,
                                                              @RequestParam(defaultValue = "20") int pageSize) {
        Page<ClueRecordEntity> page = clueService.list(taskId, null, null, null, pageNo, pageSize);
        return Result.ok(PagedResponse.fromPage(page, pageNo, pageSize));
    }

    /** 3.2.4 复核覆写（可疑 → 有效）。 */
    @PostMapping("/{clueId}/override")
    @Idempotent
    @Operation(summary = "3.2.4 复核覆写（可疑 → 有效）")
    public Result<ClueRecordEntity> override(@PathVariable Long clueId,
                                             @RequestBody Map<String, String> body) {
        ClueReviewRequest req = new ClueReviewRequest();
        req.setAction("OVERRIDE");
        req.setReason(body.getOrDefault("override_reason", body.get("reason")));
        return Result.ok(clueService.review(clueId, req));
    }

    /** 3.2.5 复核驳回（可疑 → 无效）。 */
    @PostMapping("/{clueId}/reject")
    @Idempotent
    @Operation(summary = "3.2.5 复核驳回（可疑 → 无效）")
    public Result<ClueRecordEntity> reject(@PathVariable Long clueId,
                                           @RequestBody Map<String, String> body) {
        ClueReviewRequest req = new ClueReviewRequest();
        req.setAction("REJECT");
        req.setReason(body.getOrDefault("reject_reason", body.get("reason")));
        return Result.ok(clueService.review(clueId, req));
    }

    /** 兼容：POST /{clueId}/review 统一入口。 */
    @PostMapping("/{clueId}/review")
    @Idempotent
    @Operation(summary = "兼容：统一复核入口（action=OVERRIDE/REJECT）")
    public Result<ClueRecordEntity> review(@PathVariable Long clueId,
                                           @Valid @RequestBody ClueReviewRequest req) {
        return Result.ok(clueService.review(clueId, req));
    }
}
