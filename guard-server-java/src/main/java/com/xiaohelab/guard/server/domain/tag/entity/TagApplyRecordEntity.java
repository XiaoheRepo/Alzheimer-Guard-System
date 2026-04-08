package com.xiaohelab.guard.server.domain.tag.entity;

import lombok.Getter;

import java.time.Instant;

/**
 * 物资申领工单聚合根。
 * 状态机主流：PENDING → PROCESSING → SHIPPED → COMPLETED
 * 取消路径：PENDING/PROCESSING → CANCEL_PENDING → CANCELLED
 * 异常路径：SHIPPED → EXCEPTION → SHIPPED（重发）或 → COMPLETED（强关）
 */
@Getter
public class TagApplyRecordEntity {

    private Long id;
    private String orderNo;
    private Long patientId;
    private Long applicantUserId;
    private Integer quantity;
    private String applyNote;
    private String tagCode;
    /** PENDING / PROCESSING / CANCEL_PENDING / CANCELLED / SHIPPED / EXCEPTION / COMPLETED */
    private String status;
    private String deliveryAddress;
    private String trackingNumber;
    private String courierName;
    private String resourceLink;
    private String cancelReason;
    private Instant approvedAt;
    private String rejectReason;
    private Instant rejectedAt;
    private String exceptionDesc;
    private Instant closedAt;
    private Instant createdAt;
    private Instant updatedAt;

    private TagApplyRecordEntity() {}

    /** 从持久化数据重建（仅 Infrastructure 层 RepositoryImpl 调用）。 */
    public static TagApplyRecordEntity reconstitute(
            Long id, String orderNo, Long patientId, Long applicantUserId,
            Integer quantity, String applyNote, String tagCode, String status,
            String deliveryAddress, String trackingNumber, String courierName, String resourceLink,
            String cancelReason, Instant approvedAt, String rejectReason, Instant rejectedAt,
            String exceptionDesc, Instant closedAt, Instant createdAt, Instant updatedAt) {
        TagApplyRecordEntity e = new TagApplyRecordEntity();
        e.id = id;
        e.orderNo = orderNo;
        e.patientId = patientId;
        e.applicantUserId = applicantUserId;
        e.quantity = quantity;
        e.applyNote = applyNote;
        e.tagCode = tagCode;
        e.status = status;
        e.deliveryAddress = deliveryAddress;
        e.trackingNumber = trackingNumber;
        e.courierName = courierName;
        e.resourceLink = resourceLink;
        e.cancelReason = cancelReason;
        e.approvedAt = approvedAt;
        e.rejectReason = rejectReason;
        e.rejectedAt = rejectedAt;
        e.exceptionDesc = exceptionDesc;
        e.closedAt = closedAt;
        e.createdAt = createdAt;
        e.updatedAt = updatedAt;
        return e;
    }

    public static TagApplyRecordEntity create(String orderNo, Long patientId, Long applicantUserId,
                                              Integer quantity, String applyNote, String deliveryAddress) {
        TagApplyRecordEntity e = new TagApplyRecordEntity();
        e.orderNo = orderNo;
        e.patientId = patientId;
        e.applicantUserId = applicantUserId;
        e.quantity = quantity;
        e.applyNote = applyNote;
        e.status = "PENDING";
        e.deliveryAddress = deliveryAddress;
        return e;
    }

    // ===== 状态机业务方法 =====

    public void approve() {
        this.status = "PROCESSING";
        this.approvedAt = Instant.now();
    }

    public void requestCancel(String reason) {
        this.status = "CANCEL_PENDING";
        this.cancelReason = reason;
    }

    public void approveCancelRequest() {
        this.status = "CANCELLED";
        this.closedAt = Instant.now();
    }

    public void rejectCancelRequest() {
        this.status = "PROCESSING";
    }

    public void ship(String tagCode, String trackingNumber, String courierName, String resourceLink) {
        this.status = "SHIPPED";
        this.tagCode = tagCode;
        this.trackingNumber = trackingNumber;
        this.courierName = courierName;
        this.resourceLink = resourceLink;
    }

    public void confirmReceipt() {
        this.status = "COMPLETED";
        this.closedAt = Instant.now();
    }

    public void markException(String exceptionDesc) {
        this.status = "EXCEPTION";
        this.exceptionDesc = exceptionDesc;
    }

    public void reship(String trackingNumber, String courierName) {
        this.status = "SHIPPED";
        this.trackingNumber = trackingNumber;
        this.courierName = courierName;
    }

    public void closeException() {
        this.status = "COMPLETED";
        this.closedAt = Instant.now();
    }
}
