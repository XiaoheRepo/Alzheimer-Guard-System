package com.xiaohelab.guard.server.infrastructure.persistence.repository;

import com.xiaohelab.guard.server.domain.governance.entity.SysConfigEntity;
import com.xiaohelab.guard.server.domain.governance.repository.SysConfigRepository;
import com.xiaohelab.guard.server.infrastructure.persistence.do_.SysConfigDO;
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
        return d == null ? Optional.empty() : Optional.of(toEntity(d));
    }

    @Override
    public List<SysConfigEntity> listAll() {
        return sysConfigMapper.listAll()
                .stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<SysConfigEntity> listByScope(String scope) {
        return sysConfigMapper.listByScope(scope)
                .stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
    }

    @Override
    public void upsert(SysConfigEntity entity) {
        sysConfigMapper.upsert(toDO(entity));
    }

    /** DO → Entity 转换 */
    private SysConfigEntity toEntity(SysConfigDO d) {
        return SysConfigEntity.reconstitute(
                d.getConfigKey(), d.getConfigValue(), d.getScope(),
                d.getUpdatedBy(), d.getUpdatedReason(),
                d.getCreatedAt(), d.getUpdatedAt());
    }

    /** Entity → DO 转换 */
    private SysConfigDO toDO(SysConfigEntity e) {
        SysConfigDO d = new SysConfigDO();
        d.setConfigKey(e.getConfigKey());
        d.setConfigValue(e.getConfigValue());
        d.setScope(e.getScope());
        d.setUpdatedBy(e.getUpdatedBy());
        d.setUpdatedReason(e.getUpdatedReason());
        return d;
    }
}
