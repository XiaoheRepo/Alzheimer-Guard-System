package com.xiaohelab.guard.server.domain.material.entity;

import com.xiaohelab.guard.server.common.exception.BizException;

import java.util.Map;
import java.util.Set;

/**
 * 物资工单状态值对象。
 * 封装工单状态机的所有合法状态及状态流转规则（无 IO）。
 * 状态主流：PENDING → PROCESSING → SHIPPED → COMPLETED
 * 取消路径：PENDING/PROCESSING → CANCEL_PENDING → CANCELLED
 * 异常路径：SHIPPED → EXCEPTION → SHIPPED（重发）/ COMPLETED（强关）
 */
public enum OrderStatusValue {

    PENDING, PROCESSING, CANCEL_PENDING, CANCELLED, SHIPPED, EXCEPTION, COMPLETED;

    private static final Map<OrderStatusValue, Set<OrderStatusValue>> TRANSITIONS = Map.of(
            PENDING,        Set.of(PROCESSING, CANCEL_PENDING),
            PROCESSING,     Set.of(SHIPPED, CANCEL_PENDING),
            CANCEL_PENDING, Set.of(CANCELLED, PROCESSING),
            SHIPPED,        Set.of(COMPLETED, EXCEPTION),
            EXCEPTION,      Set.of(SHIPPED, COMPLETED)
    );

    /**
     * 断言状态流转合法。
     *
     * @throws BizException E_ORDER_4093 — 当前状态不允许该流转
     */
    public void assertCanTransitionTo(OrderStatusValue target) {
        Set<OrderStatusValue> allowed = TRANSITIONS.getOrDefault(this, Set.of());
        if (!allowed.contains(target)) {
            throw BizException.of("E_ORDER_4093");
        }
    }

    /**
     * 判断工单是否处于"进行中"（用于防重复创建校验）。
     */
    public boolean isOpen() {
        return this == PENDING || this == PROCESSING
                || this == CANCEL_PENDING || this == SHIPPED || this == EXCEPTION;
    }

    /**
     * 从字符串解析状态值。
     *
     * @throws BizException E_ORDER_4001 — 状态值非法
     */
    public static OrderStatusValue from(String status) {
        try {
            return valueOf(status);
        } catch (IllegalArgumentException e) {
            throw BizException.of("E_ORDER_4001");
        }
    }
}
