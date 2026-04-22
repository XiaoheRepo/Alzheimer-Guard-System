package com.xiaohelab.guard.server.material.service;

import com.xiaohelab.guard.server.common.error.ErrorCode;
import com.xiaohelab.guard.server.common.exception.BizException;
import com.xiaohelab.guard.server.common.security.AuthUser;
import com.xiaohelab.guard.server.common.security.SecurityUtil;
import com.xiaohelab.guard.server.material.dto.OrderCreateRequest;
import com.xiaohelab.guard.server.material.dto.OrderReviewRequest;
import com.xiaohelab.guard.server.material.entity.TagApplyRecordEntity;
import com.xiaohelab.guard.server.material.entity.TagAssetEntity;
import com.xiaohelab.guard.server.material.repository.TagApplyRecordRepository;
import com.xiaohelab.guard.server.material.repository.TagAssetRepository;
import com.xiaohelab.guard.server.outbox.service.OutboxService;
import com.xiaohelab.guard.server.patient.service.GuardianAuthorizationService;
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
 * {@link MaterialOrderService} 单元测试：申请 / 审批 / 库存不足。
 */
@ExtendWith(MockitoExtension.class)
class MaterialOrderServiceTest {

    @Mock TagApplyRecordRepository orderRepository;
    @Mock TagAssetRepository tagRepository;
    @Mock GuardianAuthorizationService authorizationService;
    @Mock OutboxService outboxService;
    @InjectMocks MaterialOrderService orderService;

    private MockedStatic<SecurityUtil> secMock;

    @AfterEach
    void tearDown() { if (secMock != null) secMock.close(); }

    @Test
    void create_should_persist_pending_audit_order() {
        secMock = Mockito.mockStatic(SecurityUtil.class);
        AuthUser family = new AuthUser(1L, "alice", "FAMILY");
        secMock.when(SecurityUtil::current).thenReturn(family);

        OrderCreateRequest req = new OrderCreateRequest();
        req.setPatientId(7L); req.setTagType("STICKER"); req.setQuantity(3);
        req.setShippingProvince("京"); req.setShippingCity("京");
        req.setShippingDistrict("海淀"); req.setShippingDetail("X 路 1 号");
        req.setShippingReceiver("张三"); req.setShippingPhone("13812345678");

        TagApplyRecordEntity o = orderService.create(req);

        assertThat(o.getStatus()).isEqualTo("PENDING_AUDIT");
        assertThat(o.getOrderNo()).startsWith("O");
        verify(outboxService).publish(eq("material.order.created"), anyString(), anyString(), anyMap());
    }

    @Test
    void review_approve_should_require_admin() {
        secMock = Mockito.mockStatic(SecurityUtil.class);
        AuthUser family = new AuthUser(1L, "alice", "FAMILY");
        secMock.when(SecurityUtil::current).thenReturn(family);

        OrderReviewRequest req = new OrderReviewRequest();
        req.setAction("APPROVE");

        assertThatThrownBy(() -> orderService.review(10L, req))
                .isInstanceOf(BizException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.E_MAT_4030);
    }

    @Test
    void review_approve_should_reject_when_inventory_insufficient() {
        secMock = Mockito.mockStatic(SecurityUtil.class);
        AuthUser admin = new AuthUser(9L, "admin", "ADMIN");
        secMock.when(SecurityUtil::current).thenReturn(admin);

        TagApplyRecordEntity o = new TagApplyRecordEntity();
        o.setId(10L); o.setTagType("STICKER"); o.setQuantity(5);
        o.setStatus("PENDING_AUDIT"); o.setPatientId(7L);
        o.setOrderNo("O001");
        when(orderRepository.findById(10L)).thenReturn(Optional.of(o));
        // 库存仅返回 2 条 < 5
        when(tagRepository.claimUnbound("STICKER", 5)).thenReturn(List.of(new TagAssetEntity(), new TagAssetEntity()));

        OrderReviewRequest req = new OrderReviewRequest();
        req.setAction("APPROVE");

        assertThatThrownBy(() -> orderService.review(10L, req))
                .isInstanceOf(BizException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.E_MAT_4221);
    }

    @Test
    void review_reject_should_mark_order_rejected_and_publish() {
        secMock = Mockito.mockStatic(SecurityUtil.class);
        AuthUser admin = new AuthUser(9L, "admin", "ADMIN");
        secMock.when(SecurityUtil::current).thenReturn(admin);

        TagApplyRecordEntity o = new TagApplyRecordEntity();
        o.setId(10L); o.setQuantity(2); o.setTagType("STICKER");
        o.setStatus("PENDING_AUDIT"); o.setPatientId(7L);
        o.setOrderNo("O001");
        when(orderRepository.findById(10L)).thenReturn(Optional.of(o));

        OrderReviewRequest req = new OrderReviewRequest();
        req.setAction("REJECT"); req.setReason("不符合条件");

        TagApplyRecordEntity r = orderService.review(10L, req);

        assertThat(r.getStatus()).isEqualTo("REJECTED");
        verify(outboxService).publish(eq("material.order.rejected"), anyString(), anyString(), anyMap());
    }
}
