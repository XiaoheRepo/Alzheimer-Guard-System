package com.xiaohelab.guard.server.media.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 直传凭证申请请求（V2.1 §3.8.3.1）。 */
public class MediaUploadSignRequest {

    @NotBlank
    @Pattern(regexp = "PATIENT_AVATAR|PATIENT_APPEARANCE|CLUE_PHOTO|POSTER_BG|USER_AVATAR",
            message = "scene 枚举非法")
    private String scene;

    @NotBlank
    @Size(max = 200)
    @JsonProperty("file_name")
    private String fileName;

    @NotBlank
    @Pattern(regexp = "image/jpeg|image/png|image/webp", message = "content_type 非白名单")
    @JsonProperty("content_type")
    private String contentType;

    @NotNull
    @JsonProperty("size_bytes")
    private Long sizeBytes;

    @JsonProperty("request_time")
    private String requestTime;

    public String getScene() { return scene; }
    public void setScene(String scene) { this.scene = scene; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public Long getSizeBytes() { return sizeBytes; }
    public void setSizeBytes(Long sizeBytes) { this.sizeBytes = sizeBytes; }
    public String getRequestTime() { return requestTime; }
    public void setRequestTime(String requestTime) { this.requestTime = requestTime; }
}
