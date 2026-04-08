package com.xiaohelab.guard.server.infrastructure.persistence.repository;

import com.xiaohelab.guard.server.domain.clue.entity.ClueRecordEntity;
import com.xiaohelab.guard.server.domain.clue.repository.ClueRepository;
import com.xiaohelab.guard.server.infrastructure.persistence.do_.ClueRecordDO;
import com.xiaohelab.guard.server.infrastructure.persistence.mapper.ClueRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class ClueRepositoryImpl implements ClueRepository {

    private final ClueRecordMapper mapper;

    @Override
    public Optional<ClueRecordEntity> findById(Long id) {
        ClueRecordDO d = mapper.findById(id);
        return Optional.ofNullable(d).map(ClueRecordEntity::fromDO);
    }

    @Override
    public Optional<ClueRecordEntity> findByClueNo(String clueNo) {
        ClueRecordDO d = mapper.findByClueNo(clueNo);
        return Optional.ofNullable(d).map(ClueRecordEntity::fromDO);
    }

    @Override
    public List<ClueRecordEntity> listByTaskId(Long taskId, int limit, int offset) {
        return mapper.listByTaskId(taskId, limit, offset).stream()
                .map(ClueRecordEntity::fromDO)
                .collect(Collectors.toList());
    }

    @Override
    public long countByTaskId(Long taskId) {
        return mapper.countByTaskId(taskId);
    }

    @Override
    public void insert(ClueRecordEntity entity) {
        mapper.insert(entity.toDO());
    }
}
