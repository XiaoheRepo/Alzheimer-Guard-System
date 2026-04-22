package com.xiaohelab.guard.server.material.service;

import com.xiaohelab.guard.server.common.error.ErrorCode;
import com.xiaohelab.guard.server.common.event.OutboxTopics;
import com.xiaohelab.guard.server.common.exception.BizException;
import com.xiaohelab.guard.server.common.security.AuthUser;
import com.xiaohelab.guard.server.common.security.SecurityUtil;
import com.xiaohelab.guard.server.common.util.BusinessNoUtil;
import com.xiaohelab.guard.server.common.util.CryptoUtil;
import com.xiaohelab.guard.server.material.entity.TagAssetEntity;
import com.xiaohelab.guard.server.material.repository.TagAssetRepository;
import com.xiaohelab.guard.server.outbox.service.OutboxService;
import com.xiaohelab.guard.server.patient.entity.PatientProfileEntity;
import com.xiaohelab.guard.server.patient.service.GuardianAuthorizationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** 标签生命周期：绑定/疑似丢失/丢失确认/作废。 */
@Service
public class TagService {

    private static final Logger log = LoggerFactory.getLogger(TagService.class);
    private static final int BATCH_GENERATE_LIMIT = 10000;

    private final TagAssetRepository tagRepository;
    private final GuardianAuthorizationService authorizationService;
    private final OutboxService outboxService;

    /** 进程内批量发号任务登记表（足以满足毕设场景；生产环境应落库 tag_generation_job）。 */
    private final Map<String, Map<String, Object>> jobRegistry = new ConcurrentHashMap<>();

    public TagService(TagAssetRepository tagRepository,
                      GuardianAuthorizationService authorizationService,
                      OutboxService outboxService) {
        this.tagRepository = tagRepository;
        this.authorizationService = authorizationService;
        this.outboxService = outboxService;
    }

    /** 监护人绑定标签到患者。标签需为 ALLOCATED 或 RECEIVED。 */
    @Transactional(rollbackFor = Exception.class)
    public TagAssetEntity bind(String tagCode, Long patientId) {
        AuthUser user = SecurityUtil.current();
        PatientProfileEntity p = authorizationService.assertGuardian(user, patientId);
        TagAssetEntity t = tagRepository.findByTagCode(tagCode)
                .orElseThrow(() -> BizException.of(ErrorCode.E_MAT_4044));
        if (!("ALLOCATED".equals(t.getStatus()))) {
            throw BizException.of(ErrorCode.E_MAT_4091);
        }
        t.setStatus("BOUND");
        t.setPatientId(patientId);
        t.setShortCode(p.getShortCode());
        t.setResourceToken(BusinessNoUtil.ticket() + "." + CryptoUtil.randomToken(16));
        t.setBoundAt(OffsetDateTime.now());
        tagRepository.save(t);
        outboxService.publish(OutboxTopics.TAG_BOUND, t.getTagCode(), String.valueOf(patientId),
                Map.of("tag_code", t.getTagCode(), "patient_id", patientId, "order_id", t.getOrderId()));
        return t;
    }

    /** 疑似丢失（例如长时间未扫或被多次异地扫码）。 */
    @Transactional(rollbackFor = Exception.class)
    public TagAssetEntity markSuspectedLost(String tagCode) {
        AuthUser user = SecurityUtil.current();
        TagAssetEntity t = tagRepository.findByTagCode(tagCode)
                .orElseThrow(() -> BizException.of(ErrorCode.E_MAT_4044));
        authorizationService.assertGuardian(user, t.getPatientId());
        if (!"BOUND".equals(t.getStatus())) throw BizException.of(ErrorCode.E_MAT_4098);
        t.setStatus("SUSPECTED_LOST");
        t.setSuspectedLostAt(OffsetDateTime.now());
        tagRepository.save(t);
        outboxService.publish(OutboxTopics.TAG_SUSPECTED_LOST, t.getTagCode(),
                String.valueOf(t.getPatientId()),
                Map.of("tag_code", t.getTagCode(), "patient_id", t.getPatientId()));
        return t;
    }

    /** 监护人确认丢失。 */
    @Transactional(rollbackFor = Exception.class)
    public TagAssetEntity confirmLost(String tagCode, String reason) {
        AuthUser user = SecurityUtil.current();
        TagAssetEntity t = tagRepository.findByTagCode(tagCode)
                .orElseThrow(() -> BizException.of(ErrorCode.E_MAT_4044));
        authorizationService.assertGuardian(user, t.getPatientId());
        if (!("BOUND".equals(t.getStatus()) || "SUSPECTED_LOST".equals(t.getStatus()))) {
            throw BizException.of(ErrorCode.E_MAT_4098);
        }
        t.setStatus("LOST");
        t.setLostReason(reason);
        t.setLostAt(OffsetDateTime.now());
        tagRepository.save(t);
        outboxService.publish(OutboxTopics.TAG_LOST, t.getTagCode(), String.valueOf(t.getPatientId()),
                Map.of("tag_code", t.getTagCode(), "patient_id", t.getPatientId(), "reason", reason));
        return t;
    }

    public TagAssetEntity getByCode(String tagCode) {
        AuthUser user = SecurityUtil.current();
        TagAssetEntity t = tagRepository.findByTagCode(tagCode)
                .orElseThrow(() -> BizException.of(ErrorCode.E_MAT_4044));
        if (t.getPatientId() != null) {
            authorizationService.assertGuardian(user, t.getPatientId());
        } else if (!user.isAdmin()) {
            throw BizException.of(ErrorCode.E_MAT_4030);
        }
        return t;
    }

    /**
     * 3.4.8 批量发号（管理员）。
     * <p>同步落库 UNBOUND 标签并登记任务（本毕设系统采用同步实现，保证接口语义可用）。</p>
     *
     * @param tagType   QR_CODE / NFC
     * @param quantity  1-10000
     * @param batchKeyId 可选加密批次密钥 ID（当前仅记录于 batch_no）
     * @return 任务登记信息 Map{job_id,status,quantity,created_at}
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> batchGenerate(String tagType, int quantity, String batchKeyId) {
        AuthUser user = SecurityUtil.current();
        if (!user.isAdmin()) throw BizException.of(ErrorCode.E_MAT_4030);
        if (quantity <= 0 || quantity > BATCH_GENERATE_LIMIT) {
            throw BizException.of(ErrorCode.E_MAT_4225);
        }
        if (tagType == null || !(tagType.equals("QR_CODE") || tagType.equals("NFC"))) {
            throw BizException.of(ErrorCode.E_MAT_4225);
        }
        String jobId = BusinessNoUtil.batchJobId();
        String batchNo = batchKeyId != null && !batchKeyId.isBlank() ? batchKeyId : jobId;
        OffsetDateTime now = OffsetDateTime.now();

        // 1. 构造并批量落库
        List<TagAssetEntity> batch = new ArrayList<>(quantity);
        for (int i = 0; i < quantity; i++) {
            TagAssetEntity t = new TagAssetEntity();
            t.setTagCode(BusinessNoUtil.tagCode());
            t.setTagType(tagType);
            t.setStatus("UNBOUND");
            t.setBatchNo(batchNo);
            batch.add(t);
        }
        tagRepository.saveAll(batch);

        // 2. 登记任务
        Map<String, Object> job = new LinkedHashMap<>();
        job.put("job_id", jobId);
        job.put("status", "COMPLETED");
        job.put("tag_type", tagType);
        job.put("total_count", quantity);
        job.put("success_count", quantity);
        job.put("fail_count", 0);
        job.put("quantity", quantity);
        job.put("created_at", now);
        job.put("completed_at", OffsetDateTime.now());
        jobRegistry.put(jobId, job);

        // 3. 发布发号事件
        outboxService.publish(OutboxTopics.TAG_BATCH_GENERATED, jobId, batchNo,
                Map.of("job_id", jobId, "tag_type", tagType,
                        "quantity", quantity, "batch_no", batchNo,
                        "operator", user.getUserId()));

        log.info("[Tag] batchGenerate jobId={} tagType={} quantity={} by={}",
                jobId, tagType, quantity, user.getUserId());
        return job;
    }

    /** 3.4.9 查询发号任务。 */
    public Map<String, Object> queryBatchJob(String jobId) {
        AuthUser user = SecurityUtil.current();
        if (!user.isAdmin()) throw BizException.of(ErrorCode.E_MAT_4030);
        Map<String, Object> job = jobRegistry.get(jobId);
        if (job == null) throw BizException.of(ErrorCode.E_MAT_4044);
        return job;
    }

    /**
     * 3.4.10 库存摘要：按 tag_type 聚合各状态数量。
     */
    public Map<String, Object> inventorySummary() {
        AuthUser user = SecurityUtil.current();
        if (!user.isAdmin()) throw BizException.of(ErrorCode.E_MAT_4030);
        // 1. 聚合查询 [tagType, status, count]
        List<Object[]> rows = tagRepository.aggInventoryByTypeAndStatus();
        // 2. 按 tagType 分桶
        Map<String, Map<String, Long>> bucket = new LinkedHashMap<>();
        for (Object[] row : rows) {
            String tagType = (String) row[0];
            String status = (String) row[1];
            long cnt = ((Number) row[2]).longValue();
            bucket.computeIfAbsent(tagType, k -> new HashMap<>()).merge(status, cnt, Long::sum);
        }
        // 3. 组装响应
        List<Map<String, Object>> summary = new ArrayList<>();
        for (Map.Entry<String, Map<String, Long>> e : bucket.entrySet()) {
            Map<String, Long> counts = e.getValue();
            long total = counts.values().stream().mapToLong(Long::longValue).sum();
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("tag_type", e.getKey());
            item.put("total", total);
            item.put("unbound", counts.getOrDefault("UNBOUND", 0L));
            item.put("allocated", counts.getOrDefault("ALLOCATED", 0L));
            item.put("bound", counts.getOrDefault("BOUND", 0L));
            item.put("suspected_lost", counts.getOrDefault("SUSPECTED_LOST", 0L));
            item.put("lost", counts.getOrDefault("LOST", 0L));
            item.put("voided", counts.getOrDefault("VOIDED", 0L));
            summary.add(item);
        }
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("summary", summary);
        resp.put("updated_at", OffsetDateTime.now());
        return resp;
    }
}
