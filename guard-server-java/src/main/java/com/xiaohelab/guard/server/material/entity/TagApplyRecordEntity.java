package com.xiaohelab.guard.server.material.entity;

import com.xiaohelab.guard.server.common.entity.BaseEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

@Entity
@Table(name = "tag_apply_record")
public class TagApplyRecordEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_no", length = 32, unique = true)
    private String orderNo;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "applicant_user_id", nullable = false)
    private Long applicantUserId;

    /** QR_CODE / NFC */
    @Column(name = "tag_type", length = 20, nullable = false)
    private String tagType;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "remark", length = 500)
    private String remark;

    /** PENDING_AUDIT / PENDING_SHIP / SHIPPED / RECEIVED / EXCEPTION / REJECTED / CANCELLED / VOIDED */
    @Column(name = "status", length = 32, nullable = false)
    private String status = "PENDING_AUDIT";

    @Column(name = "shipping_province", length = 64)
    private String shippingProvince;

    @Column(name = "shipping_city", length = 64)
    private String shippingCity;

    @Column(name = "shipping_district", length = 64)
    private String shippingDistrict;

    @Column(name = "shipping_detail", length = 512)
    private String shippingDetail;

    @Column(name = "shipping_receiver", length = 64)
    private String shippingReceiver;

    @Column(name = "shipping_phone", length = 32)
    private String shippingPhone;

    @Column(name = "reviewer_user_id")
    private Long reviewerUserId;

    @Column(name = "reviewed_at")
    private OffsetDateTime reviewedAt;

    @Column(name = "reject_reason", length = 256)
    private String rejectReason;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tag_codes", columnDefinition = "jsonb")
    private String tagCodes;

    @Column(name = "logistics_no", length = 64)
    private String logisticsNo;

    @Column(name = "logistics_company", length = 64)
    private String logisticsCompany;

    @Column(name = "shipped_at")
    private OffsetDateTime shippedAt;

    @Column(name = "received_at")
    private OffsetDateTime receivedAt;

    @Column(name = "cancel_reason", length = 256)
    private String cancelReason;

    @Column(name = "cancelled_at")
    private OffsetDateTime cancelledAt;

    @Column(name = "exception_desc", length = 512)
    private String exceptionDesc;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public Long getApplicantUserId() { return applicantUserId; }
    public void setApplicantUserId(Long applicantUserId) { this.applicantUserId = applicantUserId; }
    public String getTagType() { return tagType; }
    public void setTagType(String tagType) { this.tagType = tagType; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getShippingProvince() { return shippingProvince; }
    public void setShippingProvince(String shippingProvince) { this.shippingProvince = shippingProvince; }
    public String getShippingCity() { return shippingCity; }
    public void setShippingCity(String shippingCity) { this.shippingCity = shippingCity; }
    public String getShippingDistrict() { return shippingDistrict; }
    public void setShippingDistrict(String shippingDistrict) { this.shippingDistrict = shippingDistrict; }
    public String getShippingDetail() { return shippingDetail; }
    public void setShippingDetail(String shippingDetail) { this.shippingDetail = shippingDetail; }
    public String getShippingReceiver() { return shippingReceiver; }
    public void setShippingReceiver(String shippingReceiver) { this.shippingReceiver = shippingReceiver; }
    public String getShippingPhone() { return shippingPhone; }
    public void setShippingPhone(String shippingPhone) { this.shippingPhone = shippingPhone; }
    public Long getReviewerUserId() { return reviewerUserId; }
    public void setReviewerUserId(Long reviewerUserId) { this.reviewerUserId = reviewerUserId; }
    public OffsetDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(OffsetDateTime reviewedAt) { this.reviewedAt = reviewedAt; }
    public String getRejectReason() { return rejectReason; }
    public void setRejectReason(String rejectReason) { this.rejectReason = rejectReason; }
    public String getTagCodes() { return tagCodes; }
    public void setTagCodes(String tagCodes) { this.tagCodes = tagCodes; }
    public String getLogisticsNo() { return logisticsNo; }
    public void setLogisticsNo(String logisticsNo) { this.logisticsNo = logisticsNo; }
    public String getLogisticsCompany() { return logisticsCompany; }
    public void setLogisticsCompany(String logisticsCompany) { this.logisticsCompany = logisticsCompany; }
    public OffsetDateTime getShippedAt() { return shippedAt; }
    public void setShippedAt(OffsetDateTime shippedAt) { this.shippedAt = shippedAt; }
    public OffsetDateTime getReceivedAt() { return receivedAt; }
    public void setReceivedAt(OffsetDateTime receivedAt) { this.receivedAt = receivedAt; }
    public String getCancelReason() { return cancelReason; }
    public void setCancelReason(String cancelReason) { this.cancelReason = cancelReason; }
    public OffsetDateTime getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(OffsetDateTime cancelledAt) { this.cancelledAt = cancelledAt; }
    public String getExceptionDesc() { return exceptionDesc; }
    public void setExceptionDesc(String exceptionDesc) { this.exceptionDesc = exceptionDesc; }
}
