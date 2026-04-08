package com.xiaohelab.guard.server.domain.governance.repository;

import com.xiaohelab.guard.server.domain.governance.entity.SysLogEntity;

import java.util.List;

/**
 * 审计日志 Repository 接口（治理域）。
 */
public interface SysLogRepository {

    void insert(SysLogEntity log);

    List<SysLogEntity> listByFilter(int limit, int offset);

    List<SysLogEntity> listByModule(String module, int limit, int offset);

    long count();

    List<SysLogEntity> listByObjectId(String objectId, int limit, int offset);

    long countByObjectId(String objectId);

    List<SysLogEntity> listByModuleAndObjectId(String module, String objectId, int limit, int offset);

    long countByModuleAndObjectId(String module, String objectId);

    long purgeBeforeTime(String beforeTime);
}
