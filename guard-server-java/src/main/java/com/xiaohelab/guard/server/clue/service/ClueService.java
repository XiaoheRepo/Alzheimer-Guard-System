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
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
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

    /**
     * 家属上报线索。若携带 task_id 则以任务绑定的 patient_id 为准，否则使用请求中的 patient_id；
     * 最终会校验当前登录用户对该患者具有监护权。
     *
     * @param req      线索上报请求（坐标、照片、描述等）
     * @param clientIp 请求侧 IP（用于风控留痕）
     * @return 已落库的线索实体
     * @throws BizException E_TASK_4041 任务不存在；E_PRO_4041 未指定患者；E_PRO_4033 无监护权
     */
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

    /**
     * 匿名扫码上报线索（通过患者短码入口）。
     * 入口 token 的 jti 会被记录以便风控回溯；同时落设备指纹。
     *
     * @param req               线索上报请求
     * @param entryTokenJti     入口 token 的唯一标识
     * @param clientIp          请求侧 IP
     * @param deviceFingerprint 设备指纹 hash
     * @return 已落库的线索实体
     */
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

    /**
     * 内部统一落库方法：坐标归一化为 WGS84、生成业务单号、序列化照片数组、发布 CLUE_VALIDATED 事件。
     * 家属与匿名两种上报路径共用此方法，差异仅在 {@code reporterUserId}/{@code reporterType}/{@code entryTokenJti}。
     */
    private ClueRecordEntity saveClue(ClueReportRequest req, Long reporterUserId, String reporterType,
                                      Long patientId, Long taskId, String clientIp, String entryTokenJti) {
        // 1. 坐标系归一化（toWgs84 入参为 lng,lat，返回 [lng,lat]）
        double[] wgs = CoordUtil.toWgs84(req.getLongitude(), req.getLatitude(),
                req.getCoordSystem() != null ? req.getCoordSystem() : "WGS84");
        double normLng = wgs[0];
        double normLat = wgs[1];

        ClueRecordEntity c = new ClueRecordEntity();
        c.setClueNo(BusinessNoUtil.clueNo());
        c.setPatientId(patientId);
        c.setTaskId(taskId);
        c.setTagCode(req.getTagCode());
        c.setSourceType(req.getSourceType());
        c.setReporterUserId(reporterUserId);
        c.setReporterType(reporterType);
        c.setLatitude(normLat);
        c.setLongitude(normLng);
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

        outboxService.publish(OutboxTopics.CLUE_VALIDATED, c.getClueNo(),
                String.valueOf(patientId),
                Map.of("clue_id", c.getId(), "clue_no", c.getClueNo(), "task_id", taskId,
                        "patient_id", patientId, "reporter_type", reporterType,
                        "lat", normLat, "lng", normLng));
        log.info("[Clue] reported clueNo={} patientId={} reporter={}", c.getClueNo(), patientId, reporterType);
        return c;
    }

    /**
     * 查询单条线索详情（监护权校验）。
     * @throws BizException E_CLUE_4043 线索不存在
     */
    public ClueRecordEntity get(Long clueId) {
        AuthUser user = SecurityUtil.current();
        ClueRecordEntity c = clueRepository.findById(clueId)
                .orElseThrow(() -> BizException.of(ErrorCode.E_CLUE_4043));
        authorizationService.assertGuardian(user, c.getPatientId());
        return c;
    }

    /**
     * 按任务分页列出线索（倒序）。
     * @param taskId 任务主键
     * @param page   页码
     * @param size   每页大小
     */
    public Page<ClueRecordEntity> listByTask(Long taskId, int page, int size) {
        AuthUser user = SecurityUtil.current();
        RescueTaskEntity t = taskRepository.findById(taskId)
                .orElseThrow(() -> BizException.of(ErrorCode.E_TASK_4041));
        authorizationService.assertGuardian(user, t.getPatientId());
        return clueRepository.findByTaskIdOrderByCreatedAtDesc(taskId, PageRequest.of(page, size));
    }

    /**
     * 3.2.6 线索列表查询（Offset 分页 + 多条件筛选）。
     * <p>权限策略：</p>
     * <ul>
     *   <li>ADMIN：可不传 patient_id 与 task_id，返回全量（仅限管理后台）。</li>
     *   <li>家属：必须传入 patient_id 或 task_id，后端会校验监护关系。</li>
     * </ul>
     */
    public Page<ClueRecordEntity> list(Long taskId, Long patientId, String status,
                                       Boolean suspectFlag, int pageNo, int pageSize) {
        AuthUser user = SecurityUtil.current();
        Long effectivePatientId = patientId;
        if (taskId != null) {
            RescueTaskEntity t = taskRepository.findById(taskId)
                    .orElseThrow(() -> BizException.of(ErrorCode.E_TASK_4041));
            effectivePatientId = t.getPatientId();
        }
        // 家属必须指定 patient_id / task_id，且具备监护权
        if (!user.isAdmin()) {
            if (effectivePatientId == null) {
                throw BizException.of(ErrorCode.E_PRO_4041);
            }
            authorizationService.assertGuardian(user, effectivePatientId);
        }
        final Long pid = effectivePatientId;
        Specification<ClueRecordEntity> spec = (root, cq, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (taskId != null)       ps.add(cb.equal(root.get("taskId"), taskId));
            if (pid != null)          ps.add(cb.equal(root.get("patientId"), pid));
            if (status != null && !status.isBlank())
                                      ps.add(cb.equal(root.get("status"), status));
            if (suspectFlag != null)  ps.add(cb.equal(root.get("suspectFlag"), suspectFlag));
            return cb.and(ps.toArray(new Predicate[0]));
        };
        int idx = Math.max(pageNo, 1) - 1;
        int size = Math.min(Math.max(pageSize, 1), 100);
        return clueRepository.findAll(spec,
                PageRequest.of(idx, size, Sort.by(Sort.Direction.DESC, "createdAt")));
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
