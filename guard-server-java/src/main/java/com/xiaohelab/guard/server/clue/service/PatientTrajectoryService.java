package com.xiaohelab.guard.server.clue.service;

import com.xiaohelab.guard.server.clue.dto.TrackPointRequest;
import com.xiaohelab.guard.server.clue.entity.PatientTrajectoryEntity;
import com.xiaohelab.guard.server.clue.repository.PatientTrajectoryRepository;
import com.xiaohelab.guard.server.common.error.ErrorCode;
import com.xiaohelab.guard.server.common.event.OutboxTopics;
import com.xiaohelab.guard.server.common.exception.BizException;
import com.xiaohelab.guard.server.common.security.AuthUser;
import com.xiaohelab.guard.server.common.security.SecurityUtil;
import com.xiaohelab.guard.server.common.util.CoordUtil;
import com.xiaohelab.guard.server.outbox.service.OutboxService;
import com.xiaohelab.guard.server.patient.entity.PatientProfileEntity;
import com.xiaohelab.guard.server.patient.service.GuardianAuthorizationService;
import com.xiaohelab.guard.server.rescue.dto.TrajectoryLatestResponse;
import com.xiaohelab.guard.server.rescue.entity.RescueTaskEntity;
import com.xiaohelab.guard.server.rescue.repository.RescueTaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
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
    private final RescueTaskRepository rescueTaskRepository;

    public PatientTrajectoryService(PatientTrajectoryRepository trajectoryRepository,
                                    GuardianAuthorizationService authorizationService,
                                    OutboxService outboxService,
                                    RescueTaskRepository rescueTaskRepository) {
        this.trajectoryRepository = trajectoryRepository;
        this.authorizationService = authorizationService;
        this.outboxService = outboxService;
        this.rescueTaskRepository = rescueTaskRepository;
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

    /**
     * 3.2.7 任务轨迹最新切片（Cursor 分页）。
     * <p>支持 since（时间锚点）/ afterVersion（id 增量）两种增量拉取。</p>
     * <p>安全：必须为该任务对应患者的监护人或管理员。</p>
     *
     * @param taskId        目标任务
     * @param since         时间锚点（可选）
     * @param afterVersion  id 增量锚点（可选）
     * @param pageSize      每页条数（1~200）
     * @return Cursor 分页响应
     */
    public TrajectoryLatestResponse queryLatestForTask(Long taskId, OffsetDateTime since,
                                                       Long afterVersion, int pageSize) {
        AuthUser user = SecurityUtil.current();
        RescueTaskEntity task = rescueTaskRepository.findById(taskId)
                .orElseThrow(() -> BizException.of(ErrorCode.E_TASK_4041));
        authorizationService.assertGuardian(user, task.getPatientId());

        int safeSize = Math.min(Math.max(pageSize, 1), 200);
        // 多取 1 条判断 has_next
        Page<PatientTrajectoryEntity> page = trajectoryRepository.findLatestForTask(
                taskId, since, afterVersion, PageRequest.of(0, safeSize + 1));
        List<PatientTrajectoryEntity> raw = page.getContent();
        boolean hasNext = raw.size() > safeSize;
        if (hasNext) raw = raw.subList(0, safeSize);

        List<TrajectoryLatestResponse.Point> items = new ArrayList<>(raw.size());
        for (PatientTrajectoryEntity e : raw) {
            TrajectoryLatestResponse.Point p = new TrajectoryLatestResponse.Point();
            p.setTrajectoryId(e.getId());
            p.setPatientId(e.getPatientId());
            p.setTaskId(e.getTaskId());
            p.setClueId(e.getClueId());
            p.setLatitude(e.getLatitude());
            p.setLongitude(e.getLongitude());
            p.setCoordSystem("WGS84");
            p.setRecordedAt(e.getWindowEnd());
            p.setSourceType(e.getSourceType());
            p.setVersion(e.getId()); // id 作为单调递增 version 锚点
            items.add(p);
        }

        String nextCursor = null;
        if (hasNext && !items.isEmpty()) {
            // Cursor = base64(`{"id": <last_id>}`) —— 毕设简化：直接用 id 字符串作为游标
            nextCursor = String.valueOf(items.get(items.size() - 1).getTrajectoryId());
        }
        return new TrajectoryLatestResponse(items, safeSize, nextCursor, hasNext);
    }
}
