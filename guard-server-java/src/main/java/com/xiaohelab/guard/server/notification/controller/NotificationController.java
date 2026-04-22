package com.xiaohelab.guard.server.notification.controller;

import com.xiaohelab.guard.server.common.dto.Result;
import com.xiaohelab.guard.server.common.error.ErrorCode;
import com.xiaohelab.guard.server.common.exception.BizException;
import com.xiaohelab.guard.server.common.annotation.Idempotent;
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

    /**
     * 全部已读（V2.1 §3.8.4.1）。
     * <p>入参：{@code type}（可选）、{@code before_time}（可选 ISO-8601）。</p>
     * <p>返回：{@code {affected_count, read_at}}。受 {@link Idempotent} 保护，HC-04 幂等。</p>
     */
    @PostMapping("/read-all")
    @Idempotent
    @Transactional(rollbackFor = Exception.class)
    public Result<Map<String, Object>> readAll(@RequestBody(required = false) Map<String, Object> body) {
        AuthUser user = SecurityUtil.current();
        String type = null;
        OffsetDateTime before = null;
        if (body != null) {
            Object t = body.get("type");
            if (t instanceof String s && !s.isBlank()) type = s;
            Object bt = body.get("before_time");
            if (bt instanceof String bs && !bs.isBlank()) {
                try {
                    before = OffsetDateTime.parse(bs);
                } catch (Exception ex) {
                    throw BizException.of(ErrorCode.E_REQ_4220, "before_time 非 ISO-8601");
                }
            }
        }
        OffsetDateTime now = OffsetDateTime.now();
        int affected = inboxRepository.markAllReadForUser(user.getUserId(), type, before, now);
        return Result.ok(Map.of("affected_count", affected, "read_at", now));
    }
}
