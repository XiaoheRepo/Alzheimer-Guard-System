package com.xiaohelab.guard.server.domain.tag.entity;

import com.xiaohelab.guard.server.infrastructure.persistence.do_.TagApplyRecordDO;
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

    public static TagApplyRecordEntity fromDO(TagApplyRecordDO d) {
        TagApplyRecordEntity e = new TagApplyRecordEntity();
        e.id = d.getId();
        e.orderNo = d.getOrderNo();
        e.patientId = d.getPatientId();
        e.applicantUserId = d.getApplicantUserId();
        e.quantity = d.getQuantity();
        e.applyNote = d.getApplyNote();
        e.tagCode = d.getTagCode();
        e.status = d.getStatus();
        e.deliveryAddress = d.getDeliveryAddress();
        e.trackingNumber = d.getTrackingNumber();
        e.courierName = d.getCourierName();
        e.resourceLink = d.getResourceLink();
        e.cancelReason = d.getCancelReason();
        e.approvedAt = d.getApprovedAt();
        e.rejectReason = d.getRejectReason();
        e.rejectedAt = d.getRejectedAt();
        e.exceptionDesc = d.getExceptionDesc();
        e.closedAt = d.getClosedAt();
        e.createdAt = d.getCreatedAt();
        e.updatedAt = d.getUpdatedAt();
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

    public TagApplyRecordDO toDO() {
        TagApplyRecordDO d = new TagApplyRecordDO();
        d.setId(this.id);
        d.setOrderNo(this.orderNo);
        d.setPatientId(this.patientId);
        d.setApplicantUserId(this.applicantUserId);
        d.setQuantity(this.quantity);
        d.setApplyNote(this.applyNote);
        d.setTagCode(this.tagCode);
        d.setStatus(this.status);
        d.setDeliveryAddress(this.deliveryAddress);
        d.setTrackingNumber(this.trackingNumber);
        d.setCourierName(this.courierName);
        d.setResourceLink(this.resourceLink);
        d.setCancelReason(this.cancelReason);
        d.setApprovedAt(this.approvedAt);
        d.setRejectReason(this.rejectReason);
        d.setRejectedAt(this.rejectedAt);
        d.setExceptionDesc(this.exceptionDesc);
        d.setClosedAt(this.closedAt);
        return d;
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
