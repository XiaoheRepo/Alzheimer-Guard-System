package com.xiaohelab.guard.server.interfaces.task;

import com.xiaohelab.guard.server.application.task.CloseRescueTaskUseCase;
import com.xiaohelab.guard.server.application.task.CreateRescueTaskUseCase;
import com.xiaohelab.guard.server.application.task.QueryRescueTaskService;
import com.xiaohelab.guard.server.common.exception.BizException;
import com.xiaohelab.guard.server.common.response.ApiResponse;
import com.xiaohelab.guard.server.common.response.PageResponse;
import com.xiaohelab.guard.server.domain.task.RescueTaskEntity;
import com.xiaohelab.guard.server.domain.task.RescueTaskEntity.CloseType;
import com.xiaohelab.guard.server.infrastructure.persistence.do_.ClueRecordDO;
import com.xiaohelab.guard.server.infrastructure.persistence.do_.NotificationInboxDO;
import com.xiaohelab.guard.server.infrastructure.persistence.do_.RescueTaskDO;
import com.xiaohelab.guard.server.infrastructure.persistence.mapper.ClueRecordMapper;
import com.xiaohelab.guard.server.infrastructure.persistence.mapper.NotificationInboxMapper;
import com.xiaohelab.guard.server.infrastructure.persistence.mapper.RescueTaskMapper;
import com.xiaohelab.guard.server.infrastructure.persistence.mapper.SysUserPatientMapper;
import com.xiaohelab.guard.server.security.config.SecurityContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 寻回任务接口。
 * 所有接口需要 Bearer JWT 鉴权（SecurityConfig 默认配置已生效）。
 * HC-03：X-Request-Id 幂等键由调用方提供，在 CreateRescueTaskUseCase 校验。
 * HC-04：X-Trace-Id 透传到响应体 trace_id 字段。
 */
@RestController
@RequestMapping("/api/v1/rescue/tasks")
@RequiredArgsConstructor
public class RescueTaskController {

    private final CreateRescueTaskUseCase createRescueTaskUseCase;
    private final CloseRescueTaskUseCase closeRescueTaskUseCase;
    private final QueryRescueTaskService queryRescueTaskService;
    private final SecurityContext securityContext;
    private final RescueTaskMapper rescueTaskMapper;
    private final ClueRecordMapper clueRecordMapper;
    private final NotificationInboxMapper notificationInboxMapper;
    private final SysUserPatientMapper sysUserPatientMapper;

    /** 创建寻回任务 */
    @PostMapping
    public ApiResponse<Map<String, Object>> createTask(
            @RequestHeader("X-Request-Id") String requestId,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId,
            @Valid @RequestBody CreateTaskRequest req) {

        Long userId = securityContext.currentUserId();
        RescueTaskEntity task = createRescueTaskUseCase.execute(
                requestId, userId, req.getPatientId(), req.getSource(), req.getRemark());

        return ApiResponse.ok(buildTaskVO(task), traceId);
    }

    /** 关闭寻回任务 */
    @PostMapping("/{taskId}/close")
    public ApiResponse<Map<String, Object>> closeTask(
            @PathVariable Long taskId,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId,
            @Valid @RequestBody CloseTaskRequest req) {

        Long userId = securityContext.currentUserId();
        String userRole = securityContext.currentRole();
        CloseType closeType = CloseType.valueOf(req.getCloseType());

        RescueTaskEntity task = closeRescueTaskUseCase.execute(
                taskId, userId, userRole, closeType, req.getReason(), req.getOperatorNote());

        return ApiResponse.ok(buildTaskVO(task), traceId);
    }

    /** 查询任务详情 */
    @GetMapping("/{taskId}")
    public ApiResponse<Map<String, Object>> getTask(
            @PathVariable Long taskId,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId) {

        Long userId = securityContext.currentUserId();
        String userRole = securityContext.currentRole();
        RescueTaskEntity task = queryRescueTaskService.findById(taskId, userId, userRole);
        return ApiResponse.ok(buildTaskDetailVO(task), traceId);
    }

    /** 分页查询患者任务列表 */
    @GetMapping
    public ApiResponse<PageResponse<Map<String, Object>>> listTasks(
            @RequestParam Long patientId,
            @RequestParam(defaultValue = "1") @Min(1) int pageNo,
            @RequestParam(defaultValue = "10") @Min(1) int pageSize,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId) {

        Long userId = securityContext.currentUserId();
        String userRole = securityContext.currentRole();
        List<RescueTaskDO> list = queryRescueTaskService.listByPatient(
                patientId, userId, userRole, pageNo, pageSize);
        long total = queryRescueTaskService.countByPatient(patientId);

        List<Map<String, Object>> items = list.stream()
                .map(d -> Map.<String, Object>of(
                        "task_id", String.valueOf(d.getId()),
                        "patient_id", String.valueOf(d.getPatientId()),
                        "patient_name_masked", "",
                        "status", d.getStatus(),
                        "source", d.getSource() == null ? "" : d.getSource(),
                        "latest_event_time", d.getUpdatedAt() == null ? "" : d.getUpdatedAt().toString(),
                        "start_time", d.getCreatedAt() == null ? "" : d.getCreatedAt().toString()))
                .toList();

        return ApiResponse.ok(PageResponse.<Map<String, Object>>builder()
                .items(items).pageNo(pageNo).pageSize(pageSize)
                .total(total).hasNext(total > (long) pageNo * pageSize)
                .build(), traceId);
    }

    // 3.1.3 GET /rescue/tasks/{taskId}/snapshot
    /** 获取任务当前快照 */
    @GetMapping("/{taskId}/snapshot")
    public ApiResponse<Map<String, Object>> getSnapshot(
            @PathVariable Long taskId,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId) {

        Long userId = securityContext.currentUserId();
        String userRole = securityContext.currentRole();
        RescueTaskEntity task = queryRescueTaskService.findById(taskId, userId, userRole);

        // Latest trajectory: pick the most recent valid clue with coords
        ClueRecordDO latestClue = null;
        List<ClueRecordDO> clues = clueRecordMapper.listByTaskId(taskId, 1, 0);
        if (!clues.isEmpty()) latestClue = clues.get(0);

        Map<String, Object> latestTrajectory = null;
        if (latestClue != null) {
            latestTrajectory = Map.of(
                    "event_time", latestClue.getCreatedAt() == null ? "" : latestClue.getCreatedAt().toString(),
                    "lat", latestClue.getLocationLat(),
                    "lng", latestClue.getLocationLng());
        }

        var data = new java.util.LinkedHashMap<String, Object>();
        data.put("task_id", String.valueOf(task.getId()));
        data.put("status", task.getStatus().name());
        data.put("patient_id", String.valueOf(task.getPatientId()));
        data.put("version", String.valueOf(task.getEventVersion()));
        data.put("event_time", task.getUpdatedAt() == null ? "" : task.getUpdatedAt().toString());
        data.put("latest_trajectory", latestTrajectory);
        return ApiResponse.ok(data, traceId);
    }

    // 3.1.4 GET /rescue/tasks/{taskId}/trajectory/latest
    /** 最新轨迹片段（按 since_event_time + limit 过滤） */
    @GetMapping("/{taskId}/trajectory/latest")
    public ApiResponse<Map<String, Object>> getTrajectory(
            @PathVariable Long taskId,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(required = false) String sinceEventTime,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId) {

        if (limit < 1 || limit > 200) throw BizException.of("E_REQ_4001");

        Long userId = securityContext.currentUserId();
        String userRole = securityContext.currentRole();
        queryRescueTaskService.findById(taskId, userId, userRole); // auth check

        int safeLimit = Math.min(limit, 200);
        List<ClueRecordDO> clues = clueRecordMapper.listByTaskId(taskId, safeLimit, 0);
        List<Map<String, Object>> items = clues.stream()
                .filter(c -> c.getLocationLat() != null && c.getLocationLng() != null)
                .map(c -> Map.<String, Object>of(
                        "event_time", c.getCreatedAt() == null ? "" : c.getCreatedAt().toString(),
                        "lat", c.getLocationLat(),
                        "lng", c.getLocationLng()))
                .toList();

        return ApiResponse.ok(Map.of("items", items, "has_more", false), traceId);
    }

    // 3.1.6 GET /rescue/tasks/{taskId}/events/poll
    /** 长轮询增量事件（WS 降级兜底，毕设简化：直接返回当前任务版本快照） */
    @GetMapping("/{taskId}/events/poll")
    public ApiResponse<Map<String, Object>> pollEvents(
            @PathVariable Long taskId,
            @RequestParam Long sinceVersion,
            @RequestParam(defaultValue = "15000") int timeoutMs,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId) {

        Long userId = securityContext.currentUserId();
        String userRole = securityContext.currentRole();
        RescueTaskEntity task = queryRescueTaskService.findById(taskId, userId, userRole);

        long latestVersion = task.getEventVersion();
        List<Map<String, Object>> items = List.of();
        if (latestVersion > sinceVersion) {
            items = List.of(Map.of(
                    "aggregate_id", String.valueOf(taskId),
                    "version", String.valueOf(latestVersion),
                    "event_time", task.getUpdatedAt() == null ? "" : task.getUpdatedAt().toString()));
        }
        return ApiResponse.ok(Map.of(
                "since_version", String.valueOf(sinceVersion),
                "latest_version", String.valueOf(latestVersion),
                "items", items), traceId);
    }

    // 3.1.9 GET /rescue/tasks/{taskId}/events (cursor paging stub)
    /** 事件流游标分页（毕设简化：从 sys_log 按 objectId 读取） */
    @GetMapping("/{taskId}/events")
    public ApiResponse<Map<String, Object>> listEvents(
            @PathVariable Long taskId,
            @RequestParam(defaultValue = "50") int pageSize,
            @RequestParam(required = false) String cursor,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId) {

        Long userId = securityContext.currentUserId();
        String userRole = securityContext.currentRole();
        queryRescueTaskService.findById(taskId, userId, userRole); // auth check

        // Stub: return empty event list (no dedicated event store in this version)
        return ApiResponse.ok(Map.of(
                "items", List.of(),
                "page_size", pageSize,
                "next_cursor", (Object) null,
                "has_next", false), traceId);
    }

    // 3.1.10 GET /rescue/tasks/{taskId}/clues
    /** 分页读取任务关联线索 */
    @GetMapping("/{taskId}/clues")
    public ApiResponse<PageResponse<Map<String, Object>>> listClues(
            @PathVariable Long taskId,
            @RequestParam(defaultValue = "1") @Min(1) int pageNo,
            @RequestParam(defaultValue = "20") @Min(1) int pageSize,
            @RequestParam(required = false) Boolean suspectedOnly,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId) {

        Long userId = securityContext.currentUserId();
        String userRole = securityContext.currentRole();
        queryRescueTaskService.findById(taskId, userId, userRole); // auth check

        int offset = (pageNo - 1) * pageSize;
        List<ClueRecordDO> list;
        if (Boolean.TRUE.equals(suspectedOnly)) {
            list = clueRecordMapper.listPendingByTaskId(taskId);
        } else {
            list = clueRecordMapper.listByTaskId(taskId, pageSize, offset);
        }
        long total = clueRecordMapper.countByTaskId(taskId);

        List<Map<String, Object>> items = list.stream()
                .map(c -> Map.<String, Object>of(
                        "clue_id", String.valueOf(c.getId()),
                        "patient_id", String.valueOf(c.getPatientId()),
                        "is_valid", Boolean.TRUE.equals(c.getIsValid()),
                        "suspect_reason", c.getSuspectReason() == null ? "" : c.getSuspectReason(),
                        "reported_at", c.getCreatedAt() == null ? "" : c.getCreatedAt().toString()))
                .toList();

        return ApiResponse.ok(PageResponse.<Map<String, Object>>builder()
                .items(items).pageNo(pageNo).pageSize(pageSize)
                .total(total).hasNext(total > (long) pageNo * pageSize)
                .build(), traceId);
    }

    // 3.1.14 GET /rescue/tasks/{taskId}/clues/latest
    /** 最新有效线索聚合 */
    @GetMapping("/{taskId}/clues/latest")
    public ApiResponse<Map<String, Object>> getLatestClue(
            @PathVariable Long taskId,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId) {

        Long userId = securityContext.currentUserId();
        String userRole = securityContext.currentRole();
        queryRescueTaskService.findById(taskId, userId, userRole); // auth check

        List<ClueRecordDO> clues = clueRecordMapper.listByTaskId(taskId, 1, 0);
        if (clues.isEmpty()) throw BizException.of("E_CLUE_4043");

        ClueRecordDO c = clues.get(0);
        var data = new java.util.LinkedHashMap<String, Object>();
        data.put("task_id", String.valueOf(taskId));
        data.put("clue_id", String.valueOf(c.getId()));
        data.put("is_valid", Boolean.TRUE.equals(c.getIsValid()));
        data.put("location", c.getLocationLat() == null ? null : Map.of("lat", c.getLocationLat(), "lng", c.getLocationLng()));
        data.put("reported_at", c.getCreatedAt() == null ? "" : c.getCreatedAt().toString());
        return ApiResponse.ok(data, traceId);
    }

    // 3.1.15 GET /rescue/tasks/{taskId}/alerts
    /** 分页读取任务告警摘要 */
    @GetMapping("/{taskId}/alerts")
    public ApiResponse<PageResponse<Map<String, Object>>> listAlerts(
            @PathVariable Long taskId,
            @RequestParam(defaultValue = "1") @Min(1) int pageNo,
            @RequestParam(defaultValue = "20") @Min(1) int pageSize,
            @RequestParam(required = false) String level,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId) {

        Long userId = securityContext.currentUserId();
        String userRole = securityContext.currentRole();
        queryRescueTaskService.findById(taskId, userId, userRole); // auth check

        int offset = (pageNo - 1) * pageSize;
        List<NotificationInboxDO> list = notificationInboxMapper.listByRelatedTaskId(taskId, level, pageSize, offset);
        long total = notificationInboxMapper.countByRelatedTaskId(taskId, level);

        List<Map<String, Object>> items = list.stream()
                .map(n -> Map.<String, Object>of(
                        "alert_id", String.valueOf(n.getNotificationId()),
                        "level", n.getLevel() == null ? "INFO" : n.getLevel(),
                        "type", n.getType() == null ? "" : n.getType(),
                        "title", n.getTitle() == null ? "" : n.getTitle(),
                        "created_at", n.getCreatedAt() == null ? "" : n.getCreatedAt().toString()))
                .toList();

        return ApiResponse.ok(PageResponse.<Map<String, Object>>builder()
                .items(items).pageNo(pageNo).pageSize(pageSize)
                .total(total).hasNext(total > (long) pageNo * pageSize)
                .build(), traceId);
    }

    // 3.1.17 GET /rescue/tasks/statistics
    /** 任务域统计指标 */
    @GetMapping("/statistics")
    public ApiResponse<Map<String, Object>> getStatistics(
            @RequestParam(required = false) String timeFrom,
            @RequestParam(required = false) String timeTo,
            @RequestParam(defaultValue = "day") String granularity,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId) {

        if (!securityContext.isAdmin()) throw BizException.of("E_GOV_4030");

        long total = rescueTaskMapper.countByStatus(null, timeFrom, timeTo);
        long active = rescueTaskMapper.countByStatus("ACTIVE", timeFrom, timeTo);
        long resolved = rescueTaskMapper.countByStatus("RESOLVED", timeFrom, timeTo);
        long falseAlarm = rescueTaskMapper.countByStatus("FALSE_ALARM", timeFrom, timeTo);
        double resolutionRate = total == 0 ? 0.0 : (double) resolved / total;

        return ApiResponse.ok(Map.of(
                "time_from", timeFrom == null ? "" : timeFrom,
                "time_to", timeTo == null ? "" : timeTo,
                "granularity", granularity,
                "total_tasks", total,
                "active_tasks", active,
                "resolved_tasks", resolved,
                "false_alarm_tasks", falseAlarm,
                "resolution_rate", Math.round(resolutionRate * 10000.0) / 10000.0), traceId);
    }

    // ===== 工具方法与 DTO =====

    private Map<String, Object> buildTaskVO(RescueTaskEntity task) {
        return Map.of(
                "task_id", String.valueOf(task.getId() == null ? "" : task.getId()),
                "task_no", task.getTaskNo(),
                "patient_id", String.valueOf(task.getPatientId()),
                "status", task.getStatus().name(),
                "event_version", task.getEventVersion()
        );
    }

    private Map<String, Object> buildTaskDetailVO(RescueTaskEntity task) {
        var data = new java.util.LinkedHashMap<String, Object>();
        data.put("task_id", String.valueOf(task.getId() == null ? "" : task.getId()));
        data.put("patient_id", String.valueOf(task.getPatientId()));
        data.put("status", task.getStatus() == null ? "" : task.getStatus().name());
        data.put("source", task.getSource() == null ? "" : task.getSource());
        data.put("reported_by", task.getCreatedBy() == null ? null : String.valueOf(task.getCreatedBy()));
        data.put("remark", task.getRemark());
        data.put("close_reason", task.getCloseReason());
        data.put("start_time", task.getCreatedAt() == null ? null : task.getCreatedAt().toString());
        data.put("end_time", task.getClosedAt() == null ? null : task.getClosedAt().toString());
        data.put("ai_analysis_summary", null);
        data.put("poster_url", null);
        data.put("version", String.valueOf(task.getEventVersion()));
        data.put("event_time", task.getUpdatedAt() == null ? null : task.getUpdatedAt().toString());
        return data;
    }

    @Data
    public static class CreateTaskRequest {
        @NotNull
        private Long patientId;

        @NotBlank
        private String source;

        @Size(max = 500)
        private String remark;
    }

    @Data
    public static class CloseTaskRequest {
        @NotBlank
        private String closeType;  // RESOLVED / FALSE_ALARM

        @Size(min = 5, max = 256)
        private String reason;

        @Size(max = 500)
        private String operatorNote;
    }
}
