package com.xiaohelab.guard.server.patient.dto.admin;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 强制转移主监护请求（V2.1，API §3.3.17）。仅 SUPER_ADMIN + CONFIRM_3。
 */
public class ForceTransferPrimaryRequest {

    @NotNull(message = "target_user_id 不得为空")
    @JsonProperty("target_user_id")
    @Schema(description = "新的主监护 user_id，必须是当前 ACTIVE 监护人")
    private Long targetUserId;

    @NotBlank(message = "reason 不得为空")
    @Size(min = 20, max = 256, message = "reason 长度 20-256")
    @Schema(description = "行政理由（20-256）")
    private String reason;

    @Pattern(regexp = "^https?://.{1,1023}$", message = "evidence_url 必须为 http(s) 链接")
    @JsonProperty("evidence_url")
    @Schema(description = "证据材料链接（可空）")
    private String evidenceUrl;

    public Long getTargetUserId() { return targetUserId; }
    public void setTargetUserId(Long targetUserId) { this.targetUserId = targetUserId; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getEvidenceUrl() { return evidenceUrl; }
    public void setEvidenceUrl(String evidenceUrl) { this.evidenceUrl = evidenceUrl; }
}
