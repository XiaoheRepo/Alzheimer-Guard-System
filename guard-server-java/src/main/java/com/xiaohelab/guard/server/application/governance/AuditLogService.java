package com.xiaohelab.guard.server.application.governance;

import com.xiaohelab.guard.server.domain.governance.entity.SysLogEntity;
import com.xiaohelab.guard.server.domain.governance.repository.SysLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 审计日志应用服务（治理域）。
 * 封装 sys_log 的分页读取、统计与超级管理员清理操作。
 */
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final SysLogRepository sysLogRepository;

    /** 分页查询日志；module 为空时查全量 */
    public List<SysLogEntity> listLogs(String module, int limit, int offset) {
        if (module != null && !module.isBlank()) {
            return sysLogRepository.listByModule(module, limit, offset);
        }
        return sysLogRepository.listByFilter(limit, offset);
    }

    public long countLogs() {
        return sysLogRepository.count();
    }

    public List<SysLogEntity> listByModuleAndObjectId(String module, String objectId,
                                                      int limit, int offset) {
        return sysLogRepository.listByModuleAndObjectId(module, objectId, limit, offset);
    }

    public long countByModuleAndObjectId(String module, String objectId) {
        return sysLogRepository.countByModuleAndObjectId(module, objectId);
    }

    /** 超级管理员清理过期审计日志（SUPERADMIN 专属操作） */
    public long purgeBefore(String beforeTime) {
        return sysLogRepository.purgeBeforeTime(beforeTime);
    }

    /** 写一条审计记录 */
    public void writeLog(SysLogEntity log) {
        sysLogRepository.insert(log);
    }

    /** 按 objectId 查询审计记录（用于任务/线索审计轨迹） */
    public List<SysLogEntity> listByObjectId(String objectId, int limit, int offset) {
        return sysLogRepository.listByObjectId(objectId, limit, offset);
    }

    public long countByObjectId(String objectId) {
        return sysLogRepository.countByObjectId(objectId);
    }
}
