package com.xiaohelab.guard.server.domain.governance.repository;

import com.xiaohelab.guard.server.domain.governance.entity.OutboxEventEntity;

import java.util.List;

/**
 * Outbox 事件 Repository 接口（治理域）。
 * 仅暴露 DEAD 队列查询与受控重放操作。
 */
public interface OutboxEventRepository {

    List<OutboxEventEntity> listDead(int limit, int offset);

    long countDead();

    int replayDead(String eventId, Long operatorId, String replayReason, String replayToken);
}
