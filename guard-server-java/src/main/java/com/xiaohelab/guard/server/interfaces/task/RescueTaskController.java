package com.xiaohelab.guard.server.interfaces.task;

import com.xiaohelab.guard.server.application.task.CloseRescueTaskUseCase;
import com.xiaohelab.guard.server.application.task.CreateRescueTaskUseCase;
import com.xiaohelab.guard.server.application.task.QueryRescueTaskService;
import com.xiaohelab.guard.server.common.response.ApiResponse;
import com.xiaohelab.guard.server.common.response.PageResponse;
import com.xiaohelab.guard.server.domain.task.RescueTaskEntity;
import com.xiaohelab.guard.server.domain.task.RescueTaskEntity.CloseType;
import com.xiaohelab.guard.server.infrastructure.persistence.do_.RescueTaskDO;
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
        return ApiResponse.ok(buildTaskVO(task), traceId);
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
                        "task_no", d.getTaskNo(),
                        "status", d.getStatus(),
                        "created_at", d.getCreatedAt() == null ? "" : d.getCreatedAt().toString()))
                .toList();

        return ApiResponse.ok(PageResponse.<Map<String, Object>>builder()
                .items(items).pageNo(pageNo).pageSize(pageSize)
                .total(total).hasNext(total > (long) pageNo * pageSize)
                .build(), traceId);
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
