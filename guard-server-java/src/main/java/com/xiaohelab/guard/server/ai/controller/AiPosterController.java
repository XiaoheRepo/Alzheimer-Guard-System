package com.xiaohelab.guard.server.ai.controller;

import com.xiaohelab.guard.server.ai.service.AiPosterService;
import com.xiaohelab.guard.server.common.annotation.Idempotent;
import com.xiaohelab.guard.server.common.dto.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "AI.Poster", description = "寻人海报生成")
@RestController
@RequestMapping("/api/v1/ai/posters")
public class AiPosterController {

    private final AiPosterService posterService;

    public AiPosterController(AiPosterService posterService) {
        this.posterService = posterService;
    }

    @PostMapping
    @Idempotent
    @Operation(summary = "为任务生成寻人海报")
    public Result<Map<String, String>> generate(@RequestBody Map<String, Object> body) {
        Long taskId = Long.valueOf(body.get("task_id").toString());
        String template = (String) body.getOrDefault("template", "default");
        String url = posterService.generate(taskId, template);
        return Result.ok(Map.of("poster_url", url));
    }
}
