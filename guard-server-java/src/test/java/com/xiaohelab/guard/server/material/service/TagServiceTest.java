package com.xiaohelab.guard.server.material.service;

import com.xiaohelab.guard.server.common.error.ErrorCode;
import com.xiaohelab.guard.server.common.exception.BizException;
import com.xiaohelab.guard.server.common.security.AuthUser;
import com.xiaohelab.guard.server.common.security.SecurityUtil;
import com.xiaohelab.guard.server.material.entity.TagAssetEntity;
import com.xiaohelab.guard.server.material.repository.TagAssetRepository;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * {@link TagService} 单元测试：标签绑定 / 疑似丢失 / 确认丢失。
 */
@ExtendWith(MockitoExtension.class)
class TagServiceTest {

    @Mock TagAssetRepository tagRepository;
    @Mock PatientProfileRepository patientRepository;
    @Mock GuardianAuthorizationService authorizationService;
    @Mock OutboxService outboxService;

    @InjectMocks TagService tagService;

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
    void bind_should_transition_allocated_to_bound() {
        TagAssetEntity t = new TagAssetEntity();
        t.setTagCode("TAG-001");
        t.setStatus("ALLOCATED");
        when(tagRepository.findByTagCode("TAG-001")).thenReturn(Optional.of(t));
        PatientProfileEntity p = new PatientProfileEntity();
        p.setId(7L); p.setShortCode("ABCDEFGH");
        when(authorizationService.assertGuardian(user, 7L)).thenReturn(p);
        when(tagRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TagAssetEntity r = tagService.bind("TAG-001", 7L);

        assertThat(r.getStatus()).isEqualTo("BOUND");
        assertThat(r.getPatientId()).isEqualTo(7L);
        assertThat(r.getResourceToken()).isNotBlank();
        assertThat(r.getBoundAt()).isNotNull();
        verify(outboxService).publish(eq("tag.bound"), anyString(), anyString(), anyMap());
    }

    @Test
    void bind_should_reject_when_status_is_not_allocated() {
        TagAssetEntity t = new TagAssetEntity();
        t.setTagCode("TAG-002");
        t.setStatus("BOUND");
        when(tagRepository.findByTagCode("TAG-002")).thenReturn(Optional.of(t));
        when(authorizationService.assertGuardian(user, 7L)).thenReturn(new PatientProfileEntity());

        assertThatThrownBy(() -> tagService.bind("TAG-002", 7L))
                .isInstanceOf(BizException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.E_MAT_4091);
    }

    @Test
    void confirmLost_should_transition_bound_to_lost() {
        TagAssetEntity t = new TagAssetEntity();
        t.setTagCode("TAG-003");
        t.setStatus("BOUND");
        t.setPatientId(7L);
        when(tagRepository.findByTagCode("TAG-003")).thenReturn(Optional.of(t));
        when(tagRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TagAssetEntity r = tagService.confirmLost("TAG-003", "老人外出遗失");

        assertThat(r.getStatus()).isEqualTo("LOST");
        assertThat(r.getLostAt()).isNotNull();
        verify(outboxService).publish(eq("tag.lost"), anyString(), anyString(), anyMap());
    }
}
