package com.xiaohelab.guard.server.infrastructure.persistence.repository;

import com.xiaohelab.guard.server.domain.governance.entity.OutboxEventEntity;
import com.xiaohelab.guard.server.domain.governance.repository.OutboxEventRepository;
import com.xiaohelab.guard.server.infrastructure.persistence.do_.SysOutboxLogDO;
import com.xiaohelab.guard.server.infrastructure.persistence.mapper.SysOutboxLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * OutboxEventRepository 基础设施实现。
 */
@Repository
@RequiredArgsConstructor
public class OutboxEventRepositoryImpl implements OutboxEventRepository {

    private final SysOutboxLogMapper outboxLogMapper;

    @Override
    public List<OutboxEventEntity> listDead(int limit, int offset) {
        return outboxLogMapper.listDead(limit, offset).stream().map(this::toEntity).toList();
    }

    @Override
    public long countDead() {
        return outboxLogMapper.countDead();
    }

    @Override
    public int replayDead(String eventId, Long operatorId, String replayReason, String replayToken) {
        return outboxLogMapper.replayDead(eventId, operatorId, replayReason, replayToken);
    }

    // ===== 转换方法 =====

    private OutboxEventEntity toEntity(SysOutboxLogDO d) {
        return OutboxEventEntity.reconstitute(
                d.getEventId(), d.getTopic(), d.getAggregateId(), d.getPartitionKey(),
                d.getPayload(), d.getRequestId(), d.getTraceId(), d.getPhase(),
                d.getRetryCount(), d.getNextRetryAt(), d.getLeaseOwner(), d.getLeaseUntil(),
                d.getSentAt(), d.getLastError(), d.getLastInterventionBy(),
                d.getLastInterventionAt(), d.getReplayReason(), d.getReplayToken(),
                d.getReplayedAt(), d.getCreatedAt(), d.getUpdatedAt());
    }
}
