package com.xiaohelab.guard.server.domain.notification.repository;

import com.xiaohelab.guard.server.domain.notification.entity.NotificationEntity;

import java.util.List;

/**
 * 通知收件箱 Repository 接口（通知域，基础设施层实现）。
 * HC-06 约束：通知不依赖短信，使用站内通知与推送。
 */
public interface NotificationRepository {

    /** 分页查询用户收件箱，最新在前 */
    List<NotificationEntity> listByUserId(Long userId, int limit, int offset);

    long countByUserId(Long userId);

    long countUnread(Long userId);

    /** trace_id 幂等校验 */
    long countBySourceEventId(Long userId, String traceId);

    /** 写入通知 */
    NotificationEntity insert(NotificationEntity entity);

    /** 标记单条已读，返回受影响行数（0 表示不存在或越权） */
    int markRead(Long notificationId, Long userId);

    /** 一键全部已读 */
    int markAllRead(Long userId);

    /** 按关联任务查询告警，支持 level 过滤 */
    List<NotificationEntity> listByRelatedTaskId(Long taskId, String level, int limit, int offset);

    long countByRelatedTaskId(Long taskId, String level);
}
