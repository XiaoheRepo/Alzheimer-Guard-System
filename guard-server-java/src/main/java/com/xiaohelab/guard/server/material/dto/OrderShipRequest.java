package com.xiaohelab.guard.server.material.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class OrderShipRequest {

    @NotBlank
    @Size(max = 64)
    @JsonProperty("logistics_company")
    private String logisticsCompany;

    @NotBlank
    @Size(max = 64)
    @JsonProperty("logistics_no")
    private String logisticsNo;

    public String getLogisticsCompany() { return logisticsCompany; }
    public void setLogisticsCompany(String logisticsCompany) { this.logisticsCompany = logisticsCompany; }
    public String getLogisticsNo() { return logisticsNo; }
    public void setLogisticsNo(String logisticsNo) { this.logisticsNo = logisticsNo; }
}
