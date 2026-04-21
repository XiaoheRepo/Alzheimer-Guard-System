package com.xiaohelab.guard.server.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class AiSessionCreateRequest {

    @NotNull
    @JsonProperty("patient_id")
    private Long patientId;

    @JsonProperty("task_id")
    private Long taskId;

    @Size(max = 2000)
    private String prompt;

    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }
    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }
}
