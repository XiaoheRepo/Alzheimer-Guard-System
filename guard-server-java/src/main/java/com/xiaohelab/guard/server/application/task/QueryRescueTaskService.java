package com.xiaohelab.guard.server.application.task;

import com.xiaohelab.guard.server.common.exception.BizException;
import com.xiaohelab.guard.server.domain.task.RescueTaskEntity;
import com.xiaohelab.guard.server.domain.task.RescueTaskRepository;
import com.xiaohelab.guard.server.infrastructure.persistence.do_.RescueTaskDO;
import com.xiaohelab.guard.server.infrastructure.persistence.mapper.RescueTaskMapper;
import com.xiaohelab.guard.server.infrastructure.persistence.mapper.SysUserPatientMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 查询寻回任务服务。
 * 只读操作，无事务强依赖，但包含归属权校验。
 */
@Service
@RequiredArgsConstructor
public class QueryRescueTaskService {

    private final RescueTaskRepository rescueTaskRepository;
    private final RescueTaskMapper rescueTaskMapper;
    private final SysUserPatientMapper sysUserPatientMapper;

    /** 查询任务详情（含归属权校验） */
    public RescueTaskEntity findById(Long taskId, Long userId, String userRole) {
        RescueTaskEntity task = rescueTaskRepository.findById(taskId)
                .orElseThrow(() -> BizException.of("E_TASK_4041"));

        boolean isAdmin = "ADMIN".equals(userRole) || "SUPER_ADMIN".equals(userRole);
        if (!isAdmin) {
            long relCount = sysUserPatientMapper.countActiveRelation(userId, task.getPatientId());
            if (relCount == 0) {
                throw BizException.of("E_TASK_4030");
            }
        }
        return task;
    }

    /** 分页查询患者历史任务 */
    public List<RescueTaskDO> listByPatient(Long patientId, Long userId, String userRole,
                                             int page, int size) {
        boolean isAdmin = "ADMIN".equals(userRole) || "SUPER_ADMIN".equals(userRole);
        if (!isAdmin) {
            long relCount = sysUserPatientMapper.countActiveRelation(userId, patientId);
            if (relCount == 0) {
                throw BizException.of("E_TASK_4030");
            }
        }
        int offset = (page - 1) * size;
        return rescueTaskMapper.listByPatientId(patientId, size, offset);
    }

    /** 统计患者任务总数 */
    public long countByPatient(Long patientId) {
        return rescueTaskMapper.countByPatientId(patientId);
    }
}
