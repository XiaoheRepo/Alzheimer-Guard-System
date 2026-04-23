package com.xiaohelab.guard.server.gov.controller;

import com.xiaohelab.guard.server.common.annotation.Idempotent;
import com.xiaohelab.guard.server.common.dto.CursorResponse;
import com.xiaohelab.guard.server.common.dto.Result;
import com.xiaohelab.guard.server.common.error.ErrorCode;
import com.xiaohelab.guard.server.common.exception.BizException;
import com.xiaohelab.guard.server.common.security.AuthUser;
import com.xiaohelab.guard.server.common.security.SecurityUtil;
import com.xiaohelab.guard.server.gov.dto.DeadEventDto;
import com.xiaohelab.guard.server.outbox.entity.OutboxLogEntity;
import com.xiaohelab.guard.server.outbox.repository.OutboxLogRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Outbox 超管后台（API_V2.0.md §3.6.8）：
 * 查询 DEAD 消息 / 重放 / 统计。仅 SUPER_ADMIN 可访问。
 */
@Tag(name = "Admin.Outbox", description = "Outbox 消息状态管理（SUPER_ADMIN）")
@RestController
@RequestMapping("/api/v1/admin/super/outbox")
public class OutboxAdminController {

    private final OutboxLogRepository outboxRepository;

    public OutboxAdminController(OutboxLogRepository outboxRepository) {
        this.outboxRepository = outboxRepository;
    }

    /** GET /api/v1/admin/super/outbox/dead — API_V2.0.md §3.6.8 */
    @GetMapping("/dead")
    public Result<CursorResponse<DeadEventDto>> listDead(
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int limit) {
        assertSuperAdmin();
        int fetchSize = Math.min(limit, 100);
        List<OutboxLogEntity> rows = outboxRepository.findDeadCursor(cursor, fetchSize + 1);
        boolean hasNext = rows.size() > fetchSize;
        if (hasNext) rows = rows.subList(0, fetchSize);
        List<DeadEventDto> items = rows.stream().map(DeadEventDto::from).toList();
        String nextCursor = hasNext ? String.valueOf(rows.get(fetchSize - 1).getId()) : null;
        return Result.ok(CursorResponse.of(items, fetchSize, nextCursor, hasNext));
    }

    /** POST /api/v1/admin/super/outbox/dead/{eventId}/replay — API_V2.0.md §3.6.8 */
    @PostMapping("/dead/{eventId}/replay")
    @Idempotent
    @Transactional(rollbackFor = Exception.class)
    public Result<Map<String, String>> replay(
            @PathVariable String eventId,
            @RequestBody(required = false) Map<String, String> body) {
        assertSuperAdmin();
        OutboxLogEntity e = outboxRepository.findByEventId(eventId)
                .orElseThrow(() -> BizException.of(ErrorCode.E_GOV_4046));
        if (!"DEAD".equals(e.getPhase())) {
            throw BizException.of(ErrorCode.E_GOV_4096);
        }
        String replayId = UUID.randomUUID().toString();
        e.setPhase("RETRY");
        e.setRetryCount(0);
        e.setNextRetryAt(OffsetDateTime.now());
        e.setLastError(null);
        e.setReplayToken(replayId);
        e.setReplayReason(body != null ? body.get("reason") : null);
        e.setLastInterventionBy(SecurityUtil.current().getUserId());
        e.setLastInterventionAt(OffsetDateTime.now());
        outboxRepository.save(e);
        return Result.ok(Map.of("replay_id", replayId));
    }

    private void assertSuperAdmin() {
        AuthUser u = SecurityUtil.current();
        if (!u.isSuperAdmin()) throw BizException.of(ErrorCode.E_GOV_4030);
    }
}

