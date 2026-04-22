package com.xiaohelab.guard.server.common.controller;

import com.xiaohelab.guard.server.clue.repository.ClueRecordRepository;
import com.xiaohelab.guard.server.common.dto.Result;
import com.xiaohelab.guard.server.common.security.AuthUser;
import com.xiaohelab.guard.server.common.security.SecurityUtil;
import com.xiaohelab.guard.server.common.util.DesensitizeUtil;
import com.xiaohelab.guard.server.notification.repository.NotificationInboxRepository;
import com.xiaohelab.guard.server.patient.entity.GuardianRelationEntity;
import com.xiaohelab.guard.server.patient.entity.PatientProfileEntity;
import com.xiaohelab.guard.server.patient.repository.GuardianRelationRepository;
import com.xiaohelab.guard.server.patient.repository.PatientProfileRepository;
import com.xiaohelab.guard.server.rescue.entity.RescueTaskEntity;
import com.xiaohelab.guard.server.rescue.repository.RescueTaskRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 首页仪表盘 BFF（API V2.0 §3.7.1）。
 * <p>一次请求聚合：患者数 / 活跃任务数 / 未读通知数 / 患者摘要列表（含活跃任务基础信息）。</p>
 */
@Tag(name = "BFF.Dashboard", description = "首页仪表盘聚合")
@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final GuardianRelationRepository guardianRelationRepository;
    private final PatientProfileRepository patientProfileRepository;
    private final RescueTaskRepository rescueTaskRepository;
    private final ClueRecordRepository clueRecordRepository;
    private final NotificationInboxRepository notificationInboxRepository;

    public DashboardController(GuardianRelationRepository guardianRelationRepository,
                                PatientProfileRepository patientProfileRepository,
                                RescueTaskRepository rescueTaskRepository,
                                ClueRecordRepository clueRecordRepository,
                                NotificationInboxRepository notificationInboxRepository) {
        this.guardianRelationRepository = guardianRelationRepository;
        this.patientProfileRepository = patientProfileRepository;
        this.rescueTaskRepository = rescueTaskRepository;
        this.clueRecordRepository = clueRecordRepository;
        this.notificationInboxRepository = notificationInboxRepository;
    }

    /**
     * §3.7.1 首页仪表盘。
     * <ol>
     *   <li>获取当前用户的所有有效监护关系，得到 patientId 列表。</li>
     *   <li>批量查询患者档案（逻辑未删除）。</li>
     *   <li>批量查询活跃/持续任务（IN 查询）。</li>
     *   <li>逐患者组装含任务摘要的 JSON 结构。</li>
     *   <li>查询未读通知数。</li>
     * </ol>
     */
    @GetMapping
    @Operation(summary = "§3.7.1 首页仪表盘（患者 / 任务 / 未读通知聚合）")
    public Result<Map<String, Object>> dashboard() {
        AuthUser user = SecurityUtil.current();

        // 1. 取监护患者 ID 列表
        List<Long> patientIds = guardianRelationRepository
                .findByUserIdAndRelationStatus(user.getUserId(), "ACTIVE")
                .stream()
                .map(GuardianRelationEntity::getPatientId)
                .collect(Collectors.toList());

        // 2. 批量取患者档案（仅逻辑未删除）
        List<PatientProfileEntity> patients = patientIds.isEmpty()
                ? List.of()
                : patientProfileRepository.findAllById(patientIds)
                        .stream()
                        .filter(p -> p.getDeletedAt() == null)
                        .collect(Collectors.toList());

        // 3. 批量取活跃任务（ACTIVE / SUSTAINED / CREATED）
        List<RescueTaskEntity> activeTasks = patientIds.isEmpty()
                ? List.of()
                : rescueTaskRepository.findActiveByPatients(patientIds);

        // 按 patientId 做 Map，一个患者可能有多个非关闭任务，取最新一条
        Map<Long, RescueTaskEntity> taskByPatient = activeTasks.stream()
                .collect(Collectors.toMap(
                        RescueTaskEntity::getPatientId,
                        t -> t,
                        (a, b) -> a.getId() > b.getId() ? a : b   // 取 id 较大（更新）的
                ));

        // 4. 未读通知数
        long unreadCount = notificationInboxRepository
                .countByUserIdAndReadStatus(user.getUserId(), "UNREAD");

        // 5. 组装患者摘要列表
        List<Map<String, Object>> patientSummaries = new ArrayList<>();
        for (PatientProfileEntity p : patients) {
            Map<String, Object> ps = new LinkedHashMap<>();
            ps.put("patient_id", String.valueOf(p.getId()));
            ps.put("patient_name", DesensitizeUtil.chineseName(p.getName()));
            ps.put("status", p.getLostStatus());
            ps.put("avatar_url", p.getAvatarUrl());
            ps.put("short_code", p.getShortCode());

            // 活跃任务摘要（可为 null）
            RescueTaskEntity task = taskByPatient.get(p.getId());
            if (task != null) {
                long clueCount = clueRecordRepository.countByTaskId(task.getId());
                Map<String, Object> ts = new LinkedHashMap<>();
                ts.put("task_id", String.valueOf(task.getId()));
                ts.put("task_no", task.getTaskNo());
                ts.put("status", task.getStatus());
                ts.put("source", task.getSource());
                ts.put("created_at", task.getCreatedAt());
                ts.put("clue_count", clueCount);
                ps.put("active_task", ts);
            } else {
                ps.put("active_task", null);
            }
            patientSummaries.add(ps);
        }

        // 6. 顶层汇总
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("patient_count", patients.size());
        data.put("active_task_count", taskByPatient.size());
        data.put("unread_notification_count", unreadCount);
        data.put("patients", patientSummaries);

        return Result.ok(data);
    }
}
