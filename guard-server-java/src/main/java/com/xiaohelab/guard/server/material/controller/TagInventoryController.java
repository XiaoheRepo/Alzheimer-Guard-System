package com.xiaohelab.guard.server.material.controller;

import com.xiaohelab.guard.server.common.annotation.Idempotent;
import com.xiaohelab.guard.server.common.dto.Result;
import com.xiaohelab.guard.server.material.service.TagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 标签批量发号 / 库存摘要（API V2.0 §3.4.8-3.4.10）。
 * <p>注意路径为 {@code /api/v1/tags}（不同于 {@code /api/v1/material/tags}）。</p>
 */
@Tag(name = "Material.Tag.Admin", description = "标签发号与库存（管理员）")
@RestController
@RequestMapping("/api/v1/tags")
public class TagInventoryController {

    private final TagService tagService;

    public TagInventoryController(TagService tagService) {
        this.tagService = tagService;
    }

    /** 3.4.8 批量发号。 */
    @PostMapping("/batch-generate")
    @Idempotent
    @Operation(summary = "3.4.8 批量发号（ADMIN）")
    public ResponseEntity<Result<Map<String, Object>>> batchGenerate(@Valid @RequestBody BatchGenerateReq req) {
        Map<String, Object> job = tagService.batchGenerate(req.getTagType(), req.getQuantity(), req.getBatchKeyId());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(Result.ok(job));
    }

    /** 3.4.9 查询发号任务。 */
    @GetMapping("/batch-generate/jobs/{jobId}")
    @Operation(summary = "3.4.9 查询发号任务（ADMIN）")
    public Result<Map<String, Object>> queryJob(@PathVariable String jobId) {
        return Result.ok(tagService.queryBatchJob(jobId));
    }

    /** 3.4.10 库存摘要。 */
    @GetMapping("/inventory/summary")
    @Operation(summary = "3.4.10 库存摘要（ADMIN）")
    public Result<Map<String, Object>> inventorySummary() {
        return Result.ok(tagService.inventorySummary());
    }

    /** 批量发号入参。 */
    public static class BatchGenerateReq {
        @NotBlank
        @Pattern(regexp = "QR_CODE|NFC", message = "tag_type 必须是 QR_CODE 或 NFC")
        private String tagType;

        @Min(1) @Max(10000)
        private int quantity;

        private String batchKeyId;

        public String getTagType() { return tagType; }
        public void setTagType(String tagType) { this.tagType = tagType; }
        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
        public String getBatchKeyId() { return batchKeyId; }
        public void setBatchKeyId(String batchKeyId) { this.batchKeyId = batchKeyId; }
    }
}
