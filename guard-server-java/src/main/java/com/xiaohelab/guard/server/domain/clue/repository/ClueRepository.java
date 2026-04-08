package com.xiaohelab.guard.server.domain.clue.repository;

import com.xiaohelab.guard.server.domain.clue.entity.ClueRecordEntity;

import java.util.List;
import java.util.Optional;

/**
 * 线索聚合根 Repository 接口（领域层定义，基础设施层实现）。
 */
public interface ClueRepository {

    Optional<ClueRecordEntity> findById(Long id);

    Optional<ClueRecordEntity> findByClueNo(String clueNo);

    List<ClueRecordEntity> listByTaskId(Long taskId, int limit, int offset);

    long countByTaskId(Long taskId);

    void insert(ClueRecordEntity entity);
}
