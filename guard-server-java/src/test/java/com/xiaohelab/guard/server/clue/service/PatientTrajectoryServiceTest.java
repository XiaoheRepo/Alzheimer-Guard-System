package com.xiaohelab.guard.server.clue.service;

import com.xiaohelab.guard.server.clue.dto.TrackPointRequest;
import com.xiaohelab.guard.server.clue.entity.PatientTrajectoryEntity;
import com.xiaohelab.guard.server.clue.repository.PatientTrajectoryRepository;
import com.xiaohelab.guard.server.common.security.AuthUser;
import com.xiaohelab.guard.server.common.security.SecurityUtil;
import com.xiaohelab.guard.server.outbox.service.OutboxService;
import com.xiaohelab.guard.server.patient.entity.PatientProfileEntity;
import com.xiaohelab.guard.server.patient.repository.PatientProfileRepository;
import com.xiaohelab.guard.server.patient.service.GuardianAuthorizationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * {@link PatientTrajectoryService} 单元测试：轨迹落库 + 围栏判定。
 */
@ExtendWith(MockitoExtension.class)
class PatientTrajectoryServiceTest {

    @Mock PatientTrajectoryRepository trajectoryRepository;
    @Mock PatientProfileRepository patientRepository;
    @Mock GuardianAuthorizationService authorizationService;
    @Mock OutboxService outboxService;

    @InjectMocks PatientTrajectoryService trajectoryService;

    private MockedStatic<SecurityUtil> securityUtilMocked;
    private final AuthUser user = new AuthUser(1L, "alice", "FAMILY");

    @BeforeEach
    void setup() {
        securityUtilMocked = Mockito.mockStatic(SecurityUtil.class);
        securityUtilMocked.when(SecurityUtil::current).thenReturn(user);
    }

    @AfterEach
    void tearDown() { securityUtilMocked.close(); }

    @Test
    void record_should_only_publish_track_when_inside_fence() {
        PatientProfileEntity p = new PatientProfileEntity();
        p.setId(7L);
        p.setFenceEnabled(true);
        p.setFenceCenterLng(116.4);
        p.setFenceCenterLat(39.9);
        p.setFenceRadiusM(500);
        when(authorizationService.assertGuardian(user, 7L)).thenReturn(p);
        when(trajectoryRepository.save(any())).thenAnswer(inv -> {
            PatientTrajectoryEntity e = inv.getArgument(0); e.setId(1L); return e;
        });

        TrackPointRequest req = new TrackPointRequest();
        req.setPatientId(7L);
        req.setLatitude(39.9005); req.setLongitude(116.4005); // 约 60m 内
        req.setCoordSystem("WGS84");
        trajectoryService.recordPoint(req);

        verify(outboxService, times(1)).publish(eq("track.updated"), anyString(), anyString(), anyMap());
        verify(outboxService, never()).publish(eq("fence.breached"), anyString(), anyString(), anyMap());
    }

    @Test
    void record_should_publish_fence_breached_when_outside() {
        PatientProfileEntity p = new PatientProfileEntity();
        p.setId(7L);
        p.setFenceEnabled(true);
        p.setFenceCenterLng(116.4);
        p.setFenceCenterLat(39.9);
        p.setFenceRadiusM(300);
        when(authorizationService.assertGuardian(user, 7L)).thenReturn(p);
        when(trajectoryRepository.save(any())).thenAnswer(inv -> {
            PatientTrajectoryEntity e = inv.getArgument(0); e.setId(2L); return e;
        });

        TrackPointRequest req = new TrackPointRequest();
        req.setPatientId(7L);
        req.setLatitude(39.92); req.setLongitude(116.42); // 远 > 300m
        req.setCoordSystem("WGS84");

        PatientTrajectoryEntity r = trajectoryService.recordPoint(req);

        assertThat(r.getId()).isEqualTo(2L);
        verify(outboxService).publish(eq("track.updated"), anyString(), anyString(), anyMap());
        verify(outboxService).publish(eq("fence.breached"), anyString(), anyString(), anyMap());
    }
}
