package com.xiaohelab.guard.server.material.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;

public class OrderCreateRequest {

    @NotNull
    @JsonProperty("patient_id")
    private Long patientId;

    @NotBlank
    @Pattern(regexp = "QR_CODE|NFC")
    @JsonProperty("tag_type")
    private String tagType;

    @NotNull @Min(1) @Max(50)
    private Integer quantity;

    @Size(max = 500)
    private String remark;

    @Size(max = 64) @JsonProperty("shipping_province")
    private String shippingProvince;
    @Size(max = 64) @JsonProperty("shipping_city")
    private String shippingCity;
    @Size(max = 64) @JsonProperty("shipping_district")
    private String shippingDistrict;
    @Size(max = 512) @JsonProperty("shipping_detail")
    private String shippingDetail;
    @Size(max = 64) @JsonProperty("shipping_receiver")
    private String shippingReceiver;
    @Size(max = 32) @JsonProperty("shipping_phone")
    private String shippingPhone;

    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public String getTagType() { return tagType; }
    public void setTagType(String tagType) { this.tagType = tagType; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
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
}
