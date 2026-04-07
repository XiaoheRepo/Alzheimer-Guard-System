package com.xiaohelab.guard.server.infrastructure.persistence.do_;

import lombok.Data;

import java.time.Instant;

/**
 * tag_apply_record 持久化对象（物资申领工单）。
 * 状态机主流：PENDING → PROCESSING → SHIPPED → COMPLETED。
 * 终态（CANCELLED / COMPLETED）必须写入 closedAt。
 */
@Data
public class TagApplyRecordDO {

    private Long id;
    /** 工单号，格式 ORD + 时间戳 */
    private String orderNo;
    private Long patientId;
    private Long applicantUserId;
    /** 申领数量，1-20 */
    private Integer quantity;
    private String applyNote;
    /** 发货时锁定的标签码 */
    private String tagCode;
    /** PENDING / PROCESSING / CANCEL_PENDING / CANCELLED / SHIPPED / EXCEPTION / COMPLETED */
    private String status;
    private String deliveryAddress;
    private String trackingNumber;
    private String courierName;
    /** resource_link：BASE_URL/r/{resource_token} */
    private String resourceLink;
    private String cancelReason;
    private Instant approvedAt;
    private String rejectReason;
    private Instant rejectedAt;
    private String exceptionDesc;
    /** 终态关闭时间 */
    private Instant closedAt;
    private Instant createdAt;
    private Instant updatedAt;
}
