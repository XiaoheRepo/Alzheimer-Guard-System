package com.xiaohelab.guard.server.gov.controller;

import com.xiaohelab.guard.server.common.annotation.Idempotent;
import com.xiaohelab.guard.server.common.dto.Result;
import com.xiaohelab.guard.server.common.error.ErrorCode;
import com.xiaohelab.guard.server.common.event.OutboxTopics;
import com.xiaohelab.guard.server.common.exception.BizException;
import com.xiaohelab.guard.server.common.security.AuthUser;
import com.xiaohelab.guard.server.common.security.SecurityUtil;
import com.xiaohelab.guard.server.outbox.entity.OutboxLogEntity;
import com.xiaohelab.guard.server.outbox.repository.OutboxLogRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;

/** Outbox 管理后台（Admin）：查询 DEAD 消息 / 重放 / 统计。 */
@Tag(name = "Admin.Outbox", description = "Outbox 消息状态管理（Admin）")
@RestController
@RequestMapping("/api/v1/admin/outbox")
public class OutboxAdminController {

    private final OutboxLogRepository outboxRepository;

    public OutboxAdminController(OutboxLogRepository outboxRepository) {
        this.outboxRepository = outboxRepository;
    }

    @GetMapping("/dead")
    public Result<Page<OutboxLogEntity>> listDead(@RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "20") int size) {
        assertAdmin();
        return Result.ok(outboxRepository.findByPhaseOrderByUpdatedAtDesc(
                "DEAD", PageRequest.of(page, size)));
    }

    @PostMapping("/{eventId}/replay")
    @Idempotent
    @Transactional(rollbackFor = Exception.class)
    public Result<OutboxLogEntity> replay(@PathVariable String eventId) {
        assertAdmin();
        OutboxLogEntity e = outboxRepository.findByEventId(eventId)
                .orElseThrow(() -> BizException.of(ErrorCode.E_GOV_4046));
        if (!"DEAD".equals(e.getPhase())) {
            throw BizException.of(ErrorCode.E_GOV_4096);
        }
        e.setPhase("RETRY");
        e.setRetryCount(0);
        e.setNextRetryAt(OffsetDateTime.now());
        e.setLastError(null);
        outboxRepository.save(e);
        return Result.ok(e);
    }

    private void assertAdmin() {
        AuthUser u = SecurityUtil.current();
        if (!u.isAdmin()) throw BizException.of(ErrorCode.E_GOV_4030);
    }
}
