package com.xiaohelab.guard.server.interfaces.clue;

import com.xiaohelab.guard.server.application.clue.ClueService;
import com.xiaohelab.guard.server.application.governance.AuditLogService;
import com.xiaohelab.guard.server.common.exception.BizException;
import com.xiaohelab.guard.server.common.response.ApiResponse;
import com.xiaohelab.guard.server.common.response.PageResponse;
import com.xiaohelab.guard.server.domain.clue.entity.ClueRecordEntity;
import com.xiaohelab.guard.server.infrastructure.persistence.do_.SysLogDO;
import com.xiaohelab.guard.server.security.config.SecurityContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 管理端线索接口。所有 Mapper 调用已通过 application 层服务路由。
 */
@RestController
@RequiredArgsConstructor
public class AdminClueController {

    private final ClueService clueService;
    private final AuditLogService auditLogService;
    private final SecurityContext securityContext;

    /** 3.2.9 — 管理端读取线索复核详情 */
    @GetMapping("/api/v1/admin/clues/{clueId}")
    public ApiResponse<Map<String, Object>> getClueDetail(
            @PathVariable Long clueId,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId) {

        requireAdmin();
        ClueRecordEntity clue = clueService.getById(clueId);
        return ApiResponse.ok(buildFullVO(clue, true), traceId);
    }

    /** 3.2.10 — 待复核队列（suspect_flag=TRUE, PENDING） */
    @GetMapping("/api/v1/admin/clues/review/queue")
    public ApiResponse<PageResponse<Map<String, Object>>> reviewQueue(
            @RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId) {

        requireAdmin();
        int offset = (pageNo - 1) * pageSize;
        List<ClueRecordEntity> list = clueService.listReviewQueue(pageSize, offset);
        long total = clueService.countReviewQueue();

        List<Map<String, Object>> items = list.stream()
                .map(c -> buildFullVO(c, true)).toList();

        return ApiResponse.ok(PageResponse.<Map<String, Object>>builder()
                .items(items).pageNo(pageNo).pageSize(pageSize).total(total)
                .hasNext(total > (long) pageNo * pageSize)
                .build(), traceId);
    }

    /** 3.6.2 — 疑似线索队列（suspect_flag = TRUE） */
    @GetMapping("/api/v1/admin/clues/suspected")
    public ApiResponse<PageResponse<Map<String, Object>>> suspectedQueue(
            @RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) Long taskId,
            @RequestParam(name = "patient_id", required = false) Long patientId,
            @RequestParam(name = "review_status", required = false) String reviewStatus,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId) {

        requireAdmin();
        int offset = (pageNo - 1) * pageSize;
        List<ClueRecordEntity> list = clueService.listSuspected(reviewStatus, taskId, patientId,
                pageSize, offset);
        long total = clueService.countSuspectedFiltered(reviewStatus, taskId, patientId);

        List<Map<String, Object>> items = list.stream().map(c -> {
            var m = new java.util.LinkedHashMap<String, Object>();
            m.put("clue_id", String.valueOf(c.getId()));
            m.put("task_id", c.getTaskId() == null ? null : String.valueOf(c.getTaskId()));
            m.put("patient_id", c.getPatientId() == null ? null : String.valueOf(c.getPatientId()));
            m.put("location", Map.of(
                    "lat", c.getLocationLat() != null ? c.getLocationLat() : 0.0,
                    "lng", c.getLocationLng() != null ? c.getLocationLng() : 0.0));
            m.put("risk_score", c.getRiskScore() != null ? c.getRiskScore() : 0);
            m.put("suspect_reason", c.getSuspectReason() != null ? c.getSuspectReason() : "");
            m.put("reported_at", c.getCreatedAt() != null ? c.getCreatedAt().toString() : null);
            m.put("review_status", c.getReviewStatus() != null ? c.getReviewStatus() : "");
            return (Map<String, Object>) m;
        }).toList();

        return ApiResponse.ok(PageResponse.<Map<String, Object>>builder()
                .items(items).pageNo(pageNo).pageSize(pageSize).total(total)
                .hasNext(total > (long) pageNo * pageSize)
                .build(), traceId);
    }

    /** 3.2.12 — 分配线索复核责任人（写 sys_log） */
    @PostMapping("/api/v1/admin/clues/{clueId}/assign")
    @Transactional
    public ApiResponse<Map<String, Object>> assignClue(
            @PathVariable Long clueId,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            @Valid @RequestBody AssignRequest req) {

        requireAdmin();
        clueService.assign(clueId, req.getAssigneeUserId());

        SysLogDO log = new SysLogDO();
        log.setModule("CLUE");
        log.setAction("ASSIGN");
        log.setObjectId(String.valueOf(clueId));
        log.setResult("SUCCESS");
        log.setRiskLevel("LOW");
        log.setOperatorUserId(securityContext.currentUserId());
        log.setOperatorUsername(securityContext.currentUsername());
        log.setActionSource("USER");
        log.setExecutionMode("MANUAL");
        log.setDetail("{\"assignee_user_id\":" + req.getAssigneeUserId() +
                (req.getReason() != null ? ",\"reason\":\"" + req.getReason().replace("\"", "\\\"") + "\"" : "") + "}");
        log.setRequestId(requestId);
        log.setTraceId(traceId);
        log.setExecutedAt(Instant.now());
        auditLogService.writeLog(log);

        return ApiResponse.ok(Map.of(
                "clue_id", String.valueOf(clueId),
                "assignee_user_id", String.valueOf(req.getAssigneeUserId()),
                "assigned_at", Instant.now().toString()), traceId);
    }

    /** 3.2.13 — 预留接口：毕设版本不开放，直接返回 403 */
    @PostMapping("/api/v1/admin/clues/{clueId}/request-evidence")
    public ApiResponse<Void> requestEvidence(@PathVariable Long clueId) {
        throw BizException.of("E_GOV_4030");
    }

    /** 3.2.14 — 线索复核统计指标 */
    @GetMapping("/api/v1/admin/clues/statistics")
    public ApiResponse<Map<String, Object>> statistics(
            @RequestParam(name = "time_from", required = false) String timeFrom,
            @RequestParam(name = "time_to", required = false) String timeTo,
            @RequestParam(defaultValue = "day") String granularity,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId) {

        requireAdmin();
        long totalClues = clueService.countAll(timeFrom, timeTo);
        long suspected = clueService.countSuspected(timeFrom, timeTo);
        long overridden = clueService.countOverridden(timeFrom, timeTo);
        long rejected = clueService.countRejected(timeFrom, timeTo);

        return ApiResponse.ok(Map.of(
                "time_from", timeFrom == null ? "" : timeFrom,
                "time_to", timeTo == null ? "" : timeTo,
                "granularity", granularity,
                "total_clues", totalClues,
                "suspected_count", suspected,
                "overridden_count", overridden,
                "rejected_count", rejected,
                "avg_review_minutes", 0), traceId);
    }

    // ===== helpers =====

    private Map<String, Object> buildFullVO(ClueRecordEntity clue, boolean includeReviewFields) {
        var data = new java.util.LinkedHashMap<String, Object>();
        data.put("clue_id", String.valueOf(clue.getId()));
        data.put("task_id", clue.getTaskId() == null ? null : String.valueOf(clue.getTaskId()));
        data.put("patient_id", clue.getPatientId() == null ? null : String.valueOf(clue.getPatientId()));
        data.put("tag_code", clue.getTagCode());
        data.put("source_type", clue.getSourceType());
        data.put("coord_system", "WGS84");
        data.put("location", Map.of("lat", clue.getLocationLat(), "lng", clue.getLocationLng()));
        data.put("description", clue.getDescription());
        data.put("photo_url", clue.getPhotoUrl());
        data.put("is_valid", Boolean.TRUE.equals(clue.getIsValid()));
        data.put("suspect_reason", clue.getSuspectReason());
        if (includeReviewFields) {
            data.put("review_status", clue.getReviewStatus());
            data.put("assignee_user_id", clue.getAssigneeUserId() == null ? null : String.valueOf(clue.getAssigneeUserId()));
        }
        data.put("override", Boolean.TRUE.equals(clue.getOverride()));
        data.put("override_reason", clue.getOverrideReason());
        data.put("rejected_by", clue.getRejectedBy() == null ? null : String.valueOf(clue.getRejectedBy()));
        data.put("reject_reason", clue.getRejectReason());
        data.put("reported_at", clue.getCreatedAt() == null ? null : clue.getCreatedAt().toString());
        data.put("reviewed_at", clue.getReviewedAt() == null ? null : clue.getReviewedAt().toString());
        return data;
    }

    private void requireAdmin() {
        if (!securityContext.isAdmin()) throw BizException.of("E_GOV_4030");
    }

    @Data
    public static class AssignRequest {
        @NotNull
        private Long assigneeUserId;
        @Size(max = 256)
        private String reason;
    }
}
