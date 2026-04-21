package com.xiaohelab.guard.server.ai.service;

import com.xiaohelab.guard.server.ai.entity.AiIntentEntity;
import com.xiaohelab.guard.server.ai.repository.AiIntentRepository;
import com.xiaohelab.guard.server.common.exception.BizException;
import com.xiaohelab.guard.server.common.security.AuthUser;
import com.xiaohelab.guard.server.common.security.SecurityUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * {@link AiIntentService} 意图双重确认测试。
 */
@ExtendWith(MockitoExtension.class)
class AiIntentServiceTest {

    @Mock AiIntentRepository intentRepository;
    @InjectMocks AiIntentService intentService;

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
    void propose_should_create_pending_intent_with_expire_at_10min() {
        when(intentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AiIntentEntity i = intentService.propose("SES-1", "closeTask",
                "关闭任务", Map.of("task_id", 100L), "CONFIRM_1");

        assertThat(i.getStatus()).isEqualTo("PENDING");
        assertThat(i.getUserId()).isEqualTo(1L);
        assertThat(i.getRequiresConfirm()).isTrue();
        assertThat(i.getExpireAt()).isAfter(OffsetDateTime.now().plusMinutes(9));
    }

    @Test
    void confirm_approve_should_transition_to_approved() {
        AiIntentEntity exist = buildPending("INT-1", 1L);
        when(intentRepository.findByIntentId("INT-1")).thenReturn(Optional.of(exist));
        when(intentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AiIntentEntity r = intentService.confirm("INT-1", "APPROVE", Map.of("ok", true));

        assertThat(r.getStatus()).isEqualTo("APPROVED");
        assertThat(r.getProcessedAt()).isNotNull();
    }

    @Test
    void confirm_should_reject_when_user_is_not_owner() {
        AiIntentEntity exist = buildPending("INT-2", 999L);
        when(intentRepository.findByIntentId("INT-2")).thenReturn(Optional.of(exist));

        assertThatThrownBy(() -> intentService.confirm("INT-2", "APPROVE", null))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("E_AI_4033");
    }

    @Test
    void confirm_should_reject_when_intent_expired() {
        AiIntentEntity exist = buildPending("INT-3", 1L);
        exist.setExpireAt(OffsetDateTime.now().minusMinutes(1));
        when(intentRepository.findByIntentId("INT-3")).thenReturn(Optional.of(exist));
        when(intentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThatThrownBy(() -> intentService.confirm("INT-3", "APPROVE", null))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("E_AI_4091");
    }

    private AiIntentEntity buildPending(String id, long uid) {
        AiIntentEntity e = new AiIntentEntity();
        e.setIntentId(id);
        e.setSessionId("SES");
        e.setUserId(uid);
        e.setAction("noop");
        e.setStatus("PENDING");
        e.setExecutionLevel("CONFIRM_1");
        e.setRequiresConfirm(true);
        e.setExpireAt(OffsetDateTime.now().plusMinutes(10));
        return e;
    }
}
