package com.xiaohelab.guard.server.ai.controller;

import com.xiaohelab.guard.server.ai.dto.AiChatRequest;
import com.xiaohelab.guard.server.ai.dto.AiSessionCreateRequest;
import com.xiaohelab.guard.server.ai.entity.AiSessionEntity;
import com.xiaohelab.guard.server.ai.service.AiSessionService;
import com.xiaohelab.guard.server.common.annotation.Idempotent;
import com.xiaohelab.guard.server.common.dto.Result;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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
}
