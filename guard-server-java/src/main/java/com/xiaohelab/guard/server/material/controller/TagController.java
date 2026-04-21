package com.xiaohelab.guard.server.material.controller;

import com.xiaohelab.guard.server.common.annotation.Idempotent;
import com.xiaohelab.guard.server.common.dto.Result;
import com.xiaohelab.guard.server.material.entity.TagAssetEntity;
import com.xiaohelab.guard.server.material.service.TagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "Material.Tag", description = "标签生命周期：绑定 / 疑似丢失 / 确认丢失")
@RestController
@RequestMapping("/api/v1/material/tags")
public class TagController {

    private final TagService tagService;

    public TagController(TagService tagService) {
        this.tagService = tagService;
    }

    @GetMapping("/{tagCode}")
    @Operation(summary = "查询标签详情")
    public Result<TagAssetEntity> get(@PathVariable String tagCode) {
        return Result.ok(tagService.getByCode(tagCode));
    }

    @PostMapping("/{tagCode}/bind")
    @Idempotent
    @Operation(summary = "绑定标签到患者（监护人）")
    public Result<TagAssetEntity> bind(@PathVariable String tagCode,
                                       @RequestBody Map<String, Long> body) {
        return Result.ok(tagService.bind(tagCode, body.get("patient_id")));
    }

    @PostMapping("/{tagCode}/suspected-lost")
    @Idempotent
    @Operation(summary = "标签疑似丢失")
    public Result<TagAssetEntity> suspect(@PathVariable String tagCode) {
        return Result.ok(tagService.markSuspectedLost(tagCode));
    }

    @PostMapping("/{tagCode}/confirm-lost")
    @Idempotent
    @Operation(summary = "监护人确认标签丢失")
    public Result<TagAssetEntity> confirmLost(@PathVariable String tagCode,
                                              @RequestBody Map<String, String> body) {
        return Result.ok(tagService.confirmLost(tagCode, body.get("lost_reason")));
    }
}
