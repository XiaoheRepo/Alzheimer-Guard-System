package com.xiaohelab.guard.server.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 创建 AI 会话请求（API V2.0 §3.5.1）。
 * <p>基线契约：</p>
 * <ul>
 *   <li>{@code patient_id}：必填（string on the wire，由 Jackson 自动 String→Long 反序列化）。</li>
 *   <li>{@code task_id}：必填（API V2.0 §3.5.1 字段表）。AI 会话必须挂在某条寻回任务上下文之下。</li>
 * </ul>
 * <p>注：JSON wire 上 ID 为 string；Jackson 默认 ALLOW_COERCION_OF_SCALARS=true 接受 "123" 解为 Long。</p>
 */
public class AiSessionCreateRequest {

    @NotNull
    @JsonProperty("patient_id")
    private Long patientId;

    /** API V2.0 §3.5.1：task_id 必填。AI 会话必须关联到具体寻回任务。 */
    @NotNull
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
