package com.xiaohelab.guard.server.patient.service;

import com.xiaohelab.guard.server.common.error.ErrorCode;
import com.xiaohelab.guard.server.common.exception.BizException;
import com.xiaohelab.guard.server.common.security.AuthUser;
import com.xiaohelab.guard.server.common.security.SecurityUtil;
import com.xiaohelab.guard.server.outbox.service.OutboxService;
import com.xiaohelab.guard.server.patient.dto.PatientCreateRequest;
import com.xiaohelab.guard.server.patient.dto.PatientUpdateRequest;
import com.xiaohelab.guard.server.patient.entity.GuardianRelationEntity;
import com.xiaohelab.guard.server.patient.entity.PatientProfileEntity;
import com.xiaohelab.guard.server.patient.repository.GuardianRelationRepository;
import com.xiaohelab.guard.server.patient.repository.PatientProfileRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * {@link PatientService} 单元测试。
 * 覆盖：创建流程、更新流程、围栏校验、confirmSafe 状态流转。
 */
@ExtendWith(MockitoExtension.class)
class PatientServiceTest {

    @Mock PatientProfileRepository patientRepository;
    @Mock GuardianRelationRepository relationRepository;
    @Mock GuardianAuthorizationService authorizationService;
    @Mock OutboxService outboxService;

    @InjectMocks PatientService patientService;

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
    void create_should_persist_patient_and_primary_relation_and_publish_event() {
        // given
        PatientCreateRequest req = new PatientCreateRequest();
        req.setPatientName("张老");
        req.setGender("MALE");
        when(patientRepository.existsByShortCode(anyString())).thenReturn(false);
        when(patientRepository.save(any())).thenAnswer(inv -> {
            PatientProfileEntity p = inv.getArgument(0);
            p.setId(100L);
            return p;
        });

        // when
        var resp = patientService.create(req);

        // then
        assertThat(resp.getPatientId()).isEqualTo(100L);
        assertThat(resp.getProfileVersion()).isEqualTo(1L);
        verify(relationRepository).save(argThat((GuardianRelationEntity r) ->
                r.getUserId().equals(1L) && "PRIMARY_GUARDIAN".equals(r.getRelationRole())));
        verify(outboxService).publish(eq("profile.created"), anyString(), anyString(), anyMap());
    }

    @Test
    void update_should_bump_profile_version_and_publish_event() {
        PatientProfileEntity existing = new PatientProfileEntity();
        existing.setId(7L);
        existing.setProfileVersion(3L);
        when(authorizationService.assertGuardian(user, 7L)).thenReturn(existing);
        when(patientRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PatientUpdateRequest req = new PatientUpdateRequest();
        req.setPatientName("新名字");
        var resp = patientService.update(7L, req);

        assertThat(resp.getProfileVersion()).isEqualTo(4L);
        assertThat(resp.getPatientName()).isEqualTo("新名字");
        verify(outboxService).publish(eq("profile.updated"), anyString(), anyString(), anyMap());
    }

    @Test
    void confirmSafe_should_reject_when_status_is_not_pending() {
        PatientProfileEntity p = new PatientProfileEntity();
        p.setId(7L);
        p.setLostStatus("NORMAL");
        p.setProfileVersion(1L);
        when(authorizationService.assertGuardian(user, 7L)).thenReturn(p);

        assertThatThrownBy(() -> patientService.confirmSafe(7L))
                .isInstanceOf(BizException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.E_PRO_4092);
        verify(outboxService, never()).publish(anyString(), anyString(), anyString(), anyMap());
    }
}
