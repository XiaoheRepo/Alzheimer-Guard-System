package com.xiaohelab.guard.server.notification.controller;

import com.xiaohelab.guard.server.common.dto.Result;
import com.xiaohelab.guard.server.common.error.ErrorCode;
import com.xiaohelab.guard.server.common.exception.BizException;
import com.xiaohelab.guard.server.common.security.AuthUser;
import com.xiaohelab.guard.server.common.security.SecurityUtil;
import com.xiaohelab.guard.server.notification.entity.NotificationInboxEntity;
import com.xiaohelab.guard.server.notification.repository.NotificationInboxRepository;
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

    @GetMapping
    public Result<Page<NotificationInboxEntity>> list(
            @RequestParam(required = false) String readStatus,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        AuthUser user = SecurityUtil.current();
        PageRequest pr = PageRequest.of(page, size);
        Page<NotificationInboxEntity> data = (readStatus != null)
                ? inboxRepository.findByUserIdAndReadStatusOrderByCreatedAtDesc(user.getUserId(), readStatus, pr)
                : inboxRepository.findByUserIdOrderByCreatedAtDesc(user.getUserId(), pr);
        return Result.ok(data);
    }

    @GetMapping("/unread-count")
    public Result<Map<String, Long>> unreadCount() {
        AuthUser user = SecurityUtil.current();
        long cnt = inboxRepository.countByUserIdAndReadStatus(user.getUserId(), "UNREAD");
        return Result.ok(Map.of("count", cnt));
    }

    @PostMapping("/mark-read")
    @Transactional(rollbackFor = Exception.class)
    public Result<Map<String, Integer>> markRead(@RequestBody Map<String, List<Long>> body) {
        AuthUser user = SecurityUtil.current();
        List<Long> ids = body.get("notification_ids");
        if (ids == null || ids.isEmpty()) throw BizException.of(ErrorCode.E_REQ_4220);
        int updated = inboxRepository.markRead(user.getUserId(), ids, OffsetDateTime.now());
        return Result.ok(Map.of("updated", updated));
    }
}
