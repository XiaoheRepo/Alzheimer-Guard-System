package com.xiaohelab.guard.server.application.governance;

import com.xiaohelab.guard.server.domain.governance.entity.OutboxEventEntity;
import com.xiaohelab.guard.server.domain.governance.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Outbox DEAD 队列治理服务（超级管理员专属）。
 * 封装 DEAD 事件查询与受控重放操作。
 */
@Service
@RequiredArgsConstructor
public class OutboxGovernanceService {

    private final OutboxEventRepository outboxEventRepository;

    public List<OutboxEventEntity> listDead(int limit, int offset) {
        return outboxEventRepository.listDead(limit, offset);
    }

    public long countDead() {
        return outboxEventRepository.countDead();
    }

    /**
     * 将 DEAD 事件重置为 PENDING（受控重放）。
     *
     * @return 影响行数；0 表示事件不存在或已非 DEAD 状态
     */
    public int replayDead(String eventId, Long operatorId, String replayReason, String replayToken) {
        return outboxEventRepository.replayDead(eventId, operatorId, replayReason, replayToken);
    }
}
