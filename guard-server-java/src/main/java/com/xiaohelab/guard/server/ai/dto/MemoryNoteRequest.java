package com.xiaohelab.guard.server.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;

import java.util.Map;

public class MemoryNoteRequest {

    @NotNull
    @JsonProperty("patient_id")
    private Long patientId;

    @NotBlank
    @Pattern(regexp = "HABIT|PLACE|PREFERENCE|SAFETY_CUE|RESCUE_CASE")
    private String kind;

    @NotBlank
    @Size(max = 2000)
    private String content;

    private Map<String, Object> tags;

    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public String getKind() { return kind; }
    public void setKind(String kind) { this.kind = kind; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Map<String, Object> getTags() { return tags; }
    public void setTags(Map<String, Object> tags) { this.tags = tags; }
}
