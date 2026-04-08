package com.xiaohelab.guard.server.interfaces.clue;

import com.xiaohelab.guard.server.application.clue.ClueService;
import com.xiaohelab.guard.server.application.clue.ReportClueUseCase;
import com.xiaohelab.guard.server.application.guardian.GuardianInvitationService;
import com.xiaohelab.guard.server.common.exception.BizException;
import com.xiaohelab.guard.server.common.response.ApiResponse;
import com.xiaohelab.guard.server.common.response.PageResponse;
import com.xiaohelab.guard.server.domain.clue.entity.ClueRecordEntity;
import com.xiaohelab.guard.server.infrastructure.persistence.do_.SysLogDO;
import com.xiaohelab.guard.server.security.config.SecurityContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 线索接口（匿名上报 + 登录用户查询）。
 * POST /api/v1/public/clues/report — 匿名接口（SecurityConfig 已配置为公开）
 * GET  /api/v1/clues — 需要鉴权
 * GET  /api/v1/clues/{clueId} — 需要鉴权
 */
@RestController
@RequiredArgsConstructor
public class ClueController {

    private final ReportClueUseCase reportClueUseCase;
    private final ClueService clueService;
    private final GuardianInvitationService guardianInvitationService;
    private final SecurityContext securityContext;

    /**
     * 匿名线索上报（HC-06：不依赖短信，通过 entry_token 或 manual_entry_token 上报）。
     * 公开路由，无需 Bearer token。
     */
    @PostMapping("/api/v1/public/clues/report")
    public ApiResponse<Map<String, Object>> reportClue(
            @RequestHeader("X-Request-Id") String requestId,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId,
            @Valid @RequestBody ReportClueRequest req) {

        // 匿名上报：提交者 ID 为 null
        ClueRecordEntity clue = reportClueUseCase.execute(
                requestId, req.getTaskId(), null,
                buildAnonymousSnapshot(req),
                req.getLocationLat(), req.getLocationLng(),
                req.getDescription(), null);

        return ApiResponse.ok(Map.of(
                "clue_no", clue.getClueNo(),
                "review_status", clue.getReviewStatus()
        ), traceId);
    }

    /**
     * 登录用户上报线索（附带身份信息）。
     */
    @PostMapping("/api/v1/clues/report")
    public ApiResponse<Map<String, Object>> reportClueAuth(
            @RequestHeader("X-Request-Id") String requestId,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId,
            @Valid @RequestBody ReportClueRequest req) {

        Long userId = securityContext.currentUserId();
        ClueRecordEntity clue = reportClueUseCase.execute(
                requestId, req.getTaskId(), userId,
                null,   // 登录用户快照由服务端从数据库查
                req.getLocationLat(), req.getLocationLng(),
                req.getDescription(), null);

        return ApiResponse.ok(Map.of(
                "clue_no", clue.getClueNo(),
                "review_status", clue.getReviewStatus()
        ), traceId);
    }

    /** 分页查询指定任务的线索列表（需归属权） */
    @GetMapping("/api/v1/clues")
    public ApiResponse<PageResponse<Map<String, Object>>> listClues(
            @RequestParam Long taskId,
            @RequestParam(defaultValue = "1") @Min(1) int pageNo,
            @RequestParam(defaultValue = "10") @Min(1) int pageSize,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId) {

        Long userId = securityContext.currentUserId();
        String userRole = securityContext.currentRole();

        // 查询任务归属验证（通过 patient_id 校验）
        Optional<ClueRecordEntity> sample = clueService.firstByTask(taskId);
        if (sample.isPresent()) {
            boolean isAdmin = "ADMIN".equals(userRole) || "SUPER_ADMIN".equals(userRole);
            if (!isAdmin && !guardianInvitationService.hasActiveRelation(userId, sample.get().getPatientId())) {
                throw BizException.of("E_TASK_4030");
            }
        }

        List<ClueRecordEntity> list = clueService.listByTask(taskId, pageSize, (pageNo - 1) * pageSize);
        long total = clueService.countByTask(taskId);

        List<Map<String, Object>> items = list.stream().map(c -> Map.<String, Object>of(
                "clue_id", String.valueOf(c.getId()),
                "clue_no", c.getClueNo(),
                "review_status", c.getReviewStatus(),
                "risk_score", c.getRiskScore(),
                "location_lat", c.getLocationLat(),
                "location_lng", c.getLocationLng(),
                "created_at", c.getCreatedAt() == null ? "" : c.getCreatedAt().toString()
        )).toList();

        return ApiResponse.ok(PageResponse.<Map<String, Object>>builder()
                .items(items).pageNo(pageNo).pageSize(pageSize)
                .total(total).hasNext(total > (long) pageNo * pageSize)
                .build(), traceId);
    }

    /** 3.2.8 GET /api/v1/clues/{clueId} — 线索完整详情 */
    @GetMapping("/api/v1/clues/{clueId}")
    public ApiResponse<Map<String, Object>> getClueDetail(
            @PathVariable Long clueId,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId) {

        ClueRecordEntity clue = clueService.getById(clueId); // throws E_CLUE_4043 if absent

        String userRole = securityContext.currentRole();
        boolean isAdmin = "ADMIN".equals(userRole) || "SUPERADMIN".equals(userRole)
                || "SUPER_ADMIN".equals(userRole);
        if (!isAdmin && clue.getPatientId() != null) {
            if (!guardianInvitationService.hasActiveRelation(securityContext.currentUserId(), clue.getPatientId())) {
                throw BizException.of("E_GOV_4030");
            }
        }

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
        data.put("override", Boolean.TRUE.equals(clue.getOverride()));
        data.put("override_reason", clue.getOverrideReason());
        data.put("rejected_by", clue.getRejectedBy() == null ? null : String.valueOf(clue.getRejectedBy()));
        data.put("reject_reason", clue.getRejectReason());
        data.put("reported_at", clue.getCreatedAt() == null ? null : clue.getCreatedAt().toString());
        data.put("reviewed_at", clue.getReviewedAt() == null ? null : clue.getReviewedAt().toString());
        return ApiResponse.ok(data, traceId);
    }

    /** 3.2.11 GET /api/v1/clues/{clueId}/timeline — 线索处理轨迹（游标分页，来自 sys_log） */
    @GetMapping("/api/v1/clues/{clueId}/timeline")
    public ApiResponse<Map<String, Object>> getClueTimeline(
            @PathVariable Long clueId,
            @RequestParam(defaultValue = "50") int pageSize,
            @RequestParam(required = false) String cursor,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId) {

        ClueRecordEntity clue = clueService.getById(clueId); // throws E_CLUE_4043 if absent

        String userRole = securityContext.currentRole();
        boolean isAdmin = "ADMIN".equals(userRole) || "SUPERADMIN".equals(userRole)
                || "SUPER_ADMIN".equals(userRole);
        if (!isAdmin && clue.getPatientId() != null) {
            if (!guardianInvitationService.hasActiveRelation(securityContext.currentUserId(), clue.getPatientId())) {
                throw BizException.of("E_PRO_4030");
            }
        }

        int offset = 0;
        if (cursor != null && !cursor.isBlank()) {
            try {
                offset = Integer.parseInt(new String(java.util.Base64.getDecoder().decode(cursor)));
            } catch (Exception ignored) {}
        }

        List<SysLogDO> logs = clueService.listTimeline(clueId, pageSize, offset);
        long total = clueService.countTimeline(clueId);
        boolean hasNext = (long)(offset + pageSize) < total;
        String nextCursor = hasNext
                ? java.util.Base64.getEncoder().encodeToString(String.valueOf(offset + pageSize).getBytes())
                : null;

        List<Map<String, Object>> items = logs.stream()
                .map(l -> Map.<String, Object>of(
                        "timeline_id", String.valueOf(l.getId()),
                        "action", l.getAction() == null ? "" : l.getAction(),
                        "operator_user_id", l.getOperatorUserId() == null ? "" : String.valueOf(l.getOperatorUserId()),
                        "remark", l.getDetail() == null ? "" : l.getDetail(),
                        "created_at", l.getCreatedAt() == null ? "" : l.getCreatedAt().toString()))
                .toList();

        return ApiResponse.ok(Map.of(
                "items", items, "page_size", pageSize,
                "next_cursor", (Object) nextCursor, "has_next", hasNext), traceId);
    }

    private String buildAnonymousSnapshot(ReportClueRequest req) {
        return String.format("{\"contact\":\"%s\",\"nickname\":\"%s\"}",
                req.getContact() == null ? "" : req.getContact().replace("\"", "\\\""),
                req.getNickname() == null ? "匿名" : req.getNickname().replace("\"", "\\\""));
    }

    // ===== DTO =====

    @Data
    public static class ReportClueRequest {
        @NotNull
        private Long taskId;

        @DecimalMin("-90.0") @DecimalMax("90.0")
        private double locationLat;

        @DecimalMin("-180.0") @DecimalMax("180.0")
        private double locationLng;

        @Size(max = 500)
        private String description;

        /** 匿名上报可选联系方式 */
        @Size(max = 64)
        private String contact;

        /** 匿名昵称 */
        @Size(max = 32)
        private String nickname;
    }
}
