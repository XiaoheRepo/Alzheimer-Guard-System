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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Map;

/** 标签生命周期：绑定/疑似丢失/丢失确认/作废。 */
@Service
public class TagService {

    private final TagAssetRepository tagRepository;
    private final GuardianAuthorizationService authorizationService;
    private final OutboxService outboxService;

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
}
