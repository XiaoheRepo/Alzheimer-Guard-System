package com.xiaohelab.guard.server.infrastructure.persistence.repository;

import com.xiaohelab.guard.server.domain.ai.entity.AiSessionEntity;
import com.xiaohelab.guard.server.domain.ai.entity.AiSessionMessageEntity;
import com.xiaohelab.guard.server.domain.ai.repository.AiSessionRepository;
import com.xiaohelab.guard.server.infrastructure.persistence.do_.AiSessionDO;
import com.xiaohelab.guard.server.infrastructure.persistence.do_.AiSessionMessageDO;
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
        return Optional.ofNullable(d).map(this::toEntity);
    }

    @Override
    public List<AiSessionEntity> listByUserId(Long userId, Long patientId, int limit, int offset) {
        return sessionMapper.listByUserId(userId, patientId, limit, offset).stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
    }

    @Override
    public long countByUserId(Long userId, Long patientId) {
        return sessionMapper.countByUserId(userId, patientId);
    }

    @Override
    public List<AiSessionEntity> listAll(Long userId, Long patientId, int limit, int offset) {
        return sessionMapper.listAll(userId, patientId, limit, offset).stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
    }

    @Override
    public long countAll(Long userId, Long patientId) {
        return sessionMapper.countAll(userId, patientId);
    }

    @Override
    public void insert(AiSessionEntity entity) {
        sessionMapper.insert(toSessionDO(entity));
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
        messageMapper.insert(toMessageDO(msg));
    }

    @Override
    public List<AiSessionMessageEntity> listMessages(String sessionId, int limit, int offset) {
        return messageMapper.listBySessionId(sessionId, limit, offset).stream()
                .map(this::toMessageEntity)
                .collect(Collectors.toList());
    }

    @Override
    public long countMessages(String sessionId) {
        return messageMapper.countBySessionId(sessionId);
    }

    /** DO → Entity 转换（Session） */
    private AiSessionEntity toEntity(AiSessionDO d) {
        return AiSessionEntity.reconstitute(
                d.getId(), d.getSessionId(), d.getUserId(), d.getPatientId(), d.getTaskId(),
                d.getMessages(), d.getRequestTokens(), d.getResponseTokens(),
                d.getTokenUsage(), d.getTokenUsed(), d.getModelName(),
                d.getStatus(), d.getArchivedAt(), d.getVersion(),
                d.getCreatedAt(), d.getUpdatedAt());
    }

    /** Entity → DO 转换（Session） */
    private AiSessionDO toSessionDO(AiSessionEntity e) {
        AiSessionDO d = new AiSessionDO();
        d.setId(e.getId());
        d.setSessionId(e.getSessionId());
        d.setUserId(e.getUserId());
        d.setPatientId(e.getPatientId());
        d.setTaskId(e.getTaskId());
        d.setMessages(e.getMessages());
        d.setRequestTokens(e.getRequestTokens());
        d.setResponseTokens(e.getResponseTokens());
        d.setTokenUsage(e.getTokenUsage());
        d.setTokenUsed(e.getTokenUsed());
        d.setModelName(e.getModelName());
        d.setStatus(e.getStatus());
        d.setArchivedAt(e.getArchivedAt());
        d.setVersion(e.getVersion());
        return d;
    }

    /** DO → Entity 转换（Message） */
    private AiSessionMessageEntity toMessageEntity(AiSessionMessageDO d) {
        return AiSessionMessageEntity.reconstitute(
                d.getId(), d.getSessionId(), d.getSequenceNo(),
                d.getRole(), d.getContent(), d.getTokenUsage(), d.getCreatedAt());
    }

    /** Entity → DO 转换（Message） */
    private AiSessionMessageDO toMessageDO(AiSessionMessageEntity e) {
        AiSessionMessageDO d = new AiSessionMessageDO();
        d.setId(e.getId());
        d.setSessionId(e.getSessionId());
        d.setSequenceNo(e.getSequenceNo());
        d.setRole(e.getRole());
        d.setContent(e.getContent());
        d.setTokenUsage(e.getTokenUsage());
        return d;
    }
}
