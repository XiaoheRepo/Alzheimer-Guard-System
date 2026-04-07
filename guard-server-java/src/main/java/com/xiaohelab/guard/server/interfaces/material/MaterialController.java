package com.xiaohelab.guard.server.interfaces.material;

import com.xiaohelab.guard.server.application.material.MaterialOrderService;
import com.xiaohelab.guard.server.common.response.ApiResponse;
import com.xiaohelab.guard.server.common.response.PageResponse;
import com.xiaohelab.guard.server.infrastructure.persistence.do_.TagApplyRecordDO;
import com.xiaohelab.guard.server.infrastructure.persistence.do_.TagAssetDO;
import com.xiaohelab.guard.server.security.config.SecurityContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 物资申领与标签接口（用户端）。
 * POST /api/v1/material/orders              — 创建申领工单
 * GET  /api/v1/material/orders              — 查询我的工单列表
 * POST /api/v1/material/orders/{orderId}/cancel   — 申请取消
 * POST /api/v1/material/orders/{orderId}/confirm  — 确认收货
 * POST /api/v1/patients/{patientId}/tags/bind     — 绑定标签
 * POST /api/v1/patients/{patientId}/tags/{tagCode}/lost — 上报丢失
 */
@RestController
@RequiredArgsConstructor
public class MaterialController {

    private final MaterialOrderService materialOrderService;
    private final SecurityContext securityContext;

    /** 创建申领工单 */
    @PostMapping("/api/v1/material/orders")
    public ApiResponse<Map<String, Object>> createOrder(
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId,
            @Valid @RequestBody CreateOrderRequest req) {

        Long userId = securityContext.currentUserId();
        TagApplyRecordDO order = materialOrderService.createOrder(
                userId, req.getPatientId(), req.getQuantity(),
                req.getApplyNote(), req.getDeliveryAddress());
        return ApiResponse.ok(buildOrderVO(order), traceId);
    }

    /** 查询我的工单列表 */
    @GetMapping("/api/v1/material/orders")
    public ApiResponse<PageResponse<Map<String, Object>>> listMyOrders(
            @RequestParam(defaultValue = "1") @Min(1) int pageNo,
            @RequestParam(defaultValue = "10") @Min(1) int pageSize,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId) {

        Long userId = securityContext.currentUserId();
        List<TagApplyRecordDO> list = materialOrderService.listMyOrders(userId, pageNo, pageSize);
        long total = materialOrderService.countMyOrders(userId);
        List<Map<String, Object>> items = list.stream().map(this::buildOrderVO).toList();
        return ApiResponse.ok(PageResponse.<Map<String, Object>>builder()
                .items(items).pageNo(pageNo).pageSize(pageSize)
                .total(total).hasNext(total > (long) pageNo * pageSize)
                .build(), traceId);
    }

    /** 查询工单详情 */
    @GetMapping("/api/v1/material/orders/{orderId}")
    public ApiResponse<Map<String, Object>> getOrder(
            @PathVariable Long orderId,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId) {

        TagApplyRecordDO order = materialOrderService.getOrder(orderId);
        return ApiResponse.ok(buildOrderVO(order), traceId);
    }

    /** 申请取消工单 */
    @PostMapping("/api/v1/material/orders/{orderId}/cancel")
    public ApiResponse<Map<String, Object>> cancelOrder(
            @PathVariable Long orderId,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId,
            @Valid @RequestBody CancelOrderRequest req) {

        Long userId = securityContext.currentUserId();
        TagApplyRecordDO order = materialOrderService.cancelOrder(orderId, userId, req.getCancelReason());
        return ApiResponse.ok(buildOrderVO(order), traceId);
    }

    /** 确认收货 */
    @PostMapping("/api/v1/material/orders/{orderId}/confirm")
    public ApiResponse<Map<String, Object>> confirmReceipt(
            @PathVariable Long orderId,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId) {

        Long userId = securityContext.currentUserId();
        TagApplyRecordDO order = materialOrderService.confirmReceipt(orderId, userId);
        return ApiResponse.ok(buildOrderVO(order), traceId);
    }

    /** 绑定标签到患者 */
    @PostMapping("/api/v1/patients/{patientId}/tags/bind")
    public ApiResponse<Map<String, Object>> bindTag(
            @PathVariable Long patientId,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId,
            @Valid @RequestBody BindTagRequest req) {

        Long userId = securityContext.currentUserId();
        TagAssetDO tag = materialOrderService.bindTag(patientId, userId, req.getTagCode());
        return ApiResponse.ok(buildTagVO(tag), traceId);
    }

    /** 上报标签丢失 */
    @PostMapping("/api/v1/patients/{patientId}/tags/{tagCode}/lost")
    public ApiResponse<Map<String, Object>> reportLost(
            @PathVariable Long patientId,
            @PathVariable String tagCode,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId) {

        Long userId = securityContext.currentUserId();
        TagAssetDO tag = materialOrderService.reportLost(patientId, userId, tagCode);
        return ApiResponse.ok(buildTagVO(tag), traceId);
    }

    // ===== VO 构建 =====

    private Map<String, Object> buildOrderVO(TagApplyRecordDO o) {
        return Map.of(
                "order_id", String.valueOf(o.getId()),
                "order_no", o.getOrderNo(),
                "patient_id", String.valueOf(o.getPatientId()),
                "status", o.getStatus(),
                "quantity", o.getQuantity() != null ? o.getQuantity() : 0,
                "tag_code", o.getTagCode() != null ? o.getTagCode() : "",
                "tracking_number", o.getTrackingNumber() != null ? o.getTrackingNumber() : "",
                "created_at", o.getCreatedAt() != null ? o.getCreatedAt().toString() : ""
        );
    }

    private Map<String, Object> buildTagVO(TagAssetDO tag) {
        return Map.of(
                "tag_code", tag.getTagCode(),
                "tag_type", tag.getTagType() != null ? tag.getTagType() : "",
                "status", tag.getStatus()
        );
    }

    // ===== DTO =====

    @Data
    public static class CreateOrderRequest {
        @NotNull private Long patientId;
        @NotNull @Min(1) private Integer quantity;
        private String applyNote;
        @NotBlank private String deliveryAddress;
    }

    @Data
    public static class CancelOrderRequest {
        private String cancelReason;
    }

    @Data
    public static class BindTagRequest {
        @NotBlank private String tagCode;
    }
}
