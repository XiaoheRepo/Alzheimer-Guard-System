package com.xiaohelab.guard.server.domain.material.service;

import com.xiaohelab.guard.server.common.exception.BizException;
import com.xiaohelab.guard.server.domain.material.entity.OrderStatusValue;
import com.xiaohelab.guard.server.domain.tag.entity.TagApplyRecordEntity;

/**
 * 物资工单领域服务。
 * 封装工单状态流转断言与防重复创建规则，无 IO 操作。
 */
public class MaterialOrderDomainService {

    /**
     * 断言当前工单状态可以执行申请取消（PENDING/PROCESSING → CANCEL_PENDING）。
     *
     * @throws BizException E_ORDER_4093 — 当前状态不允许取消申请
     */
    public void assertCanRequestCancel(TagApplyRecordEntity order) {
        OrderStatusValue current = OrderStatusValue.from(order.getStatus());
        current.assertCanTransitionTo(OrderStatusValue.CANCEL_PENDING);
    }

    /**
     * 断言当前工单状态为 PENDING（仅待审批时可审批通过）。
     *
     * @throws BizException E_ORDER_4093 — 工单状态不为 PENDING
     */
    public void assertIsPending(TagApplyRecordEntity order) {
        if (!OrderStatusValue.PENDING.name().equals(order.getStatus())) {
            throw BizException.of("E_ORDER_4093");
        }
    }

    /**
     * 断言指定状态流转合法（通用入口）。
     *
     * @param order  工单聚合根
     * @param target 目标状态
     * @throws BizException E_ORDER_4093 — 状态流转非法
     */
    public void assertCanTransition(TagApplyRecordEntity order, OrderStatusValue target) {
        OrderStatusValue current = OrderStatusValue.from(order.getStatus());
        current.assertCanTransitionTo(target);
    }
}
