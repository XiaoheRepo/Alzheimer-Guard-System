package com.xiaohelab.guard.server.ai.controller;

import com.xiaohelab.guard.server.ai.dto.AiChatRequest;
import com.xiaohelab.guard.server.ai.dto.AiSessionCreateRequest;
import com.xiaohelab.guard.server.ai.entity.AiSessionEntity;
import com.xiaohelab.guard.server.ai.service.AiSessionService;
import com.xiaohelab.guard.server.common.annotation.Idempotent;
import com.xiaohelab.guard.server.common.dto.CursorResponse;
import com.xiaohelab.guard.server.common.dto.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** AI 会话接口：创建会话、发送消息（同步占位版本）。 */
@Tag(name = "AI.Session", description = "AI 会话与对话")
@RestController
@RequestMapping("/api/v1/ai/sessions")
public class AiSessionController {

    private final AiSessionService sessionService;

    public AiSessionController(AiSessionService sessionService) {
        this.sessionService = sessionService;
    }

    @PostMapping
    @Idempotent
    public Result<AiSessionEntity> create(@Valid @RequestBody AiSessionCreateRequest req) {
        return Result.ok(sessionService.create(req));
    }

    @GetMapping("/{sessionId}")
    public Result<AiSessionEntity> get(@PathVariable String sessionId) {
        return Result.ok(sessionService.get(sessionId));
    }

    @PostMapping("/{sessionId}/chat")
    @Idempotent
    public Result<Map<String, Object>> chat(@PathVariable String sessionId,
                                            @Valid @RequestBody AiChatRequest req) {
        return Result.ok(sessionService.chat(sessionId, req));
    }

    @PostMapping("/{sessionId}/feedback")
    @Idempotent
    public Result<AiSessionEntity> feedback(@PathVariable String sessionId,
                                            @RequestBody Map<String, Object> body) {
        Integer rating = (Integer) body.get("rating");
        String comment = (String) body.get("comment");
        return Result.ok(sessionService.feedback(sessionId, rating, comment));
    }

    /** V2.1 §3.8.1.1：会话列表（Cursor 分页）。 */
    @GetMapping
    public Result<CursorResponse<Map<String, Object>>> list(
            @RequestParam(required = false) Long patientId,
            @RequestParam(required = false) Long taskId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int pageSize) {
        return Result.ok(sessionService.list(patientId, taskId, status, cursor, pageSize));
    }

    /** V2.1 §3.8.1.3：归档会话。 */
    @DeleteMapping("/{sessionId}")
    @Idempotent
    public Result<AiSessionEntity> archive(@PathVariable String sessionId) {
        return Result.ok(sessionService.archive(sessionId));
    }

    /** V2.1 §3.8.1.4：消息历史（Cursor 分页，direction = BACKWARD|FORWARD）。 */
    @GetMapping("/{sessionId}/messages")
    public Result<CursorResponse<Map<String, Object>>> messages(
            @PathVariable String sessionId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "BACKWARD") String direction,
            @RequestParam(defaultValue = "20") int pageSize) {
        return Result.ok(sessionService.getMessages(sessionId, cursor, direction, pageSize));
    }

    /** V2.1 §3.8.1.5：取消正在生成的消息。 */
    @PostMapping("/{sessionId}/messages/{messageId}/cancel")
    @Idempotent
    public Result<Map<String, Object>> cancelMessage(@PathVariable String sessionId,
                                                     @PathVariable String messageId) {
        return Result.ok(sessionService.cancelMessage(sessionId, messageId));
    }
}
