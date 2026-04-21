package com.xiaohelab.guard.server.rescue.service;

import com.xiaohelab.guard.server.common.exception.BizException;
import com.xiaohelab.guard.server.common.security.AuthUser;
import com.xiaohelab.guard.server.common.security.SecurityUtil;
import com.xiaohelab.guard.server.outbox.service.OutboxService;
import com.xiaohelab.guard.server.patient.entity.PatientProfileEntity;
import com.xiaohelab.guard.server.patient.repository.PatientProfileRepository;
import com.xiaohelab.guard.server.patient.service.GuardianAuthorizationService;
import com.xiaohelab.guard.server.rescue.dto.TaskCloseRequest;
import com.xiaohelab.guard.server.rescue.dto.TaskCreateRequest;
import com.xiaohelab.guard.server.rescue.entity.RescueTaskEntity;
import com.xiaohelab.guard.server.rescue.repository.RescueTaskRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * {@link RescueTaskService} 单元测试：创建/关闭/升级为 SUSTAINED。
 */
@ExtendWith(MockitoExtension.class)
class RescueTaskServiceTest {

    @Mock RescueTaskRepository taskRepository;
    @Mock PatientProfileRepository patientRepository;
    @Mock GuardianAuthorizationService authorizationService;
    @Mock OutboxService outboxService;

    @InjectMocks RescueTaskService rescueTaskService;

    private MockedStatic<SecurityUtil> secMock;
    private final AuthUser user = new AuthUser(1L, "alice", "FAMILY");

    @BeforeEach
    void setup() {
        secMock = Mockito.mockStatic(SecurityUtil.class);
        secMock.when(SecurityUtil::current).thenReturn(user);
    }

    @AfterEach
    void tearDown() { secMock.close(); }

    @Test
    void create_should_persist_active_task_and_publish_events() {
        PatientProfileEntity p = buildPatient(7L, "NORMAL");
        when(authorizationService.assertGuardian(user, 7L)).thenReturn(p);
        when(taskRepository.findActiveByPatient(7L)).thenReturn(Optional.empty());
        when(taskRepository.saveAndFlush(any())).thenAnswer(inv -> {
            RescueTaskEntity t = inv.getArgument(0); t.setId(100L); return t;
        });

        TaskCreateRequest req = new TaskCreateRequest();
        req.setPatientId(7L); req.setSource("FAMILY"); req.setRemark("走失");

        RescueTaskEntity t = rescueTaskService.create(req);

        assertThat(t.getStatus()).isEqualTo("ACTIVE");
        assertThat(t.getTaskNo()).startsWith("T");
        // 患者状态从 NORMAL 同步为 MISSING，触发 patient.missing_pending 事件 + task.created 事件
        verify(patientRepository).save(any(PatientProfileEntity.class));
        verify(outboxService).publish(eq("patient.missing_pending"), anyString(), anyString(), anyMap());
        verify(outboxService).publish(eq("task.created"), anyString(), anyString(), anyMap());
    }

    @Test
    void create_should_reject_when_another_active_task_exists() {
        PatientProfileEntity p = buildPatient(7L, "MISSING");
        when(authorizationService.assertGuardian(user, 7L)).thenReturn(p);
        when(taskRepository.findActiveByPatient(7L)).thenReturn(Optional.of(new RescueTaskEntity()));

        TaskCreateRequest req = new TaskCreateRequest();
        req.setPatientId(7L); req.setSource("FAMILY");

        assertThatThrownBy(() -> rescueTaskService.create(req))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("E_TASK_4091");
    }

    @Test
    void close_found_should_mark_closed_found_and_reset_patient() {
        RescueTaskEntity t = buildTask(100L, 7L, "ACTIVE");
        when(taskRepository.findById(100L)).thenReturn(Optional.of(t));
        when(patientRepository.findById(7L)).thenReturn(Optional.of(buildPatient(7L, "MISSING")));

        TaskCloseRequest req = new TaskCloseRequest();
        req.setCloseType("FOUND");
        req.setFoundLocationLat(39.9); req.setFoundLocationLng(116.4);

        RescueTaskEntity r = rescueTaskService.close(100L, req);

        assertThat(r.getStatus()).isEqualTo("CLOSED_FOUND");
        verify(outboxService).publish(eq("task.closed.found"), anyString(), anyString(), anyMap());
    }

    @Test
    void close_false_alarm_requires_reason_when_status_active() {
        RescueTaskEntity t = buildTask(100L, 7L, "ACTIVE");
        when(taskRepository.findById(100L)).thenReturn(Optional.of(t));

        TaskCloseRequest req = new TaskCloseRequest();
        req.setCloseType("FALSE_ALARM");

        assertThatThrownBy(() -> rescueTaskService.close(100L, req))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("E_TASK_4005");
    }

    @Test
    void close_false_alarm_should_reject_when_sustained() {
        RescueTaskEntity t = buildTask(100L, 7L, "SUSTAINED");
        when(taskRepository.findById(100L)).thenReturn(Optional.of(t));

        TaskCloseRequest req = new TaskCloseRequest();
        req.setCloseType("FALSE_ALARM"); req.setCloseReason("误报");

        assertThatThrownBy(() -> rescueTaskService.close(100L, req))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("E_TASK_4093");
    }

    @Test
    void sustain_should_transition_active_to_sustained() {
        RescueTaskEntity t = buildTask(100L, 7L, "ACTIVE");
        when(taskRepository.findById(100L)).thenReturn(Optional.of(t));

        RescueTaskEntity r = rescueTaskService.sustain(100L);

        assertThat(r.getStatus()).isEqualTo("SUSTAINED");
        verify(outboxService).publish(eq("task.sustained"), anyString(), anyString(), anyMap());
    }

    private PatientProfileEntity buildPatient(long id, String lostStatus) {
        PatientProfileEntity p = new PatientProfileEntity();
        p.setId(id); p.setLostStatus(lostStatus);
        p.setShortCode("ABCDEF");
        return p;
    }
    private RescueTaskEntity buildTask(long id, long patientId, String status) {
        RescueTaskEntity t = new RescueTaskEntity();
        t.setId(id); t.setPatientId(patientId); t.setStatus(status);
        t.setTaskNo("T00001"); t.setEventVersion(1L);
        return t;
    }
}
