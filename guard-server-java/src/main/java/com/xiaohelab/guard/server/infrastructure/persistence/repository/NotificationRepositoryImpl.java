package com.xiaohelab.guard.server.infrastructure.persistence.repository;

import com.xiaohelab.guard.server.domain.notification.entity.NotificationEntity;
import com.xiaohelab.guard.server.domain.notification.repository.NotificationRepository;
import com.xiaohelab.guard.server.infrastructure.persistence.do_.NotificationInboxDO;
import com.xiaohelab.guard.server.infrastructure.persistence.mapper.NotificationInboxMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

/**
 * NotificationRepository 基础设施层实现。
 * HC-06 约束：通知不依赖短信，使用站内通知与推送。
 */
@Repository
@RequiredArgsConstructor
public class NotificationRepositoryImpl implements NotificationRepository {

    private final NotificationInboxMapper mapper;

    @Override
    public List<NotificationEntity> listByUserId(Long userId, int limit, int offset) {
        return mapper.listByUserId(userId, limit, offset).stream()
                .map(NotificationEntity::fromDO)
                .collect(Collectors.toList());
    }

    @Override
    public long countByUserId(Long userId) {
        return mapper.countByUserId(userId);
    }

    @Override
    public long countUnread(Long userId) {
        return mapper.countUnread(userId);
    }

    @Override
    public long countBySourceEventId(Long userId, String traceId) {
        return mapper.countBySourceEventId(userId, traceId);
    }

    @Override
    public NotificationEntity insert(NotificationEntity entity) {
        NotificationInboxDO d = entity.toDO();
        mapper.insert(d);
        // MyBatis useGeneratedKeys 将 notificationId 回填到 d
        return NotificationEntity.fromDO(d);
    }

    @Override
    public int markRead(Long notificationId, Long userId) {
        return mapper.markRead(notificationId, userId);
    }

    @Override
    public int markAllRead(Long userId) {
        return mapper.markAllRead(userId);
    }

    @Override
    public List<NotificationEntity> listByRelatedTaskId(Long taskId, String level,
                                                         int limit, int offset) {
        return mapper.listByRelatedTaskId(taskId, level, limit, offset).stream()
                .map(NotificationEntity::fromDO)
                .collect(Collectors.toList());
    }

    @Override
    public long countByRelatedTaskId(Long taskId, String level) {
        return mapper.countByRelatedTaskId(taskId, level);
    }
}
