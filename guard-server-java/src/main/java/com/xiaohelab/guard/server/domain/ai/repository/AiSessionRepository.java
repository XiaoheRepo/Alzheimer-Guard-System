package com.xiaohelab.guard.server.domain.ai.repository;

import com.xiaohelab.guard.server.domain.ai.entity.AiSessionEntity;
import com.xiaohelab.guard.server.domain.ai.entity.AiSessionMessageEntity;

import java.util.List;
import java.util.Optional;

/**
 * AI 会话 Repository 接口（领域层定义，基础设施层实现）。
 * 同时承载会话消息的存取，避免为无状态机的消息值对象单独定义 Repository。
 */
public interface AiSessionRepository {

    // ===== 会话查询 =====

    Optional<AiSessionEntity> findBySessionId(String sessionId);

    List<AiSessionEntity> listByUserId(Long userId, Long patientId, int limit, int offset);

    long countByUserId(Long userId, Long patientId);

    List<AiSessionEntity> listAll(Long userId, Long patientId, int limit, int offset);

    long countAll(Long userId, Long patientId);

    // ===== 会话写入/更新 =====

    void insert(AiSessionEntity entity);

    /** 归档会话（status='ACTIVE' → 'ARCHIVED'），返回受影响行数。 */
    int archiveBySessionId(String sessionId);

    /** CAS 原子累加 token（WHERE version=#{version}），返回受影响行数。 */
    int casAddTokens(String sessionId, Long version, int addTokens);

    // ===== 消息操作 =====

    int maxSequenceNo(String sessionId);

    void insertMessage(AiSessionMessageEntity msg);

    List<AiSessionMessageEntity> listMessages(String sessionId, int limit, int offset);

    long countMessages(String sessionId);
}
