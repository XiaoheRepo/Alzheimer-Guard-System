package com.xiaohelab.guard.server.application.notification;

import com.xiaohelab.guard.server.common.exception.BizException;
import com.xiaohelab.guard.server.infrastructure.persistence.do_.NotificationInboxDO;
import com.xiaohelab.guard.server.infrastructure.persistence.mapper.NotificationInboxMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 通知收件箱应用服务。
 * HC-06 约束：通知兜底为推送消息与站内通知，不依赖短信。
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationInboxMapper notificationInboxMapper;

    /** 分页查询用户收件箱 */
    public List<NotificationInboxDO> listInbox(Long userId, int page, int size) {
        return notificationInboxMapper.listByUserId(userId, size, (page - 1) * size);
    }

    /** 查询未读数量 */
    public long countUnread(Long userId) {
        return notificationInboxMapper.countUnread(userId);
    }

    /**
     * 标记单条通知已读。
     * 只允许通知所有者操作（userId 作为归属校验条件）。
     */
    @Transactional
    public void markRead(Long notificationId, Long userId) {
        int affected = notificationInboxMapper.markRead(notificationId, userId);
        if (affected == 0) {
            throw BizException.of("E_NOTI_4041");
        }
    }

    /** 一键全部已读 */
    @Transactional
    public void markAllRead(Long userId) {
        notificationInboxMapper.markAllRead(userId);
    }

    /**
     * 写入通知到收件箱（由 Kafka 消费者调用，幂等写入）。
     * trace_id 作为幂等键，避免重复写入同一事件的通知。
     */
    @Transactional
    public void receiveNotification(NotificationInboxDO notification) {
        if (notificationInboxMapper.countBySourceEventId(
                notification.getUserId(), notification.getTraceId()) > 0) {
            // 已收到，幂等跳过
            return;
        }
        notificationInboxMapper.insert(notification);
    }

    /** 总数（分页用） */
    public long count(Long userId) {
        return notificationInboxMapper.countByUserId(userId);
    }

    /** 分页查询任务相关告警（供 RescueTaskController 使用） */
    public List<NotificationInboxDO> listAlertsByTask(Long taskId, String level, int pageSize, int offset) {
        return notificationInboxMapper.listByRelatedTaskId(taskId, level, pageSize, offset);
    }

    public long countAlertsByTask(Long taskId, String level) {
        return notificationInboxMapper.countByRelatedTaskId(taskId, level);
    }
}
