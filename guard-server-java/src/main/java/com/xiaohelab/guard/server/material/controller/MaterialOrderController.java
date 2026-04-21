package com.xiaohelab.guard.server.material.controller;

import com.xiaohelab.guard.server.common.annotation.Idempotent;
import com.xiaohelab.guard.server.common.dto.Result;
import com.xiaohelab.guard.server.material.dto.OrderCreateRequest;
import com.xiaohelab.guard.server.material.dto.OrderReviewRequest;
import com.xiaohelab.guard.server.material.dto.OrderShipRequest;
import com.xiaohelab.guard.server.material.entity.TagApplyRecordEntity;
import com.xiaohelab.guard.server.material.service.MaterialOrderService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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
}
