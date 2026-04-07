package com.xiaohelab.guard.server.infrastructure.persistence.mapper;

import com.xiaohelab.guard.server.infrastructure.persistence.do_.SysOutboxLogDO;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * sys_outbox_log 数据访问层。
 * HC-02 约束：业务写操作必须在同一事务中写入此表（transactional outbox 模式）。
 * 主键为组合键 (event_id, created_at)，无自增 ID。
 * phase 字段：PENDING → DISPATCHING → SENT；失败路径：RETRY → DEAD。
 */
@Mapper
public interface SysOutboxLogMapper {

    /** 在业务事务中写入 outbox 记录（phase 默认 PENDING） */
    @Insert("INSERT INTO sys_outbox_log(event_id, topic, aggregate_id, partition_key, " +
            "payload, request_id, trace_id, phase, retry_count, created_at, updated_at) " +
            "VALUES(#{eventId}, #{topic}, #{aggregateId}, #{partitionKey}, #{payload}::jsonb, " +
            "#{requestId}, #{traceId}, 'PENDING', 0, NOW(), NOW())")
    void insert(SysOutboxLogDO log);

    /**
     * 抢占租约：将 PENDING 记录更新为 DISPATCHING，写入实例标识与租约截止时间。
     * 使用 WHERE phase='PENDING' 避免重复派发。
     */
    @Update("UPDATE sys_outbox_log SET phase='DISPATCHING', lease_owner=#{leaseOwner}, " +
            "lease_until=#{leaseUntil}, updated_at=NOW() " +
            "WHERE event_id=#{eventId} AND phase='PENDING'")
    int tryAcquireLease(SysOutboxLogDO log);

    /**
     * 查询待派发记录（PENDING 或租约已过期的 DISPATCHING）。
     * 按 created_at 升序保证分区内顺序。
     */
    @Select("SELECT event_id, topic, aggregate_id, partition_key, payload::text, request_id, " +
            "trace_id, phase, retry_count, next_retry_at, lease_owner, lease_until, " +
            "last_error, created_at, updated_at " +
            "FROM sys_outbox_log " +
            "WHERE phase = 'PENDING' OR (phase = 'DISPATCHING' AND lease_until < NOW()) " +
            "ORDER BY created_at LIMIT #{limit}")
    List<SysOutboxLogDO> listPendingOrExpired(@Param("limit") int limit);

    /** 标记派发成功（终态 SENT） */
    @Update("UPDATE sys_outbox_log SET phase='SENT', sent_at=NOW(), updated_at=NOW() " +
            "WHERE event_id=#{eventId} AND lease_owner=#{leaseOwner}")
    int markSent(@Param("eventId") String eventId, @Param("leaseOwner") String leaseOwner);

    /** 标记为重试（累加重试次数，清除租约） */
    @Update("UPDATE sys_outbox_log SET phase='RETRY', retry_count=retry_count+1, " +
            "last_error=#{lastError}, lease_owner=NULL, lease_until=NULL, " +
            "next_retry_at=NOW() + INTERVAL '1 minute' * retry_count, updated_at=NOW() " +
            "WHERE event_id=#{eventId} AND lease_owner=#{leaseOwner}")
    int markRetry(@Param("eventId") String eventId,
                  @Param("leaseOwner") String leaseOwner,
                  @Param("lastError") String lastError);

    /** 将 RETRY 状态中可重试的记录重新激活为 PENDING */
    @Update("UPDATE sys_outbox_log SET phase='PENDING', updated_at=NOW() " +
            "WHERE phase='RETRY' AND next_retry_at <= NOW()")
    int reactivateRetry();

    /** 永久失败（超过最大重试次数） */
    @Update("UPDATE sys_outbox_log SET phase='DEAD', updated_at=NOW() WHERE event_id=#{eventId}")
    int markDead(String eventId);

    /** event_id 幂等校验 */
    @Select("SELECT COUNT(*) FROM sys_outbox_log WHERE event_id = #{eventId}")
    long countByEventId(String eventId);
}
