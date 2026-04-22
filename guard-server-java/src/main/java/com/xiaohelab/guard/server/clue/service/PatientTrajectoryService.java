package com.xiaohelab.guard.server.clue.service;

import com.xiaohelab.guard.server.clue.dto.TrackPointRequest;
import com.xiaohelab.guard.server.clue.entity.PatientTrajectoryEntity;
import com.xiaohelab.guard.server.clue.repository.PatientTrajectoryRepository;
import com.xiaohelab.guard.server.common.event.OutboxTopics;
import com.xiaohelab.guard.server.common.security.AuthUser;
import com.xiaohelab.guard.server.common.security.SecurityUtil;
import com.xiaohelab.guard.server.common.util.CoordUtil;
import com.xiaohelab.guard.server.outbox.service.OutboxService;
import com.xiaohelab.guard.server.patient.entity.PatientProfileEntity;
import com.xiaohelab.guard.server.patient.service.GuardianAuthorizationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * 患者实时轨迹服务。写入一条轨迹点，并做围栏突破判定（HC 业务规则）。
 * 说明：毕设简化版，使用单点 POINT 写入；未落 LINESTRING 聚合窗口。
 */
@Service
public class PatientTrajectoryService {

    private static final Logger log = LoggerFactory.getLogger(PatientTrajectoryService.class);

    private final PatientTrajectoryRepository trajectoryRepository;
    private final GuardianAuthorizationService authorizationService;
    private final OutboxService outboxService;

    public PatientTrajectoryService(PatientTrajectoryRepository trajectoryRepository,
                                    GuardianAuthorizationService authorizationService,
                                    OutboxService outboxService) {
        this.trajectoryRepository = trajectoryRepository;
        this.authorizationService = authorizationService;
        this.outboxService = outboxService;
    }

    @Transactional(rollbackFor = Exception.class)
    public PatientTrajectoryEntity recordPoint(TrackPointRequest req) {
        AuthUser user = SecurityUtil.current();
        PatientProfileEntity p = authorizationService.assertGuardian(user, req.getPatientId());
        double[] wgs = CoordUtil.toWgs84(req.getLongitude(), req.getLatitude(),
                req.getCoordSystem() != null ? req.getCoordSystem() : "WGS84");

        OffsetDateTime now = OffsetDateTime.now();
        PatientTrajectoryEntity t = new PatientTrajectoryEntity();
        t.setPatientId(req.getPatientId());
        t.setTaskId(req.getTaskId());
        t.setWindowStart(now);
        t.setWindowEnd(now);
        t.setPointCount(1);
        t.setGeometryType("POINT");
        t.setLatitude(wgs[1]);
        t.setLongitude(wgs[0]);
        t.setSourceType(req.getSourceType());
        trajectoryRepository.save(t);

        outboxService.publish(OutboxTopics.TRACK_UPDATED, String.valueOf(t.getId()),
                String.valueOf(req.getPatientId()),
                Map.of("patient_id", req.getPatientId(), "task_id", req.getTaskId(),
                        "lat", wgs[1], "lng", wgs[0], "source", req.getSourceType()));

        // 围栏突破判定
        if (Boolean.TRUE.equals(p.getFenceEnabled())
                && p.getFenceCenterLat() != null && p.getFenceCenterLng() != null && p.getFenceRadiusM() != null) {
            double dist = CoordUtil.haversineMeter(p.getFenceCenterLng(), p.getFenceCenterLat(), wgs[0], wgs[1]);
            if (dist > p.getFenceRadiusM()) {
                outboxService.publish(OutboxTopics.FENCE_BREACHED, String.valueOf(t.getId()),
                        String.valueOf(req.getPatientId()),
                        Map.of("patient_id", req.getPatientId(), "distance_m", (int) dist,
                                "fence_radius_m", p.getFenceRadiusM(),
                                "lat", wgs[1], "lng", wgs[0]));
                log.warn("[Fence] breach patientId={} dist={}m radius={}m",
                        req.getPatientId(), (int) dist, p.getFenceRadiusM());
            }
        }
        return t;
    }

    public List<PatientTrajectoryEntity> listByTask(Long taskId) {
        AuthUser user = SecurityUtil.current();
        List<PatientTrajectoryEntity> list = trajectoryRepository.findByTaskIdOrderByWindowStartAsc(taskId);
        if (!list.isEmpty()) {
            authorizationService.assertGuardian(user, list.get(0).getPatientId());
        }
        return list;
    }

    public List<PatientTrajectoryEntity> listByPatient(Long patientId, OffsetDateTime from, OffsetDateTime to) {
        AuthUser user = SecurityUtil.current();
        authorizationService.assertGuardian(user, patientId);
        return trajectoryRepository.findByPatientIdAndWindowStartBetweenOrderByWindowStartAsc(patientId, from, to);
    }
}
