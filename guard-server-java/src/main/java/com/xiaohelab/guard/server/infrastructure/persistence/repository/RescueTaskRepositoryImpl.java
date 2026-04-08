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
        return Optional.ofNullable(d).map(this::toEntity);
    }

    @Override
    public Optional<RescueTaskEntity> findByTaskNo(String taskNo) {
        RescueTaskDO d = rescueTaskMapper.findByTaskNo(taskNo);
        return Optional.ofNullable(d).map(this::toEntity);
    }

    @Override
    public Optional<RescueTaskEntity> findActiveByPatientId(Long patientId) {
        RescueTaskDO d = rescueTaskMapper.findActiveByPatientId(patientId);
        return Optional.ofNullable(d).map(this::toEntity);
    }

    @Override
    public RescueTaskEntity save(RescueTaskEntity entity) {
        RescueTaskDO d = toDO(entity);
        if (d.getId() == null) {
            rescueTaskMapper.insert(d);
            return toEntity(rescueTaskMapper.findById(d.getId()));
        } else {
            throw new UnsupportedOperationException("使用 closeConditionally 执行条件更新");
        }
    }

    @Override
    public int closeConditionally(RescueTaskEntity entity) {
        return rescueTaskMapper.closeConditionally(toDO(entity));
    }

    @Override
    public List<RescueTaskEntity> listByPatientId(Long patientId, int limit, int offset) {
        return rescueTaskMapper.listByPatientId(patientId, limit, offset).stream()
                .map(this::toEntity)
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
                .map(this::toEntity)
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

    /** DO → Entity 转换 */
    private RescueTaskEntity toEntity(RescueTaskDO d) {
        return RescueTaskEntity.reconstitute(
                d.getId(), d.getTaskNo(), d.getPatientId(), d.getCreatedBy(),
                d.getSource(), d.getStatus(), d.getCloseReason(), d.getRemark(),
                d.getEventVersion(), d.getAiAnalysisSummary(), d.getPosterUrl(),
                d.getCreatedAt(), d.getUpdatedAt(), d.getClosedAt());
    }

    /** Entity → DO 转换 */
    private RescueTaskDO toDO(RescueTaskEntity e) {
        RescueTaskDO d = new RescueTaskDO();
        d.setId(e.getId());
        d.setTaskNo(e.getTaskNo());
        d.setPatientId(e.getPatientId());
        d.setCreatedBy(e.getCreatedBy());
        d.setSource(e.getSource());
        d.setStatus(e.getStatusName());
        d.setCloseReason(e.getCloseReason());
        d.setRemark(e.getRemark());
        d.setEventVersion(e.getEventVersion());
        d.setClosedAt(e.getClosedAt());
        return d;
    }
}
