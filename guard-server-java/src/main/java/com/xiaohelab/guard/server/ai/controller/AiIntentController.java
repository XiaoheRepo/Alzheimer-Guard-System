package com.xiaohelab.guard.server.ai.controller;

import com.xiaohelab.guard.server.ai.entity.AiIntentEntity;
import com.xiaohelab.guard.server.ai.service.AiIntentService;
import com.xiaohelab.guard.server.common.annotation.Idempotent;
import com.xiaohelab.guard.server.common.dto.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "AI.Intent", description = "AI Agent 意图双重确认")
@RestController
@RequestMapping("/api/v1/ai/intents")
public class AiIntentController {

    private final AiIntentService intentService;

    public AiIntentController(AiIntentService intentService) {
        this.intentService = intentService;
    }

    @PostMapping
    @Idempotent
    @Operation(summary = "创建 AI 意图（一般由 Agent 内部触发）")
    @SuppressWarnings("unchecked")
    public Result<AiIntentEntity> propose(@RequestBody Map<String, Object> body) {
        return Result.ok(intentService.propose(
                (String) body.get("session_id"),
                (String) body.get("action"),
                (String) body.get("description"),
                (Map<String, Object>) body.get("parameters"),
                (String) body.get("execution_level")
        ));
    }

    @GetMapping("/{intentId}")
    @Operation(summary = "查询意图详情")
    public Result<AiIntentEntity> get(@PathVariable String intentId) {
        return Result.ok(intentService.get(intentId));
    }

    @PostMapping("/{intentId}/confirm")
    @Idempotent
    @Operation(summary = "确认或拒绝 AI 意图")
    @SuppressWarnings("unchecked")
    public Result<AiIntentEntity> confirm(@PathVariable String intentId,
                                          @RequestBody Map<String, Object> body) {
        return Result.ok(intentService.confirm(
                intentId,
                (String) body.get("decision"),
                (Map<String, Object>) body.get("execution_result")
        ));
    }
}
