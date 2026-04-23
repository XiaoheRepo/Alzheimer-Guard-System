package com.xiaohelab.guard.server.rescue.controller;

import com.xiaohelab.guard.server.ai.entity.AiSessionEntity;
import com.xiaohelab.guard.server.ai.repository.AiSessionRepository;
import com.xiaohelab.guard.server.clue.entity.ClueRecordEntity;
import com.xiaohelab.guard.server.clue.repository.ClueRecordRepository;
import com.xiaohelab.guard.server.clue.service.PatientTrajectoryService;
import com.xiaohelab.guard.server.common.annotation.Idempotent;
import com.xiaohelab.guard.server.common.dto.PagedResponse;
import com.xiaohelab.guard.server.common.dto.Result;
import com.xiaohelab.guard.server.rescue.dto.TaskCloseRequest;
import com.xiaohelab.guard.server.rescue.dto.TaskCreateRequest;
import com.xiaohelab.guard.server.rescue.dto.TaskSnapshotResponse;
import com.xiaohelab.guard.server.rescue.dto.TrajectoryLatestResponse;
import com.xiaohelab.guard.server.rescue.entity.RescueTaskEntity;
import com.xiaohelab.guard.server.rescue.service.RescueTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 救援任务接口（API V2.0 §3.1）：
 * <ul>
 *     <li>3.1.1 POST /api/v1/rescue/tasks - 发布寻回任务</li>
 *     <li>3.1.2 POST /api/v1/rescue/tasks/{id}/close - 关闭任务</li>
 *     <li>3.1.3 GET /api/v1/rescue/tasks/{id}/snapshot - 任务快照聚合</li>
 *     <li>3.1.4 POST /api/v1/rescue/tasks/{id}/sustained - 标记长期维持</li>
 *     <li>3.1.5 GET /api/v1/rescue/tasks - 任务列表（Offset 分页）</li>
 *     <li>3.2.7 GET /api/v1/rescue/tasks/{id}/trajectory/latest - 轨迹查询（Cursor 分页）</li>
 * </ul>
 */
@Tag(name = "Rescue", description = "寻回任务（API §3.1 / §3.2.7）")
@RestController
@RequestMapping("/api/v1/rescue/tasks")
public class RescueTaskController {

    private final RescueTaskService taskService;
    private final PatientTrajectoryService trajectoryService;
    private final ClueRecordRepository clueRecordRepository;
    private final AiSessionRepository aiSessionRepository;

    public RescueTaskController(RescueTaskService taskService,
                                PatientTrajectoryService trajectoryService,
                                ClueRecordRepository clueRecordRepository,
                                AiSessionRepository aiSessionRepository) {
        this.taskService = taskService;
        this.trajectoryService = trajectoryService;
        this.clueRecordRepository = clueRecordRepository;
        this.aiSessionRepository = aiSessionRepository;
    }

    /** 3.1.1 发布寻回任务（FR-TASK-001）。 */
    @PostMapping
    @Idempotent
    @Operation(summary = "3.1.1 发布寻回任务")
    public Result<RescueTaskEntity> create(@Valid @RequestBody TaskCreateRequest req) {
        return Result.ok(taskService.create(req));
    }

    /** 3.1.3 任务快照（FR-TASK-003），聚合患者信息、线索、轨迹统计。 */
    @GetMapping("/{taskId}/snapshot")
    @Operation(summary = "3.1.3 任务快照聚合")
    public Result<TaskSnapshotResponse> snapshot(@PathVariable Long taskId) {
        return Result.ok(taskService.snapshot(taskId));
    }

    /** 3.1.3-兼容：直接按 ID 查询任务实体（内部调试用，推荐走 snapshot）。 */
    @GetMapping("/{taskId}")
    @Operation(summary = "按 ID 查询任务实体（兼容）")
    public Result<RescueTaskEntity> get(@PathVariable Long taskId) {
        return Result.ok(taskService.get(taskId));
    }

    /** 3.7.2 任务全景聚合（BFF）：task + patient + recent_clues + trajectory_summary + ai_session。 */
    @GetMapping("/{taskId}/full")
    @Operation(summary = "3.7.2 任务全景聚合（BFF）")
    public Result<Map<String, Object>> full(@PathVariable Long taskId) {
        TaskSnapshotResponse snap = taskService.snapshot(taskId);

        Map<String, Object> task = new HashMap<>();
        task.put("task_id", String.valueOf(snap.getTaskId()));
        task.put("task_no", snap.getTaskNo());
        task.put("status", snap.getStatus());
        task.put("source", snap.getSource());
        task.put("created_at", snap.getCreatedAt());

        Map<String, Object> patient = new HashMap<>();
        TaskSnapshotResponse.PatientSnapshot ps = snap.getPatientSnapshot();
        patient.put("patient_id", String.valueOf(snap.getPatientId()));
        if (ps != null) {
            patient.put("patient_name", ps.getPatientName());
            patient.put("gender", ps.getGender());
            patient.put("age", ps.getAge());
            patient.put("avatar_url", ps.getAvatarUrl());
            patient.put("short_code", ps.getShortCode());
            if (ps.getAppearance() != null) {
                Map<String, Object> ap = new HashMap<>();
                ap.put("clothing", ps.getAppearance().getClothing());
                ap.put("features", ps.getAppearance().getFeatures());
                patient.put("appearance", ap);
            }
        }

        List<Map<String, Object>> recentClues = new ArrayList<>();
        Page<ClueRecordEntity> cluePage = clueRecordRepository
                .findByTaskIdOrderByCreatedAtDesc(taskId, PageRequest.of(0, 10));
        for (ClueRecordEntity c : cluePage.getContent()) {
            Map<String, Object> item = new HashMap<>();
            item.put("clue_id", String.valueOf(c.getId()));
            item.put("clue_no", c.getClueNo());
            item.put("status", c.getStatus());
            item.put("latitude", c.getLatitude());
            item.put("longitude", c.getLongitude());
            item.put("created_at", c.getCreatedAt());
            recentClues.add(item);
        }

        Map<String, Object> trajectorySummary = new HashMap<>();
        if (snap.getTrajectorySummary() != null) {
            trajectorySummary.put("point_count", snap.getTrajectorySummary().getPointCount());
            trajectorySummary.put("latest_point_time", snap.getTrajectorySummary().getLatestPointTime());
        }

        Map<String, Object> aiSession = new HashMap<>();
        aiSessionRepository
                .findByPatientIdAndTaskIdAndStatus(snap.getPatientId(), taskId, "ACTIVE")
                .ifPresent(s -> {
                    aiSession.put("session_id", s.getSessionId());
                    aiSession.put("status", s.getStatus());
                    aiSession.put("message_count", messageCount(s));
                });

        Map<String, Object> data = new HashMap<>();
        data.put("task", task);
        data.put("patient", patient);
        data.put("recent_clues", recentClues);
        data.put("trajectory_summary", trajectorySummary);
        data.put("ai_session", aiSession);
        return Result.ok(data);
    }

    /** 粗略统计 AI 会话消息条数：messages 为 JSON 数组字符串。 */
    private int messageCount(AiSessionEntity s) {
        String m = s.getMessages();
        if (m == null || m.isBlank() || "[]".equals(m.trim())) return 0;
        int count = 0;
        int depth = 0;
        for (int i = 0; i < m.length(); i++) {
            char c = m.charAt(i);
            if (c == '{') {
                if (depth == 0) count++;
                depth++;
            } else if (c == '}') {
                depth--;
            }
        }
        return count;
    }

    /** 3.1.5 任务列表（Offset 分页 + 筛选）。 */
    @GetMapping
    @Operation(summary = "3.1.5 任务列表（Offset 分页）")
    public Result<PagedResponse<RescueTaskEntity>> list(
            @RequestParam(name = "patient_id", required = false) Long patientId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String source,
            @RequestParam(name = "page_no", defaultValue = "1") int pageNo,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize,
            @RequestParam(name = "sort_by", defaultValue = "created_at") String sortBy,
            @RequestParam(name = "sort_order", defaultValue = "desc") String sortOrder) {
        Page<RescueTaskEntity> page = taskService.listMine(
                patientId, status, source, pageNo, pageSize, sortBy, sortOrder);
        return Result.ok(PagedResponse.fromPage(page, pageNo, pageSize));
    }

    /** 3.1.2 关闭任务（FOUND / FALSE_ALARM）。 */
    @PostMapping("/{taskId}/close")
    @Idempotent
    @Operation(summary = "3.1.2 关闭任务")
    public Result<RescueTaskEntity> close(@PathVariable Long taskId,
                                          @Valid @RequestBody TaskCloseRequest req) {
        return Result.ok(taskService.close(taskId, req));
    }

    /** 3.1.4 标记为长期维持（ACTIVE → SUSTAINED）。 */
    @PostMapping({"/{taskId}/sustained", "/{taskId}/sustain"})
    @Idempotent
    @Operation(summary = "3.1.4 标记为长期维持（canonical: /sustained，/sustain 保留向后兼容）")
    public Result<RescueTaskEntity> sustain(@PathVariable Long taskId) {
        return Result.ok(taskService.sustain(taskId));
    }

    /** 3.2.7 任务轨迹最新切片（Cursor 分页 + 增量 version）。 */
    @GetMapping("/{taskId}/trajectory/latest")
    @Operation(summary = "3.2.7 任务关联轨迹查询（Cursor 分页）")
    public Result<TrajectoryLatestResponse> trajectoryLatest(
            @PathVariable Long taskId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime since,
            @RequestParam(name = "after_version", required = false) Long afterVersion,
            @RequestParam(name = "page_size", defaultValue = "50") int pageSize) {
        return Result.ok(trajectoryService.queryLatestForTask(taskId, since, afterVersion, pageSize));
    }
}
