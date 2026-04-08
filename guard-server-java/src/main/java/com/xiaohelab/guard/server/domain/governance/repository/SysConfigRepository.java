package com.xiaohelab.guard.server.domain.governance.repository;

import com.xiaohelab.guard.server.domain.governance.entity.SysConfigEntity;

import java.util.List;
import java.util.Optional;

/**
 * 系统配置仓储接口（治理域）。
 */
public interface SysConfigRepository {

    Optional<SysConfigEntity> findByKey(String configKey);

    List<SysConfigEntity> listAll();

    List<SysConfigEntity> listByScope(String scope);

    /** 新增或更新配置项（ON CONFLICT upsert） */
    void upsert(SysConfigEntity entity);
}
