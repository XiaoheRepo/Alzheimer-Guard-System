package com.xiaohelab.guard.server.clue.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaohelab.guard.server.clue.dto.ClueReportRequest;
import com.xiaohelab.guard.server.clue.dto.ClueReviewRequest;
import com.xiaohelab.guard.server.clue.entity.ClueRecordEntity;
import com.xiaohelab.guard.server.clue.repository.ClueRecordRepository;
import com.xiaohelab.guard.server.common.error.ErrorCode;
import com.xiaohelab.guard.server.common.event.OutboxTopics;
import com.xiaohelab.guard.server.common.exception.BizException;
import com.xiaohelab.guard.server.common.security.AuthUser;
import com.xiaohelab.guard.server.common.security.SecurityUtil;
import com.xiaohelab.guard.server.common.util.BusinessNoUtil;
import com.xiaohelab.guard.server.common.util.CoordUtil;
import com.xiaohelab.guard.server.outbox.service.OutboxService;
import com.xiaohelab.guard.server.patient.service.GuardianAuthorizationService;
import com.xiaohelab.guard.server.rescue.entity.RescueTaskEntity;
import com.xiaohelab.guard.server.rescue.repository.RescueTaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/** 线索记录服务：家属上报 + 管理员复核（override/reject）。 */
@Service
public class ClueService {

    private static final Logger log = LoggerFactory.getLogger(ClueService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final ClueRecordRepository clueRepository;
    private final RescueTaskRepository taskRepository;
    private final GuardianAuthorizationService authorizationService;
    private final OutboxService outboxService;

    public ClueService(ClueRecordRepository clueRepository, RescueTaskRepository taskRepository,
                       GuardianAuthorizationService authorizationService, OutboxService outboxService) {
        this.clueRepository = clueRepository;
        this.taskRepository = taskRepository;
        this.authorizationService = authorizationService;
        this.outboxService = outboxService;
    }

    @Transactional(rollbackFor = Exception.class)
    public ClueRecordEntity familyReport(ClueReportRequest req, String clientIp) {
        AuthUser user = SecurityUtil.current();
        Long patientId = req.getPatientId();
        Long taskId = req.getTaskId();
        if (taskId != null) {
            RescueTaskEntity t = taskRepository.findById(taskId)
                    .orElseThrow(() -> BizException.of(ErrorCode.E_TASK_4041));
            patientId = t.getPatientId();
        }
        if (patientId == null) throw BizException.of(ErrorCode.E_PRO_4041);
        authorizationService.assertGuardian(user, patientId);
        return saveClue(req, user.getUserId(), "FAMILY", patientId, taskId, clientIp, null);
    }

    @Transactional(rollbackFor = Exception.class)
    public ClueRecordEntity anonymousReport(ClueReportRequest req, String entryTokenJti,
                                            String clientIp, String deviceFingerprint) {
        Long patientId = req.getPatientId();
        if (patientId == null) throw BizException.of(ErrorCode.E_PRO_4041);
        ClueRecordEntity c = saveClue(req, null, "ANONYMOUS", patientId, req.getTaskId(), clientIp, entryTokenJti);
        c.setDeviceFingerprint(deviceFingerprint);
        clueRepository.save(c);
        return c;
    }

    private ClueRecordEntity saveClue(ClueReportRequest req, Long reporterUserId, String reporterType,
                                      Long patientId, Long taskId, String clientIp, String entryTokenJti) {
        // 1. 坐标系归一化
        double[] wgs = CoordUtil.toWgs84(req.getLatitude(), req.getLongitude(),
                req.getCoordSystem() != null ? req.getCoordSystem() : "WGS84");

        ClueRecordEntity c = new ClueRecordEntity();
        c.setClueNo(BusinessNoUtil.clueNo());
        c.setPatientId(patientId);
        c.setTaskId(taskId);
        c.setTagCode(req.getTagCode());
        c.setSourceType(req.getSourceType());
        c.setReporterUserId(reporterUserId);
        c.setReporterType(reporterType);
        c.setLatitude(wgs[0]);
        c.setLongitude(wgs[1]);
        c.setCoordSystem("WGS84");
        c.setDescription(req.getDescription());
        c.setTagOnly(Boolean.TRUE.equals(req.getTagOnly()));
        try {
            c.setPhotoUrls(MAPPER.writeValueAsString(req.getPhotoUrls() != null ? req.getPhotoUrls() : List.of()));
        } catch (JsonProcessingException e) {
            throw BizException.of(ErrorCode.E_SYS_5000, "photo_urls 序列化失败");
        }
        c.setClientIp(clientIp);
        c.setEntryTokenJti(entryTokenJti);
        c.setStatus("VALID");
        c.setSuspectFlag(false);
        c.setDriftFlag(false);
        c.setRiskScore(new BigDecimal("0.00"));
        clueRepository.save(c);

        outboxService.publish(OutboxTopics.CLUE_REPORTED_VALIDATED, c.getClueNo(),
                String.valueOf(patientId),
                Map.of("clue_id", c.getId(), "clue_no", c.getClueNo(), "task_id", taskId,
                        "patient_id", patientId, "reporter_type", reporterType,
                        "lat", wgs[0], "lng", wgs[1]));
        log.info("[Clue] reported clueNo={} patientId={} reporter={}", c.getClueNo(), patientId, reporterType);
        return c;
    }

    public ClueRecordEntity get(Long clueId) {
        AuthUser user = SecurityUtil.current();
        ClueRecordEntity c = clueRepository.findById(clueId)
                .orElseThrow(() -> BizException.of(ErrorCode.E_CLUE_4043));
        authorizationService.assertGuardian(user, c.getPatientId());
        return c;
    }

    public Page<ClueRecordEntity> listByTask(Long taskId, int page, int size) {
        AuthUser user = SecurityUtil.current();
        RescueTaskEntity t = taskRepository.findById(taskId)
                .orElseThrow(() -> BizException.of(ErrorCode.E_TASK_4041));
        authorizationService.assertGuardian(user, t.getPatientId());
        return clueRepository.findByTaskIdOrderByCreatedAtDesc(taskId, PageRequest.of(page, size));
    }

    /** 管理员复核：OVERRIDE 或 REJECT（仅针对 suspect_flag=true 的线索）。 */
    @Transactional(rollbackFor = Exception.class)
    public ClueRecordEntity review(Long clueId, ClueReviewRequest req) {
        AuthUser user = SecurityUtil.current();
        if (!user.isAdmin()) throw BizException.of(ErrorCode.E_GOV_4030);
        ClueRecordEntity c = clueRepository.findById(clueId)
                .orElseThrow(() -> BizException.of(ErrorCode.E_CLUE_4043));
        if (!Boolean.TRUE.equals(c.getSuspectFlag())) {
            throw BizException.of(ErrorCode.E_CLUE_4221);
        }
        if (c.getReviewStatus() != null && !"PENDING".equals(c.getReviewStatus())) {
            throw BizException.of(ErrorCode.E_CLUE_4091);
        }
        if ("OVERRIDE".equals(req.getAction())) {
            c.setReviewStatus("OVERRIDDEN");
            c.setStatus("OVERRIDDEN");
            c.setOverrideReason(req.getReason());
            outboxService.publish(OutboxTopics.CLUE_OVERRIDDEN, c.getClueNo(), String.valueOf(c.getPatientId()),
                    Map.of("clue_id", c.getId(), "reviewer", user.getUserId()));
        } else if ("REJECT".equals(req.getAction())) {
            c.setReviewStatus("REJECTED");
            c.setStatus("REJECTED");
            c.setRejectReason(req.getReason());
            outboxService.publish(OutboxTopics.CLUE_REJECTED, c.getClueNo(), String.valueOf(c.getPatientId()),
                    Map.of("clue_id", c.getId(), "reviewer", user.getUserId()));
        } else {
            throw BizException.of(ErrorCode.E_CLUE_4008);
        }
        c.setReviewedBy(user.getUserId());
        c.setReviewedAt(OffsetDateTime.now());
        clueRepository.save(c);
        return c;
    }
}
