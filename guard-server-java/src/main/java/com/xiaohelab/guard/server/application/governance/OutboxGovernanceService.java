package com.xiaohelab.guard.server.application.governance;

import com.xiaohelab.guard.server.infrastructure.persistence.do_.SysOutboxLogDO;
import com.xiaohelab.guard.server.infrastructure.persistence.mapper.SysOutboxLogMapper;
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

    private final SysOutboxLogMapper outboxLogMapper;

    public List<SysOutboxLogDO> listDead(int limit, int offset) {
        return outboxLogMapper.listDead(limit, offset);
    }

    public long countDead() {
        return outboxLogMapper.countDead();
    }

    /**
     * 将 DEAD 事件重置为 PENDING（受控重放）。
     *
     * @return 影响行数；0 表示事件不存在或已非 DEAD 状态
     */
    public int replayDead(String eventId, Long operatorId, String replayReason, String replayToken) {
        return outboxLogMapper.replayDead(eventId, operatorId, replayReason, replayToken);
    }
}
