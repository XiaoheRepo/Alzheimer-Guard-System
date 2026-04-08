package com.xiaohelab.guard.server.infrastructure.persistence.repository;

import com.xiaohelab.guard.server.domain.task.entity.RescueTaskEntity;
import com.xiaohelab.guard.server.domain.task.repository.RescueTaskRepository;
import com.xiaohelab.guard.server.infrastructure.persistence.do_.RescueTaskDO;
import com.xiaohelab.guard.server.infrastructure.persistence.mapper.RescueTaskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * RescueTaskRepository 基础设施层实现。
 * 只负责 DO ↔ Entity 转换 + Mapper 调用，不持有任何业务逻辑。
 */
@Repository
@RequiredArgsConstructor
public class RescueTaskRepositoryImpl implements RescueTaskRepository {

    private final RescueTaskMapper rescueTaskMapper;

    @Override
    public Optional<RescueTaskEntity> findById(Long id) {
        RescueTaskDO d = rescueTaskMapper.findById(id);
        return Optional.ofNullable(d).map(RescueTaskEntity::fromDO);
    }

    @Override
    public Optional<RescueTaskEntity> findByTaskNo(String taskNo) {
        RescueTaskDO d = rescueTaskMapper.findByTaskNo(taskNo);
        return Optional.ofNullable(d).map(RescueTaskEntity::fromDO);
    }

    @Override
    public Optional<RescueTaskEntity> findActiveByPatientId(Long patientId) {
        RescueTaskDO d = rescueTaskMapper.findActiveByPatientId(patientId);
        return Optional.ofNullable(d).map(RescueTaskEntity::fromDO);
    }

    @Override
    public RescueTaskEntity save(RescueTaskEntity entity) {
        RescueTaskDO d = entity.toDO();
        if (d.getId() == null) {
            rescueTaskMapper.insert(d);
            // MyBatis useGeneratedKeys 将 id 回填到 d.id
            return RescueTaskEntity.fromDO(rescueTaskMapper.findById(d.getId()));
        } else {
            // update 仅用于非条件性更新场景（目前没有），通常用 closeConditionally
            throw new UnsupportedOperationException("使用 closeConditionally 执行条件更新");
        }
    }

    @Override
    public int closeConditionally(RescueTaskEntity entity) {
        return rescueTaskMapper.closeConditionally(entity.toDO());
    }

    @Override
    public List<RescueTaskEntity> listByPatientId(Long patientId, int limit, int offset) {
        return rescueTaskMapper.listByPatientId(patientId, limit, offset).stream()
                .map(RescueTaskEntity::fromDO)
                .collect(Collectors.toList());
    }

    @Override
    public long countByPatientId(Long patientId) {
        return rescueTaskMapper.countByPatientId(patientId);
    }

    @Override
    public long countByStatus(String status, String timeFrom, String timeTo) {
        return rescueTaskMapper.countByStatus(status, timeFrom, timeTo);
    }

    @Override
    public List<RescueTaskEntity> listAll(String status, String source, int limit, int offset) {
        return rescueTaskMapper.listAll(status, source, limit, offset).stream()
                .map(RescueTaskEntity::fromDO)
                .collect(Collectors.toList());
    }

    @Override
    public long countAll(String status, String source) {
        return rescueTaskMapper.countAll(status, source);
    }

    @Override
    public int forceClose(Long id, String closeReason, String remark) {
        return rescueTaskMapper.forceClose(id, closeReason, remark);
    }
}
