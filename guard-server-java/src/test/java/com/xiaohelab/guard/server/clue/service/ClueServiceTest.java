package com.xiaohelab.guard.server.clue.service;

import com.xiaohelab.guard.server.clue.dto.ClueReportRequest;
import com.xiaohelab.guard.server.clue.dto.ClueReviewRequest;
import com.xiaohelab.guard.server.clue.entity.ClueRecordEntity;
import com.xiaohelab.guard.server.clue.repository.ClueRecordRepository;
import com.xiaohelab.guard.server.common.error.ErrorCode;
import com.xiaohelab.guard.server.common.exception.BizException;
import com.xiaohelab.guard.server.common.security.AuthUser;
import com.xiaohelab.guard.server.common.security.SecurityUtil;
import com.xiaohelab.guard.server.outbox.service.OutboxService;
import com.xiaohelab.guard.server.patient.service.GuardianAuthorizationService;
import com.xiaohelab.guard.server.rescue.repository.RescueTaskRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * {@link ClueService} 单元测试：家属上报、管理员 override/reject。
 */
@ExtendWith(MockitoExtension.class)
class ClueServiceTest {

    @Mock ClueRecordRepository clueRepository;
    @Mock RescueTaskRepository taskRepository;
    @Mock GuardianAuthorizationService authorizationService;
    @Mock OutboxService outboxService;
    @InjectMocks ClueService clueService;

    private MockedStatic<SecurityUtil> secMock;

    @AfterEach
    void tearDown() { if (secMock != null) secMock.close(); }

    @Test
    void familyReport_should_persist_and_publish_clue_validated() {
        secMock = Mockito.mockStatic(SecurityUtil.class);
        AuthUser family = new AuthUser(1L, "alice", "FAMILY");
        secMock.when(SecurityUtil::current).thenReturn(family);

        ClueReportRequest req = new ClueReportRequest();
        req.setPatientId(7L);
        req.setLatitude(39.9); req.setLongitude(116.4); req.setCoordSystem("WGS84");
        req.setDescription("看到疑似老人");
        req.setPhotoUrls(List.of("https://img/1.jpg"));

        when(clueRepository.save(any())).thenAnswer(inv -> {
            ClueRecordEntity c = inv.getArgument(0); c.setId(555L); return c;
        });

        ClueRecordEntity c = clueService.familyReport(req, "127.0.0.1");

        assertThat(c.getStatus()).isEqualTo("VALID");
        assertThat(c.getCoordSystem()).isEqualTo("WGS84");
        assertThat(c.getClueNo()).startsWith("C");
        verify(outboxService).publish(eq("clue.validated"), anyString(), anyString(), anyMap());
    }

    @Test
    void review_override_should_require_admin() {
        secMock = Mockito.mockStatic(SecurityUtil.class);
        AuthUser family = new AuthUser(1L, "alice", "FAMILY");
        secMock.when(SecurityUtil::current).thenReturn(family);

        ClueReviewRequest req = new ClueReviewRequest();
        req.setAction("OVERRIDE"); req.setReason("人工确认");

        assertThatThrownBy(() -> clueService.review(999L, req))
                .isInstanceOf(BizException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.E_GOV_4030);
    }

    @Test
    void review_should_reject_non_suspect_clue() {
        secMock = Mockito.mockStatic(SecurityUtil.class);
        AuthUser admin = new AuthUser(9L, "admin", "ADMIN");
        secMock.when(SecurityUtil::current).thenReturn(admin);

        ClueRecordEntity exist = new ClueRecordEntity();
        exist.setId(10L); exist.setPatientId(7L);
        exist.setSuspectFlag(false);
        when(clueRepository.findById(10L)).thenReturn(Optional.of(exist));

        ClueReviewRequest req = new ClueReviewRequest();
        req.setAction("OVERRIDE"); req.setReason("t");

        assertThatThrownBy(() -> clueService.review(10L, req))
                .isInstanceOf(BizException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.E_CLUE_4221);
    }

    @Test
    void review_override_should_transition_status_and_publish_event() {
        secMock = Mockito.mockStatic(SecurityUtil.class);
        AuthUser admin = new AuthUser(9L, "admin", "ADMIN");
        secMock.when(SecurityUtil::current).thenReturn(admin);

        ClueRecordEntity exist = new ClueRecordEntity();
        exist.setId(10L); exist.setPatientId(7L);
        exist.setSuspectFlag(true); exist.setReviewStatus("PENDING");
        exist.setClueNo("C0001");
        when(clueRepository.findById(10L)).thenReturn(Optional.of(exist));

        ClueReviewRequest req = new ClueReviewRequest();
        req.setAction("OVERRIDE"); req.setReason("人工复核通过");

        ClueRecordEntity r = clueService.review(10L, req);

        assertThat(r.getReviewStatus()).isEqualTo("OVERRIDDEN");
        assertThat(r.getStatus()).isEqualTo("OVERRIDDEN");
        verify(outboxService).publish(eq("clue.overridden"), anyString(), anyString(), anyMap());
    }
}
