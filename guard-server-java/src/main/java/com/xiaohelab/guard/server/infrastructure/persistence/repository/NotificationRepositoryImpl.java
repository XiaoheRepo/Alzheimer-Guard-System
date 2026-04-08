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
                .map(this::toEntity)
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
        NotificationInboxDO d = toDO(entity);
        mapper.insert(d);
        return toEntity(d);
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
                .map(this::toEntity)
                .collect(Collectors.toList());
    }

    @Override
    public long countByRelatedTaskId(Long taskId, String level) {
        return mapper.countByRelatedTaskId(taskId, level);
    }

    /** DO → Entity 转换 */
    private NotificationEntity toEntity(NotificationInboxDO d) {
        return NotificationEntity.reconstitute(
                d.getNotificationId(), d.getUserId(), d.getType(), d.getTitle(), d.getContent(),
                d.getLevel(), d.getRelatedTaskId(), d.getRelatedPatientId(),
                d.getReadStatus(), d.getReadAt(), d.getTraceId(),
                d.getCreatedAt(), d.getUpdatedAt());
    }

    /** Entity → DO 转换 */
    private NotificationInboxDO toDO(NotificationEntity e) {
        NotificationInboxDO d = new NotificationInboxDO();
        d.setNotificationId(e.getNotificationId());
        d.setUserId(e.getUserId());
        d.setType(e.getType());
        d.setTitle(e.getTitle());
        d.setContent(e.getContent());
        d.setLevel(e.getLevel());
        d.setRelatedTaskId(e.getRelatedTaskId());
        d.setRelatedPatientId(e.getRelatedPatientId());
        d.setReadStatus(e.getReadStatus());
        d.setReadAt(e.getReadAt());
        d.setTraceId(e.getTraceId());
        d.setCreatedAt(e.getCreatedAt());
        d.setUpdatedAt(e.getUpdatedAt());
        return d;
    }
}
