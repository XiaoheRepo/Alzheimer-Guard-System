package com.xiaohelab.guard.server.material.controller;

import com.xiaohelab.guard.server.common.annotation.Idempotent;
import com.xiaohelab.guard.server.common.dto.Result;
import com.xiaohelab.guard.server.material.dto.OrderCreateRequest;
import com.xiaohelab.guard.server.material.dto.OrderResolveExceptionRequest;
import com.xiaohelab.guard.server.material.dto.OrderReviewRequest;
import com.xiaohelab.guard.server.material.dto.OrderShipRequest;
import com.xiaohelab.guard.server.material.entity.TagApplyRecordEntity;
import com.xiaohelab.guard.server.material.service.MaterialOrderService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** 物资工单接口：申请 / 审核 / 发货 / 签收 / 取消 / 查询。 */
@Tag(name = "Material.Order", description = "标签工单申领")
@RestController
@RequestMapping("/api/v1/material/orders")
public class MaterialOrderController {

    private final MaterialOrderService orderService;

    public MaterialOrderController(MaterialOrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @Idempotent
    public Result<TagApplyRecordEntity> create(@Valid @RequestBody OrderCreateRequest req) {
        return Result.ok(orderService.create(req));
    }

    @GetMapping("/{orderId}")
    public Result<TagApplyRecordEntity> get(@PathVariable Long orderId) {
        return Result.ok(orderService.get(orderId));
    }

    @GetMapping
    public Result<Page<TagApplyRecordEntity>> list(@RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "20") int size) {
        return Result.ok(orderService.listMine(page, size));
    }

    @PostMapping("/{orderId}/review")
    @Idempotent
    public Result<TagApplyRecordEntity> review(@PathVariable Long orderId,
                                               @Valid @RequestBody OrderReviewRequest req) {
        return Result.ok(orderService.review(orderId, req));
    }

    @PostMapping("/{orderId}/ship")
    @Idempotent
    public Result<TagApplyRecordEntity> ship(@PathVariable Long orderId,
                                             @Valid @RequestBody OrderShipRequest req) {
        return Result.ok(orderService.ship(orderId, req));
    }

    @PostMapping("/{orderId}/receive")
    @Idempotent
    public Result<TagApplyRecordEntity> receive(@PathVariable Long orderId) {
        return Result.ok(orderService.receive(orderId));
    }

    @PostMapping("/{orderId}/cancel")
    @Idempotent
    public Result<TagApplyRecordEntity> cancel(@PathVariable Long orderId,
                                               @RequestBody Map<String, String> body) {
        return Result.ok(orderService.cancel(orderId, body.get("cancel_reason")));
    }

    /**
     * 3.4.12 物流异常处置（ADMIN / SUPER_ADMIN）：
     * 对 EXCEPTION 工单执行 RESHIP（补发新物流）或 VOID（直接作废）。
     */
    @PostMapping("/{orderId}/resolve-exception")
    @Idempotent
    public Result<TagApplyRecordEntity> resolveException(@PathVariable Long orderId,
                                                         @Valid @RequestBody OrderResolveExceptionRequest req) {
        return Result.ok(orderService.resolveException(orderId, req));
    }
}
