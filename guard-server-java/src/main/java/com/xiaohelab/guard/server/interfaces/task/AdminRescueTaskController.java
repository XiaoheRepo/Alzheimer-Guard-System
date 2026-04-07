package com.xiaohelab.guard.server.interfaces.task;

import com.xiaohelab.guard.server.common.exception.BizException;
import com.xiaohelab.guard.server.common.response.ApiResponse;
import com.xiaohelab.guard.server.common.response.PageResponse;
import com.xiaohelab.guard.server.infrastructure.persistence.do_.NotificationInboxDO;
import com.xiaohelab.guard.server.infrastructure.persistence.do_.RescueTaskDO;
import com.xiaohelab.guard.server.infrastructure.persistence.do_.SysLogDO;
import com.xiaohelab.guard.server.infrastructure.persistence.mapper.NotificationInboxMapper;
import com.xiaohelab.guard.server.infrastructure.persistence.mapper.RescueTaskMapper;
import com.xiaohelab.guard.server.infrastructure.persistence.mapper.SysLogMapper;
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
 */
@RestController
@RequiredArgsConstructor
public class AdminRescueTaskController {

    private final RescueTaskMapper rescueTaskMapper;
    private final SysLogMapper sysLogMapper;
    private final NotificationInboxMapper notificationInboxMapper;
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
        int offset = (pageNo - 1) * pageSize;
        List<RescueTaskDO> list = rescueTaskMapper.listAll(status, source, pageSize, offset);
        long total = rescueTaskMapper.countAll(status, source);

        List<Map<String, Object>> items = list.stream()
                .map(d -> Map.<String, Object>of(
                        "task_id", String.valueOf(d.getId()),
                        "patient_id", String.valueOf(d.getPatientId()),
                        "status", d.getStatus(),
                        "source", d.getSource() == null ? "" : d.getSource(),
                        "reported_by", d.getCreatedBy() == null ? "" : String.valueOf(d.getCreatedBy()),
                        "event_time", d.getUpdatedAt() == null ? "" : d.getUpdatedAt().toString()))
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
        RescueTaskDO task = rescueTaskMapper.findById(taskId);
        if (task == null) throw BizException.of("E_TASK_4041");

        var data = new LinkedHashMap<String, Object>();
        data.put("task_id", String.valueOf(task.getId()));
        data.put("patient_id", String.valueOf(task.getPatientId()));
        data.put("status", task.getStatus());
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
        RescueTaskDO task = rescueTaskMapper.findById(taskId);
        if (task == null) throw BizException.of("E_TASK_4041");

        // cursor-based: for simplicity use offset from cursor as integer (base64-opaque to client)
        int offset = 0;
        if (cursor != null && !cursor.isBlank()) {
            try {
                String decoded = new String(java.util.Base64.getDecoder().decode(cursor));
                offset = Integer.parseInt(decoded);
            } catch (Exception ignored) { /* invalid cursor treated as start */ }
        }

        List<SysLogDO> logs = sysLogMapper.listByObjectId(String.valueOf(taskId), pageSize, offset);
        long total = sysLogMapper.countByObjectId(String.valueOf(taskId));
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
        RescueTaskDO task = rescueTaskMapper.findById(taskId);
        if (task == null) throw BizException.of("E_TASK_4041");

        String retryJobId = "nrt_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        Instant now = Instant.now();

        // Determine accepted channels (default IN_APP + PUSH)
        List<String> channels = (req.getChannels() != null && !req.getChannels().isEmpty())
                ? req.getChannels().stream()
                    .filter(c -> "IN_APP".equals(c) || "PUSH".equals(c))
                    .toList()
                : List.of("IN_APP", "PUSH");

        // Write notification to notification_inbox for the task reporter
        if (task.getCreatedBy() != null) {
            NotificationInboxDO notification = new NotificationInboxDO();
            notification.setUserId(task.getCreatedBy());
            notification.setType("TASK_PROGRESS");
            notification.setTitle("任务通知补偿");
            notification.setContent("管理员已触发通知补偿重放：" + req.getReason());
            notification.setLevel("INFO");
            notification.setRelatedTaskId(taskId);
            notification.setRelatedPatientId(task.getPatientId());
            notification.setTraceId(traceId);
            notificationInboxMapper.insert(notification);
        }

        // Write audit log
        SysLogDO log = new SysLogDO();
        log.setModule("TASK");
        log.setAction("NOTIFY_RETRY");
        log.setObjectId(String.valueOf(taskId));
        log.setResult("SUCCESS");
        log.setRiskLevel("LOW");
        log.setOperatorUserId(securityContext.currentUserId());
        log.setOperatorUsername(securityContext.currentUsername());
        log.setActionSource("USER");
        log.setExecutionMode("MANUAL");
        log.setDetail("{\"reason\":\"" + req.getReason().replace("\"", "\\\"") + "\",\"retry_job_id\":\"" + retryJobId + "\"}");
        log.setRequestId(requestId);
        log.setTraceId(traceId);
        log.setExecutedAt(now);
        sysLogMapper.insert(log);

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

        RescueTaskDO task = rescueTaskMapper.findById(taskId);
        if (task == null) throw BizException.of("E_TASK_4041");
        if (!"ACTIVE".equals(task.getStatus())) throw BizException.of("E_TASK_4093");

        int updated = rescueTaskMapper.forceClose(taskId, "SUPER_FORCE_CLOSE", req.getReason());
        if (updated == 0) throw BizException.of("E_TASK_4093");

        // Reload
        RescueTaskDO updated2 = rescueTaskMapper.findById(taskId);

        // Write audit log (mandatory; failure triggers rollback via @Transactional)
        SysLogDO log = new SysLogDO();
        log.setModule("TASK");
        log.setAction("FORCE_CLOSE");
        log.setObjectId(String.valueOf(taskId));
        log.setResult("SUCCESS");
        log.setRiskLevel("HIGH");
        log.setOperatorUserId(securityContext.currentUserId());
        log.setOperatorUsername(securityContext.currentUsername());
        log.setActionSource("USER");
        log.setExecutionMode("MANUAL");
        log.setDetail("{\"reason\":\"" + req.getReason().replace("\"", "\\\"") + "\"}");
        log.setRequestId(requestId);
        log.setTraceId(traceId);
        log.setExecutedAt(Instant.now());
        sysLogMapper.insert(log);

        var data = new LinkedHashMap<String, Object>();
        data.put("task_id", String.valueOf(taskId));
        data.put("patient_id", String.valueOf(updated2.getPatientId()));
        data.put("status", updated2.getStatus());
        data.put("source", updated2.getSource());
        data.put("reported_by", updated2.getCreatedBy() == null ? null : String.valueOf(updated2.getCreatedBy()));
        data.put("remark", updated2.getRemark());
        data.put("close_reason", "SUPER_FORCE_CLOSE");
        data.put("start_time", updated2.getCreatedAt() == null ? null : updated2.getCreatedAt().toString());
        data.put("end_time", updated2.getClosedAt() == null ? null : updated2.getClosedAt().toString());
        data.put("ai_analysis_summary", updated2.getAiAnalysisSummary());
        data.put("poster_url", updated2.getPosterUrl());
        data.put("version", String.valueOf(updated2.getEventVersion()));
        data.put("event_time", updated2.getUpdatedAt() == null ? null : updated2.getUpdatedAt().toString());
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
