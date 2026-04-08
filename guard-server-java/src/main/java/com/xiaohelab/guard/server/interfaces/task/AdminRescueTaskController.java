package com.xiaohelab.guard.server.interfaces.task;

import com.xiaohelab.guard.server.application.governance.AuditLogService;
import com.xiaohelab.guard.server.application.notification.NotificationService;
import com.xiaohelab.guard.server.application.task.CloseRescueTaskUseCase;
import com.xiaohelab.guard.server.application.task.QueryRescueTaskService;
import com.xiaohelab.guard.server.common.exception.BizException;
import com.xiaohelab.guard.server.common.response.ApiResponse;
import com.xiaohelab.guard.server.common.response.PageResponse;
import com.xiaohelab.guard.server.domain.governance.entity.SysLogEntity;
import com.xiaohelab.guard.server.domain.task.entity.RescueTaskEntity;
import com.xiaohelab.guard.server.security.config.SecurityContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 管理端寻回任务接口（ADMIN / SUPERADMIN）。
 * 包含：任务列表、任务详情、审计轨迹、通知补偿重放、强制关闭。
 * 所有 Mapper 调用已移除，通过 application 层服务/use-case 访问基础设施。
 */
@RestController
@RequiredArgsConstructor
public class AdminRescueTaskController {

    private final QueryRescueTaskService queryRescueTaskService;
    private final CloseRescueTaskUseCase closeRescueTaskUseCase;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;
    private final SecurityContext securityContext;

    // 3.1.11 GET /api/v1/admin/rescue/tasks
    /** 管理端分页检索任务 */
    @GetMapping("/api/v1/admin/rescue/tasks")
    public ApiResponse<PageResponse<Map<String, Object>>> listAdminTasks(
            @RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String source,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId) {

        requireAdmin();
        List<RescueTaskEntity> list = queryRescueTaskService.listAll(status, source, pageNo, pageSize);
        long total = queryRescueTaskService.countAll(status, source);

        List<Map<String, Object>> items = list.stream()
                .map(e -> Map.<String, Object>of(
                        "task_id", String.valueOf(e.getId()),
                        "patient_id", String.valueOf(e.getPatientId()),
                        "status", e.getStatus().name(),
                        "source", e.getSource() == null ? "" : e.getSource(),
                        "reported_by", e.getCreatedBy() == null ? "" : String.valueOf(e.getCreatedBy()),
                        "event_time", e.getUpdatedAt() == null ? "" : e.getUpdatedAt().toString()))
                .toList();

        return ApiResponse.ok(PageResponse.<Map<String, Object>>builder()
                .items(items).pageNo(pageNo).pageSize(pageSize)
                .total(total).hasNext(total > (long) pageNo * pageSize)
                .build(), traceId);
    }

    // 3.1.12 GET /api/v1/admin/rescue/tasks/{taskId}
    /** 管理端读取任务详情 */
    @GetMapping("/api/v1/admin/rescue/tasks/{taskId}")
    public ApiResponse<Map<String, Object>> getAdminTask(
            @PathVariable Long taskId,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId) {

        requireAdmin();
        RescueTaskEntity task = queryRescueTaskService.findById(
                taskId, securityContext.currentUserId(), securityContext.currentRole());

        var data = new LinkedHashMap<String, Object>();
        data.put("task_id", String.valueOf(task.getId()));
        data.put("patient_id", String.valueOf(task.getPatientId()));
        data.put("status", task.getStatus().name());
        data.put("source", task.getSource());
        data.put("reported_by", task.getCreatedBy() == null ? null : String.valueOf(task.getCreatedBy()));
        data.put("remark", task.getRemark());
        data.put("close_reason", task.getCloseReason());
        data.put("start_time", task.getCreatedAt() == null ? null : task.getCreatedAt().toString());
        data.put("end_time", task.getClosedAt() == null ? null : task.getClosedAt().toString());
        data.put("ai_analysis_summary", task.getAiAnalysisSummary());
        data.put("poster_url", task.getPosterUrl());
        data.put("version", String.valueOf(task.getEventVersion()));
        data.put("event_time", task.getUpdatedAt() == null ? null : task.getUpdatedAt().toString());
        return ApiResponse.ok(data, traceId);
    }

    // 3.1.13 GET /api/v1/admin/rescue/tasks/{taskId}/audit
    /** 读取任务审计轨迹（游标分页） */
    @GetMapping("/api/v1/admin/rescue/tasks/{taskId}/audit")
    public ApiResponse<Map<String, Object>> getAuditTrail(
            @PathVariable Long taskId,
            @RequestParam(defaultValue = "50") int pageSize,
            @RequestParam(required = false) String cursor,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId) {

        requireAdmin();
        // validate task exists
        queryRescueTaskService.findById(taskId, securityContext.currentUserId(),
                securityContext.currentRole());

        int offset = 0;
        if (cursor != null && !cursor.isBlank()) {
            try {
                String decoded = new String(java.util.Base64.getDecoder().decode(cursor));
                offset = Integer.parseInt(decoded);
            } catch (Exception ignored) { /* invalid cursor treated as start */ }
        }

        List<SysLogEntity> logs = auditLogService.listByObjectId(String.valueOf(taskId), pageSize, offset);
        long total = auditLogService.countByObjectId(String.valueOf(taskId));
        boolean hasNext = (long)(offset + pageSize) < total;
        String nextCursor = hasNext
                ? java.util.Base64.getEncoder().encodeToString(String.valueOf(offset + pageSize).getBytes())
                : null;

        List<Map<String, Object>> items = logs.stream()
                .map(l -> Map.<String, Object>of(
                        "audit_id", String.valueOf(l.getId()),
                        "module", l.getModule() == null ? "" : l.getModule(),
                        "action", l.getAction() == null ? "" : l.getAction(),
                        "operator_user_id", l.getOperatorUserId() == null ? "" : String.valueOf(l.getOperatorUserId()),
                        "result", l.getResult() == null ? "" : l.getResult(),
                        "created_at", l.getCreatedAt() == null ? "" : l.getCreatedAt().toString()))
                .toList();

        return ApiResponse.ok(Map.of(
                "items", items,
                "page_size", pageSize,
                "next_cursor", (Object) nextCursor,
                "has_next", hasNext), traceId);
    }

    // 3.1.16 POST /api/v1/admin/rescue/tasks/{taskId}/notify/retry
    /** 通知补偿重放（写 notification_inbox + sys_log） */
    @PostMapping("/api/v1/admin/rescue/tasks/{taskId}/notify/retry")
    @Transactional
    public ApiResponse<Map<String, Object>> retryNotify(
            @PathVariable Long taskId,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            @Valid @RequestBody NotifyRetryRequest req) {

        requireAdmin();
        RescueTaskEntity task = queryRescueTaskService.findById(
                taskId, securityContext.currentUserId(), securityContext.currentRole());

        String retryJobId = "nrt_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        Instant now = Instant.now();

        List<String> channels = (req.getChannels() != null && !req.getChannels().isEmpty())
                ? req.getChannels().stream()
                    .filter(c -> "IN_APP".equals(c) || "PUSH".equals(c))
                    .toList()
                : List.of("IN_APP", "PUSH");

        if (task.getCreatedBy() != null) {
            notificationService.sendNotification(
                    task.getCreatedBy(), "TASK_PROGRESS", "任务通知补偿",
                    "管理员已触发通知补偿重放：" + req.getReason(), "INFO",
                    taskId, task.getPatientId(), traceId);
        }

        SysLogEntity log = SysLogEntity.create(
                "TASK", "NOTIFY_RETRY", null,
                String.valueOf(taskId), null, "SUCCESS",
                "LOW", securityContext.currentUserId(),
                securityContext.currentUsername(),
                "{\"reason\":\"" + req.getReason().replace("\"", "\\\"") + "\",\"retry_job_id\":\"" + retryJobId + "\"}",
                "USER", "MANUAL", requestId, traceId);
        auditLogService.writeLog(log);

        return ApiResponse.ok(Map.of(
                "task_id", String.valueOf(taskId),
                "accepted_channels", channels,
                "retry_job_id", retryJobId,
                "processed_at", now.toString()), traceId);
    }

    // 3.1.7 POST /api/v1/rescue/tasks/{taskId}/force-close  (SUPERADMIN only)
    /** 超级管理员强制关闭任务 */
    @PostMapping("/api/v1/rescue/tasks/{taskId}/force-close")
    @Transactional
    public ApiResponse<Map<String, Object>> forceCloseTask(
            @PathVariable Long taskId,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            @RequestHeader(value = "X-Action-Source", required = false) String actionSource,
            @Valid @RequestBody ForceCloseRequest req) {

        if (!securityContext.isSuperAdmin()) throw BizException.of("E_GOV_4032");
        if ("AI_AGENT".equals(actionSource)) throw BizException.of("E_GOV_4231");

        closeRescueTaskUseCase.forceCloseAdmin(taskId, req.getReason());

        RescueTaskEntity updated = queryRescueTaskService.findById(
                taskId, securityContext.currentUserId(), securityContext.currentRole());

        SysLogEntity forceCloseLog = SysLogEntity.create(
                "TASK", "FORCE_CLOSE", null,
                String.valueOf(taskId), null, "SUCCESS",
                "HIGH", securityContext.currentUserId(),
                securityContext.currentUsername(),
                "{\"reason\":\"" + req.getReason().replace("\"", "\\\"") + "\"}",
                "USER", "MANUAL", requestId, traceId);
        auditLogService.writeLog(forceCloseLog);

        var data = new LinkedHashMap<String, Object>();
        data.put("task_id", String.valueOf(taskId));
        data.put("patient_id", String.valueOf(updated.getPatientId()));
        data.put("status", updated.getStatus().name());
        data.put("source", updated.getSource());
        data.put("reported_by", updated.getCreatedBy() == null ? null : String.valueOf(updated.getCreatedBy()));
        data.put("remark", updated.getRemark());
        data.put("close_reason", "SUPER_FORCE_CLOSE");
        data.put("start_time", updated.getCreatedAt() == null ? null : updated.getCreatedAt().toString());
        data.put("end_time", updated.getClosedAt() == null ? null : updated.getClosedAt().toString());
        data.put("ai_analysis_summary", updated.getAiAnalysisSummary());
        data.put("poster_url", updated.getPosterUrl());
        data.put("version", String.valueOf(updated.getEventVersion()));
        data.put("event_time", updated.getUpdatedAt() == null ? null : updated.getUpdatedAt().toString());
        return ApiResponse.ok(data, traceId);
    }

    // ===== helpers =====

    private void requireAdmin() {
        if (!securityContext.isAdmin()) throw BizException.of("E_GOV_4030");
    }

    @Data
    public static class NotifyRetryRequest {
        @NotBlank
        @Size(min = 5, max = 256)
        private String reason;

        private List<String> channels;
    }

    @Data
    public static class ForceCloseRequest {
        @NotBlank
        @Size(min = 5, max = 256)
        private String reason;
    }
}
