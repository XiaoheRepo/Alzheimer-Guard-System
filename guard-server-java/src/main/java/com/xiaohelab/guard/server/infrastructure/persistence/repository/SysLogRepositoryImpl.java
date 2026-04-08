package com.xiaohelab.guard.server.infrastructure.persistence.repository;

import com.xiaohelab.guard.server.domain.governance.entity.SysLogEntity;
import com.xiaohelab.guard.server.domain.governance.repository.SysLogRepository;
import com.xiaohelab.guard.server.infrastructure.persistence.do_.SysLogDO;
import com.xiaohelab.guard.server.infrastructure.persistence.mapper.SysLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * SysLogRepository 基础设施实现。
 */
@Repository
@RequiredArgsConstructor
public class SysLogRepositoryImpl implements SysLogRepository {

    private final SysLogMapper sysLogMapper;

    @Override
    public void insert(SysLogEntity log) {
        SysLogDO d = toDO(log);
        sysLogMapper.insert(d);
    }

    @Override
    public List<SysLogEntity> listByFilter(int limit, int offset) {
        return sysLogMapper.listByFilter(limit, offset).stream().map(this::toEntity).toList();
    }

    @Override
    public List<SysLogEntity> listByModule(String module, int limit, int offset) {
        return sysLogMapper.listByModule(module, limit, offset).stream().map(this::toEntity).toList();
    }

    @Override
    public long count() {
        return sysLogMapper.count();
    }

    @Override
    public List<SysLogEntity> listByObjectId(String objectId, int limit, int offset) {
        return sysLogMapper.listByObjectId(objectId, limit, offset).stream().map(this::toEntity).toList();
    }

    @Override
    public long countByObjectId(String objectId) {
        return sysLogMapper.countByObjectId(objectId);
    }

    @Override
    public List<SysLogEntity> listByModuleAndObjectId(String module, String objectId, int limit, int offset) {
        return sysLogMapper.listByModuleAndObjectId(module, objectId, limit, offset).stream().map(this::toEntity).toList();
    }

    @Override
    public long countByModuleAndObjectId(String module, String objectId) {
        return sysLogMapper.countByModuleAndObjectId(module, objectId);
    }

    @Override
    public long purgeBeforeTime(String beforeTime) {
        return sysLogMapper.purgeBeforeTime(beforeTime);
    }

    // ===== 转换方法 =====

    private SysLogEntity toEntity(SysLogDO d) {
        return SysLogEntity.reconstitute(
                d.getId(), d.getModule(), d.getAction(), d.getActionId(),
                d.getResultCode(), d.getExecutedAt(), d.getOperatorUserId(),
                d.getOperatorUsername(), d.getObjectId(), d.getResult(),
                d.getRiskLevel(), d.getDetail(), d.getActionSource(),
                d.getAgentProfile(), d.getExecutionMode(), d.getConfirmLevel(),
                d.getBlockedReason(), d.getRequestId(), d.getTraceId(),
                d.getCreatedAt());
    }

    private SysLogDO toDO(SysLogEntity e) {
        SysLogDO d = new SysLogDO();
        d.setId(e.getId());
        d.setModule(e.getModule());
        d.setAction(e.getAction());
        d.setActionId(e.getActionId());
        d.setResultCode(e.getResultCode());
        d.setExecutedAt(e.getExecutedAt());
        d.setOperatorUserId(e.getOperatorUserId());
        d.setOperatorUsername(e.getOperatorUsername());
        d.setObjectId(e.getObjectId());
        d.setResult(e.getResult());
        d.setRiskLevel(e.getRiskLevel());
        d.setDetail(e.getDetail());
        d.setActionSource(e.getActionSource());
        d.setAgentProfile(e.getAgentProfile());
        d.setExecutionMode(e.getExecutionMode());
        d.setConfirmLevel(e.getConfirmLevel());
        d.setBlockedReason(e.getBlockedReason());
        d.setRequestId(e.getRequestId());
        d.setTraceId(e.getTraceId());
        d.setCreatedAt(e.getCreatedAt());
        return d;
    }
}
