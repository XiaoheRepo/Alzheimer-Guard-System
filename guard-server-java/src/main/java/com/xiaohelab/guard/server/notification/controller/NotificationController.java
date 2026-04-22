package com.xiaohelab.guard.server.notification.controller;

import com.xiaohelab.guard.server.common.dto.Result;
import com.xiaohelab.guard.server.common.error.ErrorCode;
import com.xiaohelab.guard.server.common.exception.BizException;
import com.xiaohelab.guard.server.common.annotation.Idempotent;
import com.xiaohelab.guard.server.common.dto.PagedResponse;
import com.xiaohelab.guard.server.common.security.AuthUser;
import com.xiaohelab.guard.server.common.security.SecurityUtil;
import com.xiaohelab.guard.server.notification.entity.NotificationInboxEntity;
import com.xiaohelab.guard.server.notification.repository.NotificationInboxRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/** 用户站内通知：分页拉取 / 未读数量 / 置为已读。 */
@Tag(name = "Notification", description = "用户站内通知")
@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationInboxRepository inboxRepository;

    public NotificationController(NotificationInboxRepository inboxRepository) {
        this.inboxRepository = inboxRepository;
    }

    /**
     * 3.6.11 通知收件箱（canonical: /inbox；兼容旧路径根路径 GET /notifications）。
     * <p>支持 read_status / type 筛选，Offset 分页（page_no + page_size）。</p>
     */
    @GetMapping({"/inbox", ""})
    @Operation(summary = "3.6.11 通知收件箱（Offset 分页）")
    public Result<PagedResponse<NotificationInboxEntity>> list(
            @RequestParam(name = "read_status", required = false) String readStatus,
            @RequestParam(required = false) String type,
            @RequestParam(name = "page_no", defaultValue = "1") int pageNo,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize) {
        AuthUser user = SecurityUtil.current();
        int idx = Math.max(pageNo, 1) - 1;
        int size = Math.min(Math.max(pageSize, 1), 100);
        PageRequest pr = PageRequest.of(idx, size);
        Page<NotificationInboxEntity> data;
        if (readStatus != null && type != null) {
            data = inboxRepository.findByUserIdAndReadStatusAndTypeOrderByCreatedAtDesc(
                    user.getUserId(), readStatus, type, pr);
        } else if (readStatus != null) {
            data = inboxRepository.findByUserIdAndReadStatusOrderByCreatedAtDesc(
                    user.getUserId(), readStatus, pr);
        } else if (type != null) {
            data = inboxRepository.findByUserIdAndTypeOrderByCreatedAtDesc(
                    user.getUserId(), type, pr);
        } else {
            data = inboxRepository.findByUserIdOrderByCreatedAtDesc(user.getUserId(), pr);
        }
        return Result.ok(PagedResponse.fromPage(data, pageNo, pageSize));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "未读通知数量")
    public Result<java.util.Map<String, Long>> unreadCount() {
        AuthUser user = SecurityUtil.current();
        long cnt = inboxRepository.countByUserIdAndReadStatus(user.getUserId(), "UNREAD");
        return Result.ok(java.util.Map.of("count", cnt));
    }

    /**
     * 3.6.12 标记单条通知已读（POST /{notification_id}/read）。
     */
    @PostMapping("/{notificationId}/read")
    @Idempotent
    @Transactional(rollbackFor = Exception.class)
    @Operation(summary = "3.6.12 标记单条通知已读")
    public Result<java.util.Map<String, Object>> markOneRead(@PathVariable Long notificationId) {
        AuthUser user = SecurityUtil.current();
        NotificationInboxEntity n = inboxRepository.findById(notificationId)
                .orElseThrow(() -> BizException.of(ErrorCode.E_NOTI_4041));
        if (!n.getUserId().equals(user.getUserId())) {
            throw BizException.of(ErrorCode.E_NOTI_4030);
        }
        java.time.OffsetDateTime now = java.time.OffsetDateTime.now();
        if ("UNREAD".equals(n.getReadStatus())) {
            n.setReadStatus("READ");
            n.setReadAt(now);
            inboxRepository.save(n);
        }
        return Result.ok(java.util.Map.of(
                "notification_id", String.valueOf(n.getNotificationId()),
                "read_status", n.getReadStatus(),
                "read_at", n.getReadAt() != null ? n.getReadAt() : now));
    }

    @PostMapping("/mark-read")
    @Transactional(rollbackFor = Exception.class)
    @Operation(summary = "批量标记已读（传 notification_ids 数组）")
    public Result<java.util.Map<String, Integer>> markRead(
            @RequestBody java.util.Map<String, java.util.List<Long>> body) {
        AuthUser user = SecurityUtil.current();
        java.util.List<Long> ids = body.get("notification_ids");
        if (ids == null || ids.isEmpty()) throw BizException.of(ErrorCode.E_REQ_4220);
        int updated = inboxRepository.markRead(user.getUserId(), ids, java.time.OffsetDateTime.now());
        return Result.ok(java.util.Map.of("updated", updated));
    }

    /**
     * 全部已读（V2.1 §3.8.4.1）。
     */
    @PostMapping("/read-all")
    @Idempotent
    @Transactional(rollbackFor = Exception.class)
    @Operation(summary = "全部已读")
    public Result<java.util.Map<String, Object>> readAll(
            @RequestBody(required = false) java.util.Map<String, Object> body) {
        AuthUser user = SecurityUtil.current();
        String type = null;
        java.time.OffsetDateTime before = null;
        if (body != null) {
            Object t = body.get("type");
            if (t instanceof String s && !s.isBlank()) type = s;
            Object bt = body.get("before_time");
            if (bt instanceof String bs && !bs.isBlank()) {
                try { before = java.time.OffsetDateTime.parse(bs); }
                catch (Exception ex) { throw BizException.of(ErrorCode.E_REQ_4220, "before_time 非 ISO-8601"); }
            }
        }
        java.time.OffsetDateTime now = java.time.OffsetDateTime.now();
        int affected = inboxRepository.markAllReadForUser(user.getUserId(), type, before, now);
        return Result.ok(java.util.Map.of("affected_count", affected, "read_at", now));
    }
}
