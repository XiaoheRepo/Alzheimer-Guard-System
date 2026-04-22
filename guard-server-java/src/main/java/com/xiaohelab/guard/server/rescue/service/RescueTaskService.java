package com.xiaohelab.guard.server.rescue.service;

import com.xiaohelab.guard.server.clue.repository.ClueRecordRepository;
import com.xiaohelab.guard.server.clue.repository.PatientTrajectoryRepository;
import com.xiaohelab.guard.server.common.error.ErrorCode;
import com.xiaohelab.guard.server.common.event.OutboxTopics;
import com.xiaohelab.guard.server.common.exception.BizException;
import com.xiaohelab.guard.server.common.security.AuthUser;
import com.xiaohelab.guard.server.common.security.SecurityUtil;
import com.xiaohelab.guard.server.common.util.BusinessNoUtil;
import com.xiaohelab.guard.server.common.util.DesensitizeUtil;
import com.xiaohelab.guard.server.outbox.service.OutboxService;
import com.xiaohelab.guard.server.patient.entity.PatientProfileEntity;
import com.xiaohelab.guard.server.patient.repository.PatientProfileRepository;
import com.xiaohelab.guard.server.patient.service.GuardianAuthorizationService;
import com.xiaohelab.guard.server.rescue.dto.TaskCloseRequest;
import com.xiaohelab.guard.server.rescue.dto.TaskCreateRequest;
import com.xiaohelab.guard.server.rescue.dto.TaskSnapshotResponse;
import com.xiaohelab.guard.server.rescue.entity.RescueTaskEntity;
import com.xiaohelab.guard.server.rescue.repository.RescueTaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.Period;
import java.util.List;
import java.util.Map;

/** 救援任务服务：创建 / 状态流转 / 关闭。 */
@Service
public class RescueTaskService {

    private static final Logger log = LoggerFactory.getLogger(RescueTaskService.class);

    private final RescueTaskRepository taskRepository;
    private final PatientProfileRepository patientRepository;
    private final GuardianAuthorizationService authorizationService;
    private final OutboxService outboxService;
    private final ClueRecordRepository clueRecordRepository;
    private final PatientTrajectoryRepository trajectoryRepository;

    public RescueTaskService(RescueTaskRepository taskRepository,
                             PatientProfileRepository patientRepository,
                             GuardianAuthorizationService authorizationService,
                             OutboxService outboxService,
                             ClueRecordRepository clueRecordRepository,
                             PatientTrajectoryRepository trajectoryRepository) {
        this.taskRepository = taskRepository;
        this.patientRepository = patientRepository;
        this.authorizationService = authorizationService;
        this.outboxService = outboxService;
        this.clueRecordRepository = clueRecordRepository;
        this.trajectoryRepository = trajectoryRepository;
    }

    /** 创建协同寻回任务：同患者仅允许一条活跃任务（唯一部分索引兜底）。 */
    @Transactional(rollbackFor = Exception.class)
    public RescueTaskEntity create(TaskCreateRequest req) {
        AuthUser user = SecurityUtil.current();
        PatientProfileEntity patient = authorizationService.assertGuardian(user, req.getPatientId());
        // 1. 双重保险：先查，再让数据库 partial unique index 最终兜底（HC-04）
        if (taskRepository.findActiveByPatient(patient.getId()).isPresent()) {
            throw BizException.of(ErrorCode.E_TASK_4091);
        }
        RescueTaskEntity t = new RescueTaskEntity();
        t.setTaskNo(BusinessNoUtil.taskNo());
        t.setPatientId(patient.getId());
        t.setStatus("ACTIVE");
        t.setSource(req.getSource());
        t.setRemark(req.getRemark());
        t.setDailyAppearance(req.getDailyAppearance());
        t.setDailyPhotoUrl(req.getDailyPhotoUrl());
        t.setEventVersion(1L);
        t.setCreatedBy(user.getUserId());
        try {
            taskRepository.saveAndFlush(t);
        } catch (DataIntegrityViolationException e) {
            throw BizException.of(ErrorCode.E_TASK_4091);
        }

        // 2. 患者 lost_status: MISSING_PENDING -> MISSING
        if (!"MISSING".equals(patient.getLostStatus())) {
            patient.setLostStatus("MISSING");
            patient.setLostStatusEventTime(OffsetDateTime.now());
            patientRepository.save(patient);
            outboxService.publish(OutboxTopics.PATIENT_MISSING_PENDING, String.valueOf(patient.getId()),
                    String.valueOf(patient.getId()),
                    Map.of("patient_id", patient.getId(), "task_id", t.getId()));
        }

        outboxService.publish(OutboxTopics.TASK_CREATED, t.getTaskNo(), String.valueOf(patient.getId()),
                Map.of("task_id", t.getId(), "task_no", t.getTaskNo(), "patient_id", patient.getId(),
                        "status", t.getStatus(), "source", t.getSource(), "created_by", user.getUserId()));
        log.info("[Task] created taskNo={} patientId={} by={}", t.getTaskNo(), patient.getId(), user.getUserId());
        return t;
    }

    /**
     * 查询单个任务。
     * @param taskId 任务主键
     * @return 任务实体
     * @throws BizException E_TASK_4041 任务不存在；越权访问时抛 E_PRO_4033
     */
    public RescueTaskEntity get(Long taskId) {
        AuthUser user = SecurityUtil.current();
        RescueTaskEntity t = taskRepository.findById(taskId)
                .orElseThrow(() -> BizException.of(ErrorCode.E_TASK_4041));
        authorizationService.assertGuardian(user, t.getPatientId());
        return t;
    }

    /**
     * 分页列出当前登录用户所有有权查看的任务。
     * @param page 页码（0-based）
     * @param size 每页大小
     * @return 分页结果；若无可访问患者返回空页
     */
    public Page<RescueTaskEntity> listMine(int page, int size) {
        AuthUser user = SecurityUtil.current();
        List<Long> patientIds = authorizationService.listAccessiblePatientIds(user.getUserId());
        if (patientIds.isEmpty()) return Page.empty();
        return taskRepository.findByPatientIdInOrderByCreatedAtDesc(patientIds, PageRequest.of(page, size));
    }

    /**
     * 任务列表查询（API §3.1.5）：支持 patient_id / status / source 过滤 + 排序字段选择。
     * @param patientId  指定患者（可选）
     * @param status     指定状态（可选）
     * @param source     指定来源（可选）
     * @param pageNo     页码（1-based，API 契约）
     * @param pageSize   每页条数（≤ 100）
     * @param sortBy     排序字段：created_at / closed_at
     * @param sortOrder  asc / desc
     */
    public Page<RescueTaskEntity> listMine(Long patientId, String status, String source,
                                           int pageNo, int pageSize,
                                           String sortBy, String sortOrder) {
        AuthUser user = SecurityUtil.current();
        List<Long> patientIds = authorizationService.listAccessiblePatientIds(user.getUserId());
        if (patientIds.isEmpty()) return Page.empty();

        // 1. 白名单化排序字段，防止 SQL 注入
        String sortField = "closed_at".equals(sortBy) ? "closedAt" : "createdAt";
        Sort sort = "asc".equalsIgnoreCase(sortOrder)
                ? Sort.by(sortField).ascending()
                : Sort.by(sortField).descending();

        // 2. 规范分页参数（API 1-based → Spring 0-based）
        int safeSize = Math.min(Math.max(pageSize, 1), 100);
        int safePage = Math.max(pageNo, 1) - 1;

        // 3. 带多条件 JPQL 查询
        return taskRepository.searchMine(patientIds, patientId, status, source,
                PageRequest.of(safePage, safeSize, sort));
    }

    /**
     * 任务快照聚合（API §3.1.3）：任务主信息 + 患者外观 + 线索统计 + 轨迹统计。
     * <p>脱敏规则：patient_name → {@link DesensitizeUtil#chineseName(String)}。</p>
     * @param taskId 目标任务 ID
     * @throws BizException E_TASK_4041 / E_TASK_4030 / E_PRO_4033
     */
    public TaskSnapshotResponse snapshot(Long taskId) {
        AuthUser user = SecurityUtil.current();
        RescueTaskEntity t = taskRepository.findById(taskId)
                .orElseThrow(() -> BizException.of(ErrorCode.E_TASK_4041));
        authorizationService.assertGuardian(user, t.getPatientId());

        TaskSnapshotResponse r = new TaskSnapshotResponse();
        r.setTaskId(t.getId());
        r.setTaskNo(t.getTaskNo());
        r.setPatientId(t.getPatientId());
        r.setStatus(t.getStatus());
        r.setSource(t.getSource());
        r.setReportedBy(t.getCreatedBy());
        r.setRemark(t.getRemark());
        r.setCreatedAt(t.getCreatedAt());
        r.setClosedAt(t.getClosedAt());
        r.setCloseType(t.getCloseType());
        r.setCloseReason(t.getCloseReason());
        r.setSustainedAt(t.getSustainedAt());
        r.setVersion(t.getEventVersion());

        // 1. 患者快照（脱敏姓名 + 外观特征）
        patientRepository.findById(t.getPatientId()).ifPresent(p -> {
            TaskSnapshotResponse.PatientSnapshot ps = new TaskSnapshotResponse.PatientSnapshot();
            ps.setPatientName(DesensitizeUtil.chineseName(p.getName()));
            ps.setGender(p.getGender());
            ps.setAvatarUrl(p.getAvatarUrl());
            ps.setShortCode(p.getShortCode());
            if (p.getBirthday() != null) {
                try {
                    ps.setAge(Period.between(p.getBirthday(), LocalDate.now()).getYears());
                } catch (Exception ignored) { /* 生日格式异常仅忽略 age，不影响主流程 */ }
            }
            TaskSnapshotResponse.Appearance ap = new TaskSnapshotResponse.Appearance();
            ap.setHeightCm(p.getAppearanceHeightCm());
            ap.setWeightKg(p.getAppearanceWeightKg());
            // 任务当日着装优先取任务上的 dailyAppearance/dailyPhotoUrl，否则回退到档案
            ap.setClothing(t.getDailyAppearance() != null ? t.getDailyAppearance() : p.getAppearanceClothing());
            ap.setFeatures(p.getAppearanceFeatures());
            ps.setAppearance(ap);
            r.setPatientSnapshot(ps);
        });

        // 2. 线索统计
        TaskSnapshotResponse.ClueSummary cs = new TaskSnapshotResponse.ClueSummary();
        cs.setTotalClueCount(clueRecordRepository.countByTaskId(taskId));
        cs.setValidClueCount(clueRecordRepository.countByTaskIdAndReviewStatus(taskId, "VALID"));
        cs.setSuspectClueCount(clueRecordRepository.countByTaskIdAndSuspectFlag(taskId, Boolean.TRUE));
        clueRecordRepository.findLatestClueTimeByTask(taskId).ifPresent(cs::setLatestClueTime);
        r.setClueSummary(cs);

        // 3. 轨迹统计（bounding_box）
        TaskSnapshotResponse.TrajectorySummary ts = new TaskSnapshotResponse.TrajectorySummary();
        ts.setPointCount(trajectoryRepository.countByTaskId(taskId));
        trajectoryRepository.findTopByTaskIdOrderByIdDesc(taskId).ifPresent(last -> {
            ts.setLatestPointTime(last.getWindowEnd());
        });
        Object[] agg = trajectoryRepository.aggBoundingBoxByTask(taskId);
        if (agg != null && agg.length == 4 && agg[0] != null) {
            // JPA 原生 min/max 返回会被包装为 Object[]，元素类型为具体数值类型
            try {
                Object[] row = agg;
                // 某些 JPA 实现会返回 Object[]{Object[]}，此处做兼容处理
                if (row.length == 1 && row[0] instanceof Object[] inner) row = inner;
                TaskSnapshotResponse.BoundingBox bb = new TaskSnapshotResponse.BoundingBox();
                bb.setMinLat(((Number) row[0]).doubleValue());
                bb.setMaxLat(((Number) row[1]).doubleValue());
                bb.setMinLng(((Number) row[2]).doubleValue());
                bb.setMaxLng(((Number) row[3]).doubleValue());
                ts.setBoundingBox(bb);
            } catch (Exception ignored) { /* 无点或聚合异常时不返回 bounding_box */ }
        }
        r.setTrajectorySummary(ts);
        return r;
    }

    /** 关闭任务。FOUND / FALSE_ALARM。 */
    @Transactional(rollbackFor = Exception.class)
    public RescueTaskEntity close(Long taskId, TaskCloseRequest req) {
        AuthUser user = SecurityUtil.current();
        RescueTaskEntity t = taskRepository.findById(taskId)
                .orElseThrow(() -> BizException.of(ErrorCode.E_TASK_4041));
        authorizationService.assertGuardian(user, t.getPatientId());
        if ("CLOSED_FOUND".equals(t.getStatus()) || "CLOSED_FALSE_ALARM".equals(t.getStatus())) {
            throw BizException.of(ErrorCode.E_TASK_4092);
        }
        if ("FALSE_ALARM".equals(req.getCloseType())) {
            if ("SUSTAINED".equals(t.getStatus())) {
                throw BizException.of(ErrorCode.E_TASK_4093);
            }
            if (req.getCloseReason() == null || req.getCloseReason().isBlank()) {
                throw BizException.of(ErrorCode.E_TASK_4005);
            }
            t.setStatus("CLOSED_FALSE_ALARM");
        } else if ("FOUND".equals(req.getCloseType())) {
            t.setStatus("CLOSED_FOUND");
            t.setFoundLocationLat(req.getFoundLocationLat());
            t.setFoundLocationLng(req.getFoundLocationLng());
        } else {
            throw BizException.of(ErrorCode.E_TASK_4004);
        }
        t.setCloseType(req.getCloseType());
        t.setCloseReason(req.getCloseReason());
        t.setClosedBy(user.getUserId());
        t.setClosedAt(OffsetDateTime.now());
        t.setEventVersion(t.getEventVersion() + 1);
        taskRepository.save(t);

        // 患者状态回归 NORMAL
        PatientProfileEntity patient = patientRepository.findById(t.getPatientId()).orElse(null);
        if (patient != null) {
            patient.setLostStatus("NORMAL");
            patient.setLostStatusEventTime(OffsetDateTime.now());
            patientRepository.save(patient);
        }

        String topic = "FOUND".equals(req.getCloseType())
                ? OutboxTopics.TASK_CLOSED_FOUND : OutboxTopics.TASK_CLOSED_FALSE_ALARM;
        outboxService.publish(topic, t.getTaskNo(), String.valueOf(t.getPatientId()),
                Map.of("task_id", t.getId(), "task_no", t.getTaskNo(),
                        "close_type", t.getCloseType(), "closed_by", user.getUserId()));
        return t;
    }

    /** 升级为长期任务 SUSTAINED（由调度器或管理员触发）。 */
    @Transactional(rollbackFor = Exception.class)
    public RescueTaskEntity sustain(Long taskId) {
        RescueTaskEntity t = taskRepository.findById(taskId)
                .orElseThrow(() -> BizException.of(ErrorCode.E_TASK_4041));
        if (!"ACTIVE".equals(t.getStatus())) {
            throw BizException.of(ErrorCode.E_TASK_4092);
        }
        t.setStatus("SUSTAINED");
        t.setSustainedAt(OffsetDateTime.now());
        t.setEventVersion(t.getEventVersion() + 1);
        taskRepository.save(t);
        outboxService.publish(OutboxTopics.TASK_SUSTAINED, t.getTaskNo(), String.valueOf(t.getPatientId()),
                Map.of("task_id", t.getId(), "task_no", t.getTaskNo()));
        return t;
    }
}
