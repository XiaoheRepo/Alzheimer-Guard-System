package com.xiaohelab.guard.server.infrastructure.persistence.repository;

import com.xiaohelab.guard.server.domain.ai.entity.AiSessionEntity;
import com.xiaohelab.guard.server.domain.ai.entity.AiSessionMessageEntity;
import com.xiaohelab.guard.server.domain.ai.repository.AiSessionRepository;
import com.xiaohelab.guard.server.infrastructure.persistence.do_.AiSessionDO;
import com.xiaohelab.guard.server.infrastructure.persistence.mapper.AiSessionMapper;
import com.xiaohelab.guard.server.infrastructure.persistence.mapper.AiSessionMessageMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class AiSessionRepositoryImpl implements AiSessionRepository {

    private final AiSessionMapper sessionMapper;
    private final AiSessionMessageMapper messageMapper;

    @Override
    public Optional<AiSessionEntity> findBySessionId(String sessionId) {
        AiSessionDO d = sessionMapper.findBySessionId(sessionId);
        return Optional.ofNullable(d).map(AiSessionEntity::fromDO);
    }

    @Override
    public List<AiSessionEntity> listByUserId(Long userId, Long patientId, int limit, int offset) {
        return sessionMapper.listByUserId(userId, patientId, limit, offset).stream()
                .map(AiSessionEntity::fromDO)
                .collect(Collectors.toList());
    }

    @Override
    public long countByUserId(Long userId, Long patientId) {
        return sessionMapper.countByUserId(userId, patientId);
    }

    @Override
    public List<AiSessionEntity> listAll(Long userId, Long patientId, int limit, int offset) {
        return sessionMapper.listAll(userId, patientId, limit, offset).stream()
                .map(AiSessionEntity::fromDO)
                .collect(Collectors.toList());
    }

    @Override
    public long countAll(Long userId, Long patientId) {
        return sessionMapper.countAll(userId, patientId);
    }

    @Override
    public void insert(AiSessionEntity entity) {
        sessionMapper.insert(entity.toDO());
    }

    @Override
    public int archiveBySessionId(String sessionId) {
        return sessionMapper.archiveBySessionId(sessionId);
    }

    @Override
    public int casAddTokens(String sessionId, Long version, int addTokens) {
        return sessionMapper.casAddTokens(sessionId, version, addTokens);
    }

    @Override
    public int maxSequenceNo(String sessionId) {
        return messageMapper.maxSequenceNo(sessionId);
    }

    @Override
    public void insertMessage(AiSessionMessageEntity msg) {
        messageMapper.insert(msg.toDO());
    }

    @Override
    public List<AiSessionMessageEntity> listMessages(String sessionId, int limit, int offset) {
        return messageMapper.listBySessionId(sessionId, limit, offset).stream()
                .map(AiSessionMessageEntity::fromDO)
                .collect(Collectors.toList());
    }

    @Override
    public long countMessages(String sessionId) {
        return messageMapper.countBySessionId(sessionId);
    }
}
