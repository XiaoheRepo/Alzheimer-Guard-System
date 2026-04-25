package com.xiaohelab.guard.server.ai.controller;

import com.xiaohelab.guard.server.ai.dto.AiChatRequest;
import com.xiaohelab.guard.server.ai.dto.AiSessionCreateRequest;
import com.xiaohelab.guard.server.ai.entity.AiSessionEntity;
import com.xiaohelab.guard.server.ai.service.AiSessionService;
import com.xiaohelab.guard.server.common.annotation.Idempotent;
import com.xiaohelab.guard.server.common.dto.CursorResponse;
import com.xiaohelab.guard.server.common.dto.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;

/** AI 会话接口（API §3.5 / V2.1 §3.8）：创建会话 / 流式发送消息（SSE） / 历史 / 归档 / 反馈。 */
@Tag(name = "AI.Session", description = "AI 会话与对话")
@RestController
@RequestMapping("/api/v1/ai/sessions")
public class AiSessionController {

    private static final Logger log = LoggerFactory.getLogger(AiSessionController.class);
    /** SSE 保活超时：5 分钟（超过后客户端应发起新请求）。 */
    private static final long SSE_TIMEOUT_MILLIS = 5 * 60 * 1000L;

    private final AiSessionService sessionService;

    public AiSessionController(AiSessionService sessionService) {
        this.sessionService = sessionService;
    }

    @PostMapping
    @Idempotent
    @Operation(summary = "3.5.1 创建 AI 会话")
    public Result<AiSessionEntity> create(@Valid @RequestBody AiSessionCreateRequest req) {
        return Result.ok(sessionService.create(req));
    }

    @GetMapping("/{sessionId}")
    @Operation(summary = "查询会话详情")
    public Result<AiSessionEntity> get(@PathVariable String sessionId) {
        return Result.ok(sessionService.get(sessionId));
    }

    /**
     * 3.5.2 canonical: 发送消息（SSE 流式）。
     * <p>当前毕设后端采用同步 LLM 桩，因此 SSE 将 reply 拆分为若干 token 事件投递，
     * 保证前端按 text/event-stream 协议解析的体验与生产路径一致。</p>
     */
    @PostMapping(value = "/{sessionId}/messages", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "3.5.2 发送消息（SSE 流式）")
    public SseEmitter sendMessage(@PathVariable String sessionId,
                                  @Valid @RequestBody AiChatRequest req) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MILLIS);
        // 同步调用 chat（已处理权限 / 配额 / 落库）
        try {
            Map<String, Object> result = sessionService.chat(sessionId, req);
            String reply = String.valueOf(result.getOrDefault("reply", ""));
            // 1. 流式切片，模拟 token-by-token 推送
            int sliceLen = 12;
            int idx = 0;
            for (int i = 0; i < reply.length(); i += sliceLen) {
                String chunk = reply.substring(i, Math.min(reply.length(), i + sliceLen));
                emitter.send(SseEmitter.event()
                        .name("token")
                        .data(Map.of("content", chunk, "index", idx++)));
            }
            // 2. usage 统计
            emitter.send(SseEmitter.event()
                    .name("usage")
                    .data(Map.of(
                            "total_tokens", result.get("total_tokens"),
                            "prompt_tokens", result.getOrDefault("prompt_tokens", 0),
                            "completion_tokens", result.getOrDefault("completion_tokens", 0),
                            "model_name", "stub-local",
                            "billing_source", "local")));
            // 3. done
            emitter.send(SseEmitter.event()
                    .name("done")
                    .data(Map.of("finish_reason", "stop")));
            // TODO(RFC-5/AI-Agent): 待 FunctionCalling Agent 能力接入后，按 API V2.0 §3.5.2
            //   在 done 之前增发 `event:tool_call`，载荷字段对齐 AiToolCallEvent
            //   （intent_id / action / description / execution_level / requires_confirm / expires_at）。
            //   目前后端未实现 Agent 推理路径，事件暂不发出；Android 端已就绪接收。
            emitter.complete();
        } catch (IOException io) {
            log.warn("[AI-SSE] send io error sessionId={} err={}", sessionId, io.getMessage());
            emitter.completeWithError(io);
        } catch (Exception ex) {
            // 非 IO 异常：发送 error 事件后结束
            try {
                emitter.send(SseEmitter.event()
                        .name("error")
                        .data(Map.of("code", "E_AI_5031", "message", ex.getMessage() == null ? "AI 服务异常" : ex.getMessage())));
            } catch (IOException ignore) { /* emitter 已断开 */ }
            emitter.completeWithError(ex);
        }
        return emitter;
    }

    /** 兼容：保留同步 /chat 路径，便于前端在 SSE 不支持环境下回退。 */
    @PostMapping("/{sessionId}/chat")
    @Idempotent
    @Operation(summary = "同步发送消息（兼容 /messages 的 JSON 回退）")
    public Result<Map<String, Object>> chat(@PathVariable String sessionId,
                                            @Valid @RequestBody AiChatRequest req) {
        return Result.ok(sessionService.chat(sessionId, req));
    }

    @PostMapping("/{sessionId}/feedback")
    @Idempotent
    @Operation(summary = "会话反馈评分")
    public Result<AiSessionEntity> feedback(@PathVariable String sessionId,
                                            @RequestBody Map<String, Object> body) {
        Integer rating = (Integer) body.get("rating");
        String comment = (String) body.get("comment");
        return Result.ok(sessionService.feedback(sessionId, rating, comment));
    }

    /** V2.1 §3.8.1.1：会话列表（Cursor 分页）。 */
    @GetMapping
    @Operation(summary = "3.8.1.1 会话列表（Cursor 分页）")
    public Result<CursorResponse<Map<String, Object>>> list(
            @RequestParam(name = "patient_id", required = false) Long patientId,
            @RequestParam(name = "task_id", required = false) Long taskId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String cursor,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize) {
        return Result.ok(sessionService.list(patientId, taskId, status, cursor, pageSize));
    }

    /** V2.1 §3.8.1.3：归档会话。 */
    @DeleteMapping("/{sessionId}")
    @Idempotent
    @Operation(summary = "3.8.1.3 归档会话")
    public Result<AiSessionEntity> archive(@PathVariable String sessionId) {
        return Result.ok(sessionService.archive(sessionId));
    }

    /** V2.1 §3.8.1.4：消息历史（Cursor 分页，direction = BACKWARD|FORWARD）。 */
    @GetMapping("/{sessionId}/messages")
    @Operation(summary = "3.8.1.4 消息历史（Cursor 分页）")
    public Result<CursorResponse<Map<String, Object>>> messages(
            @PathVariable String sessionId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "BACKWARD") String direction,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize) {
        return Result.ok(sessionService.getMessages(sessionId, cursor, direction, pageSize));
    }

    /** V2.1 §3.8.1.5：取消正在生成的消息。 */
    @PostMapping("/{sessionId}/messages/{messageId}/cancel")
    @Idempotent
    @Operation(summary = "3.8.1.5 取消正在生成的消息")
    public Result<Map<String, Object>> cancelMessage(@PathVariable String sessionId,
                                                     @PathVariable String messageId) {
        return Result.ok(sessionService.cancelMessage(sessionId, messageId));
    }
}
