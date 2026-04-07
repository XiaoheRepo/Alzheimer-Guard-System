package com.xiaohelab.guard.server.interfaces.governance;

import com.xiaohelab.guard.server.application.material.MaterialOrderService;
import com.xiaohelab.guard.server.common.exception.BizException;
import com.xiaohelab.guard.server.common.response.ApiResponse;
import com.xiaohelab.guard.server.common.response.PageResponse;
import com.xiaohelab.guard.server.infrastructure.persistence.do_.ClueRecordDO;
import com.xiaohelab.guard.server.infrastructure.persistence.do_.SysLogDO;
import com.xiaohelab.guard.server.infrastructure.persistence.do_.SysUserDO;
import com.xiaohelab.guard.server.infrastructure.persistence.do_.TagApplyRecordDO;
import com.xiaohelab.guard.server.infrastructure.persistence.mapper.ClueRecordMapper;
import com.xiaohelab.guard.server.infrastructure.persistence.mapper.SysLogMapper;
import com.xiaohelab.guard.server.infrastructure.persistence.mapper.SysUserMapper;
import com.xiaohelab.guard.server.security.config.SecurityContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 管理员后台接口（需 ADMIN 或 SUPERADMIN 角色）。
 * 涵盖：用户管理 / 审计日志 / 物资申领管理 / 标签管理 / 线索审查
 */
@RestController
@RequiredArgsConstructor
public class AdminController {

    private final SysUserMapper sysUserMapper;
    private final SysLogMapper sysLogMapper;
    private final MaterialOrderService materialOrderService;
    private final ClueRecordMapper clueRecordMapper;
    private final SecurityContext securityContext;

    // ===== 用户管理 =====

    /** 分页查询用户列表 */
    @GetMapping("/api/v1/admin/users")
    public ApiResponse<PageResponse<Map<String, Object>>> listUsers(
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") @Min(1) int pageNo,
            @RequestParam(defaultValue = "20") @Min(1) int pageSize,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId) {

        requireAdmin();
        List<SysUserDO> list = sysUserMapper.listByFilter(role, status, keyword, pageSize, (pageNo - 1) * pageSize);
        long total = sysUserMapper.countByFilter(role, status, keyword);
        List<Map<String, Object>> items = list.stream().map(this::buildUserVO).toList();
        return ApiResponse.ok(PageResponse.<Map<String, Object>>builder()
                .items(items).pageNo(pageNo).pageSize(pageSize)
                .total(total).hasNext(total > (long) pageNo * pageSize)
                .build(), traceId);
    }

    /** 更新用户状态（封禁/解封） */
    @PutMapping("/api/v1/admin/users/{userId}/status")
    public ApiResponse<Map<String, Object>> updateUserStatus(
            @PathVariable Long userId,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId,
            @Valid @RequestBody UpdateStatusRequest req) {

        requireAdmin();
        SysUserDO user = sysUserMapper.findById(userId);
        if (user == null) throw BizException.of("E_USER_4041");
        sysUserMapper.updateStatus(userId, req.getStatus());
        return ApiResponse.ok(Map.of("user_id", String.valueOf(userId), "status", req.getStatus()), traceId);
    }

    // ===== 审计日志 =====

    /** 分页查询审计日志 */
    @GetMapping("/api/v1/admin/logs")
    public ApiResponse<PageResponse<Map<String, Object>>> listLogs(
            @RequestParam(required = false) String module,
            @RequestParam(defaultValue = "1") @Min(1) int pageNo,
            @RequestParam(defaultValue = "20") @Min(1) int pageSize,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId) {

        requireAdmin();
        List<SysLogDO> list;
        long total;
        if (module != null && !module.isBlank()) {
            list = sysLogMapper.listByModule(module, pageSize, (pageNo - 1) * pageSize);
            total = sysLogMapper.count(); // 简化：总数不过滤 module
        } else {
            list = sysLogMapper.listByFilter(pageSize, (pageNo - 1) * pageSize);
            total = sysLogMapper.count();
        }
        List<Map<String, Object>> items = list.stream().map(l -> Map.<String, Object>of(
                "log_id", String.valueOf(l.getId()),
                "module", l.getModule() != null ? l.getModule() : "",
                "action", l.getAction() != null ? l.getAction() : "",
                "operator_user_id", String.valueOf(l.getOperatorUserId() != null ? l.getOperatorUserId() : 0),
                "operator_username", l.getOperatorUsername() != null ? l.getOperatorUsername() : "",
                "result", l.getResult() != null ? l.getResult() : "",
                "executed_at", l.getExecutedAt() != null ? l.getExecutedAt().toString() : ""
        )).toList();
        return ApiResponse.ok(PageResponse.<Map<String, Object>>builder()
                .items(items).pageNo(pageNo).pageSize(pageSize)
                .total(total).hasNext(total > (long) pageNo * pageSize)
                .build(), traceId);
    }

    // ===== 物资申领管理 =====

    /** 管理员查询工单列表 */
    @GetMapping("/api/v1/admin/material/orders")
    public ApiResponse<PageResponse<Map<String, Object>>> adminListOrders(
            @RequestParam(required = false, defaultValue = "PENDING") String status,
            @RequestParam(defaultValue = "1") @Min(1) int pageNo,
            @RequestParam(defaultValue = "20") @Min(1) int pageSize,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId) {

        requireAdmin();
        List<TagApplyRecordDO> list = materialOrderService.adminListOrders(status, pageNo, pageSize);
        long total = materialOrderService.adminCountOrders(status);
        List<Map<String, Object>> items = list.stream().map(o -> Map.<String, Object>of(
                "order_id", String.valueOf(o.getId()),
                "order_no", o.getOrderNo(),
                "patient_id", String.valueOf(o.getPatientId()),
                "status", o.getStatus(),
                "quantity", o.getQuantity() != null ? o.getQuantity() : 0,
                "tag_code", o.getTagCode() != null ? o.getTagCode() : "",
                "created_at", o.getCreatedAt() != null ? o.getCreatedAt().toString() : ""
        )).toList();
        return ApiResponse.ok(PageResponse.<Map<String, Object>>builder()
                .items(items).pageNo(pageNo).pageSize(pageSize)
                .total(total).hasNext(total > (long) pageNo * pageSize)
                .build(), traceId);
    }

    /** 管理员审批通过 */
    @PutMapping("/api/v1/admin/material/orders/{orderId}/approve")
    public ApiResponse<Map<String, Object>> approveOrder(
            @PathVariable Long orderId,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId) {

        requireAdmin();
        materialOrderService.adminApprove(orderId);
        return ApiResponse.ok(Map.of("order_id", String.valueOf(orderId), "status", "PROCESSING"), traceId);
    }

    /** 管理员发货 */
    @PutMapping("/api/v1/admin/material/orders/{orderId}/ship")
    public ApiResponse<Map<String, Object>> shipOrder(
            @PathVariable Long orderId,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId,
            @Valid @RequestBody ShipOrderRequest req) {

        requireAdmin();
        materialOrderService.adminShip(orderId, req.getTagCode(),
                req.getTrackingNumber(), req.getCourierName(), req.getResourceLink());
        return ApiResponse.ok(Map.of("order_id", String.valueOf(orderId), "status", "SHIPPED"), traceId);
    }

    /** 管理员批准取消申请 */
    @PutMapping("/api/v1/admin/material/orders/{orderId}/cancel/approve")
    public ApiResponse<Map<String, Object>> approveCancelRequest(
            @PathVariable Long orderId,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId) {

        requireAdmin();
        materialOrderService.adminApproveCancelRequest(orderId);
        return ApiResponse.ok(Map.of("order_id", String.valueOf(orderId), "status", "CANCELLED"), traceId);
    }

    /** 管理员拒绝取消申请 */
    @PutMapping("/api/v1/admin/material/orders/{orderId}/cancel/reject")
    public ApiResponse<Map<String, Object>> rejectCancelRequest(
            @PathVariable Long orderId,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId) {

        requireAdmin();
        materialOrderService.adminRejectCancelRequest(orderId);
        return ApiResponse.ok(Map.of("order_id", String.valueOf(orderId), "status", "PROCESSING"), traceId);
    }

    /** 管理员标记物流异常 */
    @PutMapping("/api/v1/admin/material/orders/{orderId}/logistics-exception")
    public ApiResponse<Map<String, Object>> markException(
            @PathVariable Long orderId,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId,
            @Valid @RequestBody ExceptionRequest req) {

        requireAdmin();
        materialOrderService.adminMarkException(orderId, req.getExceptionDesc());
        return ApiResponse.ok(Map.of("order_id", String.valueOf(orderId), "status", "EXCEPTION"), traceId);
    }

    /** 管理员重新发货 */
    @PutMapping("/api/v1/admin/material/orders/{orderId}/reship")
    public ApiResponse<Map<String, Object>> reshipOrder(
            @PathVariable Long orderId,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId,
            @Valid @RequestBody ReshipRequest req) {

        requireAdmin();
        materialOrderService.adminReship(orderId, req.getTrackingNumber(), req.getCourierName());
        return ApiResponse.ok(Map.of("order_id", String.valueOf(orderId), "status", "SHIPPED"), traceId);
    }

    /** 管理员强制关闭异常 */
    @PutMapping("/api/v1/admin/material/orders/{orderId}/close-exception")
    public ApiResponse<Map<String, Object>> closeException(
            @PathVariable Long orderId,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId) {

        requireAdmin();
        materialOrderService.adminCloseException(orderId);
        return ApiResponse.ok(Map.of("order_id", String.valueOf(orderId), "status", "COMPLETED"), traceId);
    }

    // ===== 标签管理 =====

    /** 管理员批量导入标签 */
    @PostMapping("/api/v1/admin/tags/import")
    public ApiResponse<Map<String, Object>> importTags(
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId,
            @Valid @RequestBody ImportTagsRequest req) {

        requireAdmin();
        materialOrderService.adminImportTags(req.getTagCodes(), req.getTagType(), req.getBatchNo());
        return ApiResponse.ok(Map.of("imported", req.getTagCodes().size(), "batch_no", req.getBatchNo()), traceId);
    }

    /** 管理员作废标签 */
    @PostMapping("/api/v1/admin/tags/{tagCode}/void")
    public ApiResponse<Map<String, Object>> voidTag(
            @PathVariable String tagCode,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId,
            @Valid @RequestBody VoidTagRequest req) {

        requireAdmin();
        materialOrderService.adminVoidTag(tagCode, req.getVoidReason());
        return ApiResponse.ok(Map.of("tag_code", tagCode, "status", "VOID"), traceId);
    }

    /** 管理员重置标签 */
    @PostMapping("/api/v1/admin/tags/{tagCode}/reset")
    public ApiResponse<Map<String, Object>> resetTag(
            @PathVariable String tagCode,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId) {

        requireAdmin();
        materialOrderService.adminResetTag(tagCode);
        return ApiResponse.ok(Map.of("tag_code", tagCode, "status", "UNBOUND"), traceId);
    }

    /** 管理员恢复丢失标签 */
    @PostMapping("/api/v1/admin/tags/{tagCode}/recover")
    public ApiResponse<Map<String, Object>> recoverTag(
            @PathVariable String tagCode,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId) {

        requireAdmin();
        materialOrderService.adminRecoverTag(tagCode);
        return ApiResponse.ok(Map.of("tag_code", tagCode, "status", "BOUND"), traceId);
    }

    // ===== 线索审查 =====

    /** 管理员复核队列 */
    @GetMapping("/api/v1/admin/clues/review/queue")
    public ApiResponse<PageResponse<Map<String, Object>>> reviewQueue(
            @RequestParam(defaultValue = "1") @Min(1) int pageNo,
            @RequestParam(defaultValue = "20") @Min(1) int pageSize,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId) {

        requireAdmin();
        List<ClueRecordDO> list = clueRecordMapper.listReviewQueue(pageSize, (pageNo - 1) * pageSize);
        long total = clueRecordMapper.countReviewQueue();
        List<Map<String, Object>> items = list.stream().map(this::buildClueVO).toList();
        return ApiResponse.ok(PageResponse.<Map<String, Object>>builder()
                .items(items).pageNo(pageNo).pageSize(pageSize)
                .total(total).hasNext(total > (long) pageNo * pageSize)
                .build(), traceId);
    }

    /** 查询线索详情 */
    @GetMapping("/api/v1/admin/clues/{clueId}")
    public ApiResponse<Map<String, Object>> getClue(
            @PathVariable Long clueId,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId) {

        requireAdmin();
        ClueRecordDO clue = clueRecordMapper.findById(clueId);
        if (clue == null) throw BizException.of("E_CLUE_4041");
        return ApiResponse.ok(buildClueVO(clue), traceId);
    }

    /** 管理员分配线索给复核员 */
    @PostMapping("/api/v1/admin/clues/{clueId}/assign")
    public ApiResponse<Map<String, Object>> assignClue(
            @PathVariable Long clueId,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId,
            @Valid @RequestBody AssignClueRequest req) {

        requireAdmin();
        ClueRecordDO clue = clueRecordMapper.findById(clueId);
        if (clue == null) throw BizException.of("E_CLUE_4041");
        clue.setAssigneeUserId(req.getAssigneeUserId());
        clueRecordMapper.assign(clue);
        return ApiResponse.ok(Map.of("clue_id", String.valueOf(clueId),
                "assignee_user_id", String.valueOf(req.getAssigneeUserId())), traceId);
    }

    /** 管理员 override 线索 */
    @PostMapping("/api/v1/clues/{clueId}/override")
    public ApiResponse<Map<String, Object>> overrideClue(
            @PathVariable Long clueId,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId,
            @Valid @RequestBody OverrideClueRequest req) {

        requireAdmin();
        ClueRecordDO clue = clueRecordMapper.findById(clueId);
        if (clue == null) throw BizException.of("E_CLUE_4041");
        clue.setOverrideBy(securityContext.currentUserId());
        clue.setOverrideReason(req.getOverrideReason());
        clueRecordMapper.override(clue);
        return ApiResponse.ok(Map.of("clue_id", String.valueOf(clueId), "review_status", "OVERRIDDEN"), traceId);
    }

    /** 管理员 reject 线索 */
    @PostMapping("/api/v1/clues/{clueId}/reject")
    public ApiResponse<Map<String, Object>> rejectClue(
            @PathVariable Long clueId,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId,
            @Valid @RequestBody RejectClueRequest req) {

        requireAdmin();
        ClueRecordDO clue = clueRecordMapper.findById(clueId);
        if (clue == null) throw BizException.of("E_CLUE_4041");
        clue.setRejectedBy(securityContext.currentUserId());
        clue.setRejectReason(req.getRejectReason());
        clueRecordMapper.reject(clue);
        return ApiResponse.ok(Map.of("clue_id", String.valueOf(clueId), "review_status", "REJECTED"), traceId);
    }

    // ===== 内部工具 =====

    private void requireAdmin() {
        if (!securityContext.isAdmin()) throw BizException.of("E_AUTH_4031");
    }

    private Map<String, Object> buildUserVO(SysUserDO u) {
        return Map.of(
                "user_id", String.valueOf(u.getId()),
                "username", u.getUsername(),
                "display_name", u.getDisplayName() != null ? u.getDisplayName() : "",
                "phone", u.getPhone() != null ? u.getPhone() : "",
                "role", u.getRole(),
                "status", u.getStatus(),
                "created_at", u.getCreatedAt() != null ? u.getCreatedAt().toString() : ""
        );
    }

    private Map<String, Object> buildClueVO(ClueRecordDO c) {
        return Map.of(
                "clue_id", String.valueOf(c.getId()),
                "clue_no", c.getClueNo(),
                "patient_id", String.valueOf(c.getPatientId()),
                "review_status", c.getReviewStatus() != null ? c.getReviewStatus() : "",
                "risk_score", c.getRiskScore() != null ? c.getRiskScore() : 0,
                "suspect_flag", c.getSuspectFlag() != null && c.getSuspectFlag(),
                "created_at", c.getCreatedAt() != null ? c.getCreatedAt().toString() : ""
        );
    }

    // ===== DTO =====

    @Data
    public static class UpdateStatusRequest {
        @NotBlank private String status;
    }

    @Data
    public static class ShipOrderRequest {
        @NotBlank private String tagCode;
        @NotBlank private String trackingNumber;
        @NotBlank private String courierName;
        private String resourceLink;
    }

    @Data
    public static class ExceptionRequest {
        @NotBlank private String exceptionDesc;
    }

    @Data
    public static class ReshipRequest {
        @NotBlank private String trackingNumber;
        @NotBlank private String courierName;
    }

    @Data
    public static class ImportTagsRequest {
        private List<String> tagCodes;
        @NotBlank private String tagType;
        @NotBlank private String batchNo;
    }

    @Data
    public static class VoidTagRequest {
        @NotBlank private String voidReason;
    }

    @Data
    public static class AssignClueRequest {
        private Long assigneeUserId;
    }

    @Data
    public static class OverrideClueRequest {
        @NotBlank private String overrideReason;
    }

    @Data
    public static class RejectClueRequest {
        @NotBlank private String rejectReason;
    }
}
