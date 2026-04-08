package com.xiaohelab.guard.server.interfaces.notification;

import com.xiaohelab.guard.server.application.notification.NotificationService;
import com.xiaohelab.guard.server.common.response.ApiResponse;
import com.xiaohelab.guard.server.common.response.PageResponse;
import com.xiaohelab.guard.server.domain.notification.entity.NotificationEntity;
import com.xiaohelab.guard.server.security.config.SecurityContext;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 通知收件箱接口。
 * HC-06 约束：通知兜底为推送消息与站内通知，不依赖短信。
 * HC-05：实时通知通过 WebSocket 定向推送，此接口提供离线补偿查询能力。
 */
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final SecurityContext securityContext;

    /** 分页查询收件箱 */
    @GetMapping
    public ApiResponse<PageResponse<Map<String, Object>>> listNotifications(
            @RequestParam(defaultValue = "1") @Min(1) int pageNo,
            @RequestParam(defaultValue = "20") @Min(1) int pageSize,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId) {

        Long userId = securityContext.currentUserId();
        List<NotificationEntity> list = notificationService.listInbox(userId, pageNo, pageSize);
        long total = notificationService.count(userId);

        List<Map<String, Object>> items = list.stream().map(n -> Map.<String, Object>of(
                "notification_id", String.valueOf(n.getNotificationId()),
                "type", n.getType(),
                "title", n.getTitle(),
                "content", n.getContent(),
                "level", n.getLevel(),
                "read_status", n.getReadStatus(),
                "created_at", n.getCreatedAt() == null ? "" : n.getCreatedAt().toString()
        )).toList();

        return ApiResponse.ok(PageResponse.<Map<String, Object>>builder()
                .items(items).pageNo(pageNo).pageSize(pageSize)
                .total(total).hasNext(total > (long) pageNo * pageSize)
                .build(), traceId);
    }

    /** 查询未读数量 */
    @GetMapping("/unread-count")
    public ApiResponse<Map<String, Long>> unreadCount(
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId) {
        Long userId = securityContext.currentUserId();
        return ApiResponse.ok(Map.of("unread_count", notificationService.countUnread(userId)), traceId);
    }

    /** 标记单条已读 */
    @PutMapping("/{notificationId}/read")
    public ApiResponse<Void> markRead(
            @PathVariable Long notificationId,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId) {
        Long userId = securityContext.currentUserId();
        notificationService.markRead(notificationId, userId);
        return ApiResponse.ok(null, traceId);
    }

    /** 一键全部已读 */
    @PutMapping("/read-all")
    public ApiResponse<Void> markAllRead(
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId) {
        Long userId = securityContext.currentUserId();
        notificationService.markAllRead(userId);
        return ApiResponse.ok(null, traceId);
    }
}
