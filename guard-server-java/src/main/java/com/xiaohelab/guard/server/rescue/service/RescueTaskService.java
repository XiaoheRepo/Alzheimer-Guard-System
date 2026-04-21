package com.xiaohelab.guard.server.rescue.service;

import com.xiaohelab.guard.server.common.error.ErrorCode;
import com.xiaohelab.guard.server.common.event.OutboxTopics;
import com.xiaohelab.guard.server.common.exception.BizException;
import com.xiaohelab.guard.server.common.security.AuthUser;
import com.xiaohelab.guard.server.common.security.SecurityUtil;
import com.xiaohelab.guard.server.common.util.BusinessNoUtil;
import com.xiaohelab.guard.server.outbox.service.OutboxService;
import com.xiaohelab.guard.server.patient.entity.PatientProfileEntity;
import com.xiaohelab.guard.server.patient.repository.PatientProfileRepository;
import com.xiaohelab.guard.server.patient.service.GuardianAuthorizationService;
import com.xiaohelab.guard.server.rescue.dto.TaskCloseRequest;
import com.xiaohelab.guard.server.rescue.dto.TaskCreateRequest;
import com.xiaohelab.guard.server.rescue.entity.RescueTaskEntity;
import com.xiaohelab.guard.server.rescue.repository.RescueTaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
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

    public RescueTaskService(RescueTaskRepository taskRepository,
                             PatientProfileRepository patientRepository,
                             GuardianAuthorizationService authorizationService,
                             OutboxService outboxService) {
        this.taskRepository = taskRepository;
        this.patientRepository = patientRepository;
        this.authorizationService = authorizationService;
        this.outboxService = outboxService;
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
