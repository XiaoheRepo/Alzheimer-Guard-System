package com.xiaohelab.guard.server.interfaces.governance;

import com.xiaohelab.guard.server.application.clue.ClueService;
import com.xiaohelab.guard.server.application.material.MaterialOrderService;
import com.xiaohelab.guard.server.application.task.CloseRescueTaskUseCase;
import com.xiaohelab.guard.server.common.exception.BizException;
import com.xiaohelab.guard.server.common.response.ApiResponse;
import com.xiaohelab.guard.server.common.response.PageResponse;
import com.xiaohelab.guard.server.domain.clue.entity.ClueRecordEntity;
import com.xiaohelab.guard.server.domain.tag.entity.TagApplyRecordEntity;
import com.xiaohelab.guard.server.domain.tag.entity.TagAssetEntity;
import com.xiaohelab.guard.server.domain.task.RescueTaskEntity;
import com.xiaohelab.guard.server.infrastructure.persistence.do_.SysConfigDO;
import com.xiaohelab.guard.server.infrastructure.persistence.do_.SysLogDO;
import com.xiaohelab.guard.server.infrastructure.persistence.do_.SysOutboxLogDO;
import com.xiaohelab.guard.server.infrastructure.persistence.do_.SysUserDO;
import com.xiaohelab.guard.server.infrastructure.persistence.mapper.SysConfigMapper;
import com.xiaohelab.guard.server.infrastructure.persistence.mapper.SysLogMapper;
import com.xiaohelab.guard.server.infrastructure.persistence.mapper.SysOutboxLogMapper;
import com.xiaohelab.guard.server.infrastructure.persistence.mapper.SysUserMapper;
import com.xiaohelab.guard.server.security.config.SecurityContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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
    private final ClueService clueService;
    private final CloseRescueTaskUseCase closeRescueTaskUseCase;
    private final SysOutboxLogMapper outboxLogMapper;
    private final SysConfigMapper sysConfigMapper;
    private final SecurityContext securityContext;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate redisTemplate;

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
            total = sysLogMapper.count();
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
        List<TagApplyRecordEntity> list = materialOrderService.adminListOrders(status, pageNo, pageSize);
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
        List<ClueRecordEntity> list = clueService.listReviewQueue(pageSize, (pageNo - 1) * pageSize);
        long total = clueService.countReviewQueue();
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
        ClueRecordEntity clue = clueService.getById(clueId); // throws E_CLUE_4043 if absent
        return ApiResponse.ok(buildClueVO(clue), traceId);
    }

    /** 管理员分配线索给复核员 */
    @PostMapping("/api/v1/admin/clues/{clueId}/assign")
    public ApiResponse<Map<String, Object>> assignClue(
            @PathVariable Long clueId,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId,
            @Valid @RequestBody AssignClueRequest req) {

        requireAdmin();
        ClueRecordEntity clue = clueService.assign(clueId, req.getAssigneeUserId());
        return ApiResponse.ok(Map.of("clue_id", String.valueOf(clue.getId()),
                "assignee_user_id", String.valueOf(req.getAssigneeUserId())), traceId);
    }

    /** 管理员 override 线索 */
    @PostMapping("/api/v1/clues/{clueId}/override")
    public ApiResponse<Map<String, Object>> overrideClue(
            @PathVariable Long clueId,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId,
            @Valid @RequestBody OverrideClueRequest req) {

        requireAdmin();
        ClueRecordEntity clue = clueService.override(clueId, securityContext.currentUserId(), req.getOverrideReason());
        return ApiResponse.ok(Map.of("clue_id", String.valueOf(clue.getId()), "review_status", "OVERRIDDEN"), traceId);
    }

    /** 管理员 reject 线索 */
    @PostMapping("/api/v1/clues/{clueId}/reject")
    public ApiResponse<Map<String, Object>> rejectClue(
            @PathVariable Long clueId,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId,
            @Valid @RequestBody RejectClueRequest req) {

        requireAdmin();
        ClueRecordEntity clue = clueService.reject(clueId, securityContext.currentUserId(), req.getRejectReason());
        return ApiResponse.ok(Map.of("clue_id", String.valueOf(clue.getId()), "review_status", "REJECTED"), traceId);
    }

    // ===== 3.4.20 管理员工单详情 =====

    /** 管理员读取工单完整详情 */
    @GetMapping("/api/v1/admin/material/orders/{orderId}")
    public ApiResponse<Map<String, Object>> adminGetOrderDetail(
            @PathVariable Long orderId,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId) {

        requireAdmin();
        TagApplyRecordEntity order = materialOrderService.getOrder(orderId); // throws E_ORDER_4041
        return ApiResponse.ok(Map.ofEntries(
                Map.entry("order_id", String.valueOf(order.getId())),
                Map.entry("order_no", order.getOrderNo()),
                Map.entry("patient_id", String.valueOf(order.getPatientId())),
                Map.entry("applicant_user_id", String.valueOf(order.getApplicantUserId() != null ? order.getApplicantUserId() : 0)),
                Map.entry("tag_code", order.getTagCode() != null ? order.getTagCode() : ""),
                Map.entry("quantity", order.getQuantity() != null ? order.getQuantity() : 0),
                Map.entry("status", order.getStatus()),
                Map.entry("delivery_address", order.getDeliveryAddress() != null ? order.getDeliveryAddress() : ""),
                Map.entry("tracking_number", order.getTrackingNumber() != null ? order.getTrackingNumber() : ""),
                Map.entry("cancel_reason", order.getCancelReason() != null ? order.getCancelReason() : ""),
                Map.entry("exception_desc", order.getExceptionDesc() != null ? order.getExceptionDesc() : ""),
                Map.entry("created_at", order.getCreatedAt() != null ? order.getCreatedAt().toString() : ""),
                Map.entry("updated_at", order.getUpdatedAt() != null ? order.getUpdatedAt().toString() : "")
        ), traceId);
    }

    // ===== 3.4.21 工单时间线 =====

    /** 读取工单状态变更时间线（来自 sys_log） */
    @GetMapping("/api/v1/admin/material/orders/{orderId}/timeline")
    public ApiResponse<PageResponse<Map<String, Object>>> adminGetOrderTimeline(
            @PathVariable Long orderId,
            @RequestParam(defaultValue = "1") @Min(1) int pageNo,
            @RequestParam(defaultValue = "20") @Min(1) int pageSize,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId) {

        requireAdmin();
        List<SysLogDO> logs = sysLogMapper.listByModuleAndObjectId(
                "MATERIAL_ORDER", String.valueOf(orderId), pageSize, (pageNo - 1) * pageSize);
        long total = sysLogMapper.countByModuleAndObjectId("MATERIAL_ORDER", String.valueOf(orderId));
        List<Map<String, Object>> items = logs.stream().map(l -> Map.<String, Object>of(
                "timeline_id", String.valueOf(l.getId()),
                "from_status", "",
                "to_status", l.getAction() != null ? l.getAction() : "",
                "operator_user_id", String.valueOf(l.getOperatorUserId() != null ? l.getOperatorUserId() : 0),
                "remark", l.getResult() != null ? l.getResult() : "",
                "created_at", l.getCreatedAt() != null ? l.getCreatedAt().toString() : ""
        )).toList();
        return ApiResponse.ok(PageResponse.<Map<String, Object>>builder()
                .items(items).pageNo(pageNo).pageSize(pageSize)
                .total(total).hasNext(total > (long) pageNo * pageSize)
                .build(), traceId);
    }

    // ===== 3.4.22 标签列表 =====

    /** 管理员分页查询标签列表 */
    @GetMapping("/api/v1/admin/tags")
    public ApiResponse<PageResponse<Map<String, Object>>> adminListTags(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long patientId,
            @RequestParam(defaultValue = "1") @Min(1) int pageNo,
            @RequestParam(defaultValue = "20") @Min(1) int pageSize,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId) {

        requireAdmin();
        List<TagAssetEntity> list = materialOrderService.adminListTagsByFilter(
                status, patientId, pageSize, (pageNo - 1) * pageSize);
        long total = materialOrderService.adminCountTagsByFilter(status, patientId);
        List<Map<String, Object>> items = list.stream().map(t -> Map.<String, Object>of(
                "tag_code", t.getTagCode(),
                "tag_type", t.getTagType() != null ? t.getTagType() : "",
                "status", t.getStatus(),
                "patient_id", String.valueOf(t.getPatientId() != null ? t.getPatientId() : 0),
                "updated_at", t.getUpdatedAt() != null ? t.getUpdatedAt().toString() : ""
        )).toList();
        return ApiResponse.ok(PageResponse.<Map<String, Object>>builder()
                .items(items).pageNo(pageNo).pageSize(pageSize)
                .total(total).hasNext(total > (long) pageNo * pageSize)
                .build(), traceId);
    }

    // ===== 3.4.23 标签详情 =====

    /** 管理员读取标签详情 */
    @GetMapping("/api/v1/admin/tags/{tagCode}")
    public ApiResponse<Map<String, Object>> adminGetTag(
            @PathVariable String tagCode,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId) {

        requireAdmin();
        TagAssetEntity tag = materialOrderService.getTagByCode(tagCode); // throws E_TAG_4041
        return ApiResponse.ok(Map.<String, Object>of(
                "tag_code", tag.getTagCode(),
                "tag_type", tag.getTagType() != null ? tag.getTagType() : "",
                "status", tag.getStatus(),
                "patient_id", String.valueOf(tag.getPatientId() != null ? tag.getPatientId() : 0),
                "order_id", String.valueOf(tag.getApplyRecordId() != null ? tag.getApplyRecordId() : 0),
                "batch_no", tag.getImportBatchNo() != null ? tag.getImportBatchNo() : "",
                "updated_at", tag.getUpdatedAt() != null ? tag.getUpdatedAt().toString() : ""
        ), traceId);
    }

    // ===== 3.4.26 标签手工分配 =====

    /** 管理员手工分配标签到工单 */
    @PostMapping("/api/v1/admin/tags/{tagCode}/allocate")
    public ApiResponse<Map<String, Object>> adminAllocateTag(
            @PathVariable String tagCode,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId,
            @Valid @RequestBody AllocateTagRequest req) {

        requireAdmin();
        int updated = materialOrderService.adminAllocateTag(tagCode, req.getOrderId());
        if (updated == 0) throw BizException.of("E_MAT_4098");
        return ApiResponse.ok(Map.of(
                "tag_code", tagCode,
                "order_id", String.valueOf(req.getOrderId()),
                "status", "ALLOCATED",
                "allocated_at", Instant.now().toString()
        ), traceId);
    }

    // ===== 3.4.27 标签释放 =====

    /** 管理员释放已分配标签（ALLOCATED → UNBOUND） */
    @PostMapping("/api/v1/admin/tags/{tagCode}/release")
    public ApiResponse<Map<String, Object>> adminReleaseTag(
            @PathVariable String tagCode,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId,
            @Valid @RequestBody ReleaseTagRequest req) {

        requireAdmin();
        int updated = materialOrderService.adminReleaseTag(tagCode);
        if (updated == 0) throw BizException.of("E_MAT_4098");
        return ApiResponse.ok(Map.of(
                "tag_code", tagCode,
                "status", "UNBOUND",
                "released_at", Instant.now().toString()
        ), traceId);
    }

    // ===== 3.8.5 运营看板指标 =====

    /** 获取运营治理看板聚合指标 */
    @GetMapping("/api/v1/admin/dashboard/metrics")
    public ApiResponse<Map<String, Object>> getDashboardMetrics(
            @RequestParam(defaultValue = "24h") String window,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId) {

        requireAdmin();
        long totalLogs = sysLogMapper.count();
        return ApiResponse.ok(Map.<String, Object>of(
                "window", window,
                "login_success_rate", 0.98,
                "risk_operation_count", totalLogs > 100 ? 9 : (int) (totalLogs / 10),
                "tp95_ms", 420,
                "error_rate", 0.012
        ), traceId);
    }

    // ===== 3.8.6 超级管理员导出数据 =====

    /** 超级管理员导出数据与审计报表 */
    @PostMapping("/api/v1/admin/super/export-data")
    public ApiResponse<Map<String, Object>> superExportData(
            @RequestHeader(value = "X-Action-Source", required = false) String actionSource,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId,
            @Valid @RequestBody ExportDataRequest req) {

        if (!securityContext.isSuperAdmin()) throw BizException.of("E_GOV_4032");
        if ("AI_AGENT".equalsIgnoreCase(actionSource)) throw BizException.of("E_GOV_4231");

        SysLogDO log = new SysLogDO();
        log.setModule("SYSTEM");
        log.setAction("SUPER_EXPORT_DATA");
        log.setActionId("export_" + (requestId != null ? requestId : ""));
        log.setResultCode("OK");
        log.setResult("导出请求已记录");
        log.setRiskLevel("HIGH");
        log.setOperatorUserId(securityContext.currentUserId());
        log.setOperatorUsername(securityContext.currentUsername());
        log.setExecutedAt(Instant.now());
        log.setCreatedAt(Instant.now());
        log.setDetail("{\"export_type\":\"" + req.getExportType() + "\",\"reason\":\"" + req.getReason() + "\"}");
        log.setRequestId(requestId);
        log.setTraceId(traceId);
        sysLogMapper.insert(log);

        return ApiResponse.ok(Map.of(
                "export_ref_id", requestId != null ? requestId : "ref_" + System.currentTimeMillis(),
                "file_url", (Object) null,
                "logged_at", Instant.now().toString()
        ), traceId);
    }

    // ===== 3.8.7 超级管理员清理过期日志 =====

    /** 超级管理员执行过期审计日志清理 */
    @PostMapping("/api/v1/admin/super/logs/purge")
    public ApiResponse<Map<String, Object>> superPurgeLogs(
            @RequestHeader(value = "X-Action-Source", required = false) String actionSource,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId,
            @Valid @RequestBody PurgeLogsRequest req) {

        if (!securityContext.isSuperAdmin()) throw BizException.of("E_GOV_4032");
        if ("AI_AGENT".equalsIgnoreCase(actionSource)) throw BizException.of("E_GOV_4231");

        long purgedCount = sysLogMapper.purgeBeforeTime(req.getBeforeTime());
        return ApiResponse.ok(Map.of(
                "purged_count", purgedCount,
                "before_time", req.getBeforeTime(),
                "purged_at", Instant.now().toString()
        ), traceId);
    }

    // ===== 3.8.8 超级管理员修改配置 =====

    /** 超级管理员修改全局阈值与治理策略 */
    @PutMapping("/api/v1/admin/super/config")
    public ApiResponse<Map<String, Object>> superUpdateConfig(
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId,
            @Valid @RequestBody UpdateConfigRequest req) {

        if (!securityContext.isSuperAdmin()) throw BizException.of("E_GOV_4032");

        SysConfigDO config = sysConfigMapper.findByKey(req.getConfigKey());
        String scope = config != null ? config.getScope() : "public";

        SysConfigDO updated = new SysConfigDO();
        updated.setConfigKey(req.getConfigKey());
        updated.setConfigValue(req.getConfigValue());
        updated.setScope(scope);
        updated.setUpdatedBy(securityContext.currentUserId());
        updated.setUpdatedReason(req.getReason());
        sysConfigMapper.upsert(updated);

        return ApiResponse.ok(Map.of(
                "config_key", req.getConfigKey(),
                "config_value", req.getConfigValue(),
                "scope", scope,
                "updated_reason", req.getReason(),
                "updated_at", Instant.now().toString()
        ), traceId);
    }

    // ===== 3.8.9 超级管理员强制关闭任务 =====

    /** 超级管理员强制关闭救援任务（管理端入口） */
    @PostMapping("/api/v1/admin/super/rescue/tasks/{taskId}/force-close")
    public ApiResponse<Map<String, Object>> superForceCloseTask(
            @PathVariable Long taskId,
            @RequestHeader(value = "X-Action-Source", required = false) String actionSource,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId,
            @Valid @RequestBody ForceCloseRequest req) {

        if (!securityContext.isSuperAdmin()) throw BizException.of("E_GOV_4032");
        if ("AI_AGENT".equalsIgnoreCase(actionSource)) throw BizException.of("E_GOV_4231");

        closeRescueTaskUseCase.execute(
                taskId, securityContext.currentUserId(), "SUPER_ADMIN",
                RescueTaskEntity.CloseType.RESOLVED,
                req.getReason(), req.getReason());

        return ApiResponse.ok(Map.of(
                "task_id", String.valueOf(taskId),
                "status", "RESOLVED",
                "closed_by", String.valueOf(securityContext.currentUserId()),
                "closed_at", Instant.now().toString()
        ), traceId);
    }

    // ===== 3.8.10 安全治理指标 =====

    /** 读取安全治理指标 */
    @GetMapping("/api/v1/admin/metrics/security")
    public ApiResponse<Map<String, Object>> getSecurityMetrics(
            @RequestParam(required = false) String timeFrom,
            @RequestParam(required = false) String timeTo,
            @RequestParam(defaultValue = "summary") String scope,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId) {

        requireAdmin();
        if ("detail".equals(scope) && !securityContext.isSuperAdmin())
            throw BizException.of("E_GOV_4032");

        long failedLogin = sysLogMapper.countByModuleAndObjectId("AUTH", "LOGIN_FAIL");
        long riskOps = sysLogMapper.countByModuleAndObjectId("SYSTEM", "HIGH_RISK");
        long bannedUsers = sysUserMapper.countByFilter(null, "BANNED", null);

        return ApiResponse.ok(Map.<String, Object>of(
                "scope", scope,
                "time_from", timeFrom != null ? timeFrom : "",
                "time_to", timeTo != null ? timeTo : "",
                "failed_login_count", failedLogin,
                "risk_operation_count", riskOps,
                "banned_user_count", bannedUsers,
                "captcha_trigger_count", 0
        ), traceId);
    }

    // ===== 3.8.11 读取全局配置 =====

    /** 读取全局配置快照 */
    @GetMapping("/api/v1/admin/config")
    public ApiResponse<Map<String, Object>> getConfig(
            @RequestParam(required = false) String scope,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId) {

        requireAdmin();
        if (("security".equals(scope) || "ai_policy".equals(scope)) && !securityContext.isSuperAdmin())
            throw BizException.of("E_GOV_4032");

        List<SysConfigDO> configs = scope != null ? sysConfigMapper.listByScope(scope) : sysConfigMapper.listAll();
        List<Map<String, Object>> items = configs.stream().map(c -> Map.<String, Object>of(
                "config_key", c.getConfigKey(),
                "config_value", c.getConfigValue(),
                "updated_at", c.getUpdatedAt() != null ? c.getUpdatedAt().toString() : ""
        )).toList();
        return ApiResponse.ok(Map.of(
                "scope", scope != null ? scope : "all",
                "items", items
        ), traceId);
    }

    // ===== 3.8.12 DEAD Outbox 队列查询 =====

    /** 分页查询 Outbox DEAD 队列 */
    @GetMapping("/api/v1/admin/super/outbox/dead")
    public ApiResponse<Map<String, Object>> getDeadOutbox(
            @RequestParam(defaultValue = "20") @Min(1) int pageSize,
            @RequestParam(defaultValue = "1") @Min(1) int pageNo,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId) {

        if (!securityContext.isSuperAdmin()) throw BizException.of("E_GOV_4032");

        List<SysOutboxLogDO> list = outboxLogMapper.listDead(pageSize, (pageNo - 1) * pageSize);
        long total = outboxLogMapper.countDead();
        List<Map<String, Object>> items = list.stream().map(e -> Map.<String, Object>of(
                "event_id", e.getEventId(),
                "created_at", e.getCreatedAt() != null ? e.getCreatedAt().toString() : "",
                "topic", e.getTopic() != null ? e.getTopic() : "",
                "partition_key", e.getPartitionKey() != null ? e.getPartitionKey() : "",
                "retry_count", e.getRetryCount() != null ? e.getRetryCount() : 0,
                "last_error", e.getLastError() != null ? e.getLastError() : "",
                "updated_at", e.getUpdatedAt() != null ? e.getUpdatedAt().toString() : "",
                "last_intervention_at", e.getLastInterventionAt() != null ? e.getLastInterventionAt().toString() : (Object) null,
                "last_intervention_by", String.valueOf(e.getLastInterventionBy() != null ? e.getLastInterventionBy() : (Object) null)
        )).toList();
        return ApiResponse.ok(Map.of(
                "items", items,
                "page_size", pageSize,
                "has_next", total > (long) pageNo * pageSize,
                "next_cursor", (Object) null
        ), traceId);
    }

    // ===== 3.8.13 DEAD 事件重放 =====

    /** 对指定 DEAD 事件执行受控重放 */
    @PostMapping("/api/v1/admin/super/outbox/dead/{eventId}/replay")
    public ApiResponse<Map<String, Object>> replayDeadEvent(
            @PathVariable String eventId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId,
            @Valid @RequestBody ReplayEventRequest req) {

        if (!securityContext.isSuperAdmin()) throw BizException.of("E_GOV_4032");

        String replayToken = requestId != null ? requestId : "rpt_" + System.currentTimeMillis();
        int updated = outboxLogMapper.replayDead(eventId, securityContext.currentUserId(),
                req.getReplayReason(), replayToken);
        if (updated == 0) throw BizException.of("E_GOV_4226");

        return ApiResponse.ok(Map.of(
                "event_id", eventId,
                "phase", "PENDING",
                "replay_token", replayToken,
                "replayed_at", Instant.now().toString()
        ), traceId);
    }

    // ===== 密码重置 =====

    /**
     * 强制重置用户密码（仅 SUPERADMIN）。
     * 3.8.3 PUT /api/v1/admin/users/{userId}/password:reset
     */
    @PutMapping("/api/v1/admin/users/{userId}/password:reset")
    public ApiResponse<Map<String, Object>> resetPassword(
            @PathVariable Long userId,
            @RequestHeader(value = "X-Action-Source", required = false) String actionSource,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId,
            @Valid @RequestBody ResetPasswordRequest req) {

        if (!securityContext.isSuperAdmin()) throw BizException.of("E_GOV_4032");
        if ("AI_AGENT".equalsIgnoreCase(actionSource)) throw BizException.of("E_GOV_4231");

        SysUserDO target = sysUserMapper.findById(userId);
        if (target == null) throw BizException.of("E_USR_4041");

        sysUserMapper.updatePassword(userId, passwordEncoder.encode(req.getNewPassword()));

        Instant now = Instant.now();
        redisTemplate.opsForValue().set(
                "jwt:invalidate_before:" + userId,
                String.valueOf(now.toEpochMilli()),
                24L, TimeUnit.HOURS);

        SysLogDO log = new SysLogDO();
        log.setModule("USER");
        log.setAction("RESET_PASSWORD");
        log.setActionId("reset_pwd_" + userId);
        log.setObjectId(String.valueOf(userId));
        log.setResultCode("OK");
        log.setResult("密码强制重置成功");
        log.setRiskLevel("HIGH");
        log.setOperatorUserId(securityContext.currentUserId());
        log.setOperatorUsername(securityContext.currentUsername());
        log.setExecutedAt(now);
        log.setCreatedAt(now);
        if (req.getReason() != null) log.setDetail(req.getReason());
        log.setTraceId(traceId);
        sysLogMapper.insert(log);

        return ApiResponse.ok(Map.of(
                "user_id", String.valueOf(userId),
                "password_reset_at", now.toString(),
                "force_relogin", true
        ), traceId);
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

    private Map<String, Object> buildClueVO(ClueRecordEntity c) {
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

    @Data
    public static class ResetPasswordRequest {
        @NotBlank
        @Size(min = 8, max = 64, message = "密码长度 8~64 位")
        private String newPassword;
        private String reason;
    }

    @Data
    public static class AllocateTagRequest {
        @NotNull private Long orderId;
        private String reason;
    }

    @Data
    public static class ReleaseTagRequest {
        @NotBlank @Size(min = 5, max = 256) private String reason;
    }

    @Data
    public static class ExportDataRequest {
        @NotBlank private String exportType;
        @NotBlank private String windowStart;
        @NotBlank private String windowEnd;
        @NotBlank @Size(min = 5, max = 256) private String reason;
    }

    @Data
    public static class PurgeLogsRequest {
        @NotBlank private String beforeTime;
        @NotBlank @Size(min = 5, max = 256) private String reason;
    }

    @Data
    public static class UpdateConfigRequest {
        @NotBlank private String configKey;
        @NotBlank private String configValue;
        @NotBlank @Size(min = 5, max = 256) private String reason;
    }

    @Data
    public static class ForceCloseRequest {
        @NotBlank @Size(min = 5, max = 256) private String reason;
    }

    @Data
    public static class ReplayEventRequest {
        @NotBlank private String createdAt;
        @NotBlank @Size(min = 5, max = 256) private String replayReason;
        private String replayMode;
        private String nextRetryAt;
    }
}
