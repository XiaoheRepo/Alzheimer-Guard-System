package com.xiaohelab.guard.server.application.governance;

import com.xiaohelab.guard.server.infrastructure.persistence.do_.SysLogDO;
import com.xiaohelab.guard.server.infrastructure.persistence.mapper.SysLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 审计日志应用服务（治理域）。
 * 封装 sys_log 的分页读取、统计与超级管理员清理操作。
 * 日志写入散落在各业务服务中（application 层直接调用 SysLogMapper.insert 属于惯例允许）。
 */
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final SysLogMapper sysLogMapper;

    /** 分页查询日志；module 为空时查全量 */
    public List<SysLogDO> listLogs(String module, int limit, int offset) {
        if (module != null && !module.isBlank()) {
            return sysLogMapper.listByModule(module, limit, offset);
        }
        return sysLogMapper.listByFilter(limit, offset);
    }

    public long countLogs() {
        return sysLogMapper.count();
    }

    public List<SysLogDO> listByModuleAndObjectId(String module, String objectId,
                                                   int limit, int offset) {
        return sysLogMapper.listByModuleAndObjectId(module, objectId, limit, offset);
    }

    public long countByModuleAndObjectId(String module, String objectId) {
        return sysLogMapper.countByModuleAndObjectId(module, objectId);
    }

    /** 超级管理员清理过期审计日志（SUPERADMIN 专属操作） */
    public long purgeBefore(String beforeTime) {
        return sysLogMapper.purgeBeforeTime(beforeTime);
    }

    /** 写一条审计记录（如数据导出场景） */
    public void writeLog(SysLogDO log) {
        sysLogMapper.insert(log);
    }

    /** 按 objectId 查询审计记录（用于任务/线索审计轨迹） */
    public List<SysLogDO> listByObjectId(String objectId, int limit, int offset) {
        return sysLogMapper.listByObjectId(objectId, limit, offset);
    }

    public long countByObjectId(String objectId) {
        return sysLogMapper.countByObjectId(objectId);
    }
}
