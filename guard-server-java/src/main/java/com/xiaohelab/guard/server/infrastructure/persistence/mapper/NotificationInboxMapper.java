package com.xiaohelab.guard.server.infrastructure.persistence.mapper;

import com.xiaohelab.guard.server.infrastructure.persistence.do_.NotificationInboxDO;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * notification_inbox 数据访问层。
 * 主键列名：notification_id（BIGSERIAL）。
 * HC-06 约束：通知兜底为推送消息与站内通知，不依赖短信。
 * 通知通过 WebSocket 定向下发（HC-05），同时持久化到收件箱供离线补偿。
 */
@Mapper
public interface NotificationInboxMapper {

    @Select("SELECT notification_id, user_id, type, title, content, level, " +
            "related_task_id, related_patient_id, read_status, read_at, trace_id, " +
            "created_at, updated_at " +
            "FROM notification_inbox WHERE notification_id = #{id}")
    NotificationInboxDO findById(Long id);

    /** 分页查询用户收件箱（最新在前） */
    @Select("SELECT notification_id, user_id, type, title, content, level, " +
            "related_task_id, related_patient_id, read_status, read_at, trace_id, " +
            "created_at, updated_at " +
            "FROM notification_inbox WHERE user_id = #{userId} " +
            "ORDER BY created_at DESC LIMIT #{limit} OFFSET #{offset}")
    List<NotificationInboxDO> listByUserId(@Param("userId") Long userId,
                                           @Param("limit") int limit,
                                           @Param("offset") int offset);

    @Select("SELECT COUNT(*) FROM notification_inbox WHERE user_id = #{userId}")
    long countByUserId(Long userId);

    /** 查询未读数量 */
    @Select("SELECT COUNT(*) FROM notification_inbox WHERE user_id = #{userId} AND read_status = 'UNREAD'")
    long countUnread(Long userId);

    /** trace_id 幂等校验，避免重复写入同一事件的通知 */
    @Select("SELECT COUNT(*) FROM notification_inbox " +
            "WHERE user_id = #{userId} AND trace_id = #{traceId}")
    long countBySourceEventId(@Param("userId") Long userId,
                              @Param("traceId") String traceId);

    /** 写入通知（由 Kafka 消费者触发，通知调度分发） */
    @Insert("INSERT INTO notification_inbox(user_id, type, title, content, level, " +
            "related_task_id, related_patient_id, read_status, trace_id, created_at, updated_at) " +
            "VALUES(#{userId}, #{type}, #{title}, #{content}, #{level}, " +
            "#{relatedTaskId}, #{relatedPatientId}, 'UNREAD', " +
            "#{traceId}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "notificationId")
    void insert(NotificationInboxDO notification);

    /** 标记单条已读（归属校验：user_id 在 WHERE 中防越权） */
    @Update("UPDATE notification_inbox SET read_status='READ', read_at=NOW(), updated_at=NOW() " +
            "WHERE notification_id=#{id} AND user_id=#{userId} AND read_status='UNREAD'")
    int markRead(@Param("id") Long id, @Param("userId") Long userId);

    /** 一键全部已读 */
    @Update("UPDATE notification_inbox SET read_status='READ', read_at=NOW(), updated_at=NOW() " +
            "WHERE user_id=#{userId} AND read_status='UNREAD'")
    int markAllRead(Long userId);
}
