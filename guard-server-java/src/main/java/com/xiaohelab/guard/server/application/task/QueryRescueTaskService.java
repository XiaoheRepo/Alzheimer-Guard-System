package com.xiaohelab.guard.server.application.task;

import com.xiaohelab.guard.server.common.exception.BizException;
import com.xiaohelab.guard.server.domain.guardian.repository.GuardianRepository;
import com.xiaohelab.guard.server.domain.task.entity.RescueTaskEntity;
import com.xiaohelab.guard.server.domain.task.repository.RescueTaskRepository;
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
    private final GuardianRepository guardianRepository;

    /** 查询任务详情（含归属权校验） */
    public RescueTaskEntity findById(Long taskId, Long userId, String userRole) {
        RescueTaskEntity task = rescueTaskRepository.findById(taskId)
                .orElseThrow(() -> BizException.of("E_TASK_4041"));

        boolean isAdmin = "ADMIN".equals(userRole) || "SUPER_ADMIN".equals(userRole);
        if (!isAdmin) {
            if (guardianRepository.countActiveRelation(userId, task.getPatientId()) == 0) {
                throw BizException.of("E_TASK_4030");
            }
        }
        return task;
    }

    /** 分页查询患者历史任务 */
    public List<RescueTaskEntity> listByPatient(Long patientId, Long userId, String userRole,
                                                 int page, int size) {
        boolean isAdmin = "ADMIN".equals(userRole) || "SUPER_ADMIN".equals(userRole);
        if (!isAdmin) {
            if (guardianRepository.countActiveRelation(userId, patientId) == 0) {
                throw BizException.of("E_TASK_4030");
            }
        }
        int offset = (page - 1) * size;
        return rescueTaskRepository.listByPatientId(patientId, size, offset);
    }

    /** 统计患者任务总数 */
    public long countByPatient(Long patientId) {
        return rescueTaskRepository.countByPatientId(patientId);
    }

    /** 按状态统计任务数（供统计指标接口使用） */
    public long countByStatus(String status, String timeFrom, String timeTo) {
        return rescueTaskRepository.countByStatus(status, timeFrom, timeTo);
    }

    /** 管理端全量任务列表 */
    public List<RescueTaskEntity> listAll(String status, String source, int pageNo, int pageSize) {
        int offset = (pageNo - 1) * pageSize;
        return rescueTaskRepository.listAll(status, source, pageSize, offset);
    }

    /** 管理端全量任务计数 */
    public long countAll(String status, String source) {
        return rescueTaskRepository.countAll(status, source);
    }
}
