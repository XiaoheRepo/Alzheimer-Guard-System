package com.xiaohelab.guard.server.material.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 物流异常处置请求（API V2.0 §3.4.12，LLD §6.3.8，FR-MAT-004）。
 * <p>- {@code RESHIP}：{@code tracking_no} + {@code carrier} 必填（在 Service 层二次校验，避免 action=VOID 时的无意义约束）。
 * <br>- {@code VOID}：直接作废，仅需 reason。</p>
 */
public class OrderResolveExceptionRequest {

    @NotBlank(message = "action 不得为空")
    @Pattern(regexp = "^(RESHIP|VOID)$", message = "action 仅支持 RESHIP / VOID")
    @Schema(description = "处置动作", example = "RESHIP")
    private String action;

    @NotBlank(message = "reason 不得为空")
    @Size(min = 10, max = 200, message = "reason 长度 10-200")
    @Schema(description = "行政理由（10-200）", example = "快递公司确认丢件,补发新单号")
    private String reason;

    @Size(min = 6, max = 32)
    @JsonProperty("tracking_no")
    @Schema(description = "action=RESHIP 时必填；6-32", example = "SF1234567890")
    private String trackingNo;

    @Pattern(regexp = "^(SF|YTO|ZTO|JD|EMS|OTHER)$", message = "carrier 枚举非法")
    @Schema(description = "action=RESHIP 时必填", example = "SF")
    private String carrier;

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getTrackingNo() { return trackingNo; }
    public void setTrackingNo(String trackingNo) { this.trackingNo = trackingNo; }
    public String getCarrier() { return carrier; }
    public void setCarrier(String carrier) { this.carrier = carrier; }
}
