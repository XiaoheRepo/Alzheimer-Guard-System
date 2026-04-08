package com.xiaohelab.guard.server.infrastructure.persistence.repository;

import com.xiaohelab.guard.server.domain.governance.entity.SysConfigEntity;
import com.xiaohelab.guard.server.domain.governance.repository.SysConfigRepository;
import com.xiaohelab.guard.server.infrastructure.persistence.mapper.SysConfigMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 系统配置仓储实现（基础设施层）。
 */
@Repository
@RequiredArgsConstructor
public class SysConfigRepositoryImpl implements SysConfigRepository {

    private final SysConfigMapper sysConfigMapper;

    @Override
    public Optional<SysConfigEntity> findByKey(String configKey) {
        var d = sysConfigMapper.findByKey(configKey);
        return d == null ? Optional.empty() : Optional.of(SysConfigEntity.fromDO(d));
    }

    @Override
    public List<SysConfigEntity> listAll() {
        return sysConfigMapper.listAll()
                .stream()
                .map(SysConfigEntity::fromDO)
                .collect(Collectors.toList());
    }

    @Override
    public List<SysConfigEntity> listByScope(String scope) {
        return sysConfigMapper.listByScope(scope)
                .stream()
                .map(SysConfigEntity::fromDO)
                .collect(Collectors.toList());
    }

    @Override
    public void upsert(SysConfigEntity entity) {
        sysConfigMapper.upsert(entity.toDO());
    }
}
