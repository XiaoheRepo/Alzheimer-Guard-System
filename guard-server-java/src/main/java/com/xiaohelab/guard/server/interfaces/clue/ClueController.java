package com.xiaohelab.guard.server.interfaces.clue;

import com.xiaohelab.guard.server.application.clue.ReportClueUseCase;
import com.xiaohelab.guard.server.common.exception.BizException;
import com.xiaohelab.guard.server.common.response.ApiResponse;
import com.xiaohelab.guard.server.common.response.PageResponse;
import com.xiaohelab.guard.server.infrastructure.persistence.do_.ClueRecordDO;
import com.xiaohelab.guard.server.infrastructure.persistence.mapper.ClueRecordMapper;
import com.xiaohelab.guard.server.infrastructure.persistence.mapper.SysUserPatientMapper;
import com.xiaohelab.guard.server.security.config.SecurityContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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
    private final ClueRecordMapper clueRecordMapper;
    private final SysUserPatientMapper sysUserPatientMapper;
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
        ClueRecordDO clue = reportClueUseCase.execute(
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
        ClueRecordDO clue = reportClueUseCase.execute(
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
        ClueRecordDO sample = clueRecordMapper.listByTaskId(taskId, 1, 0).stream()
                .findFirst().orElse(null);
        if (sample != null) {
            boolean isAdmin = "ADMIN".equals(userRole) || "SUPER_ADMIN".equals(userRole);
            if (!isAdmin && sysUserPatientMapper.countActiveRelation(userId, sample.getPatientId()) == 0) {
                throw BizException.of("E_TASK_4030");
            }
        }

        List<ClueRecordDO> list = clueRecordMapper.listByTaskId(taskId, pageSize, (pageNo - 1) * pageSize);
        long total = clueRecordMapper.countByTaskId(taskId);

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

    // ===== 工具方法 =====

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
