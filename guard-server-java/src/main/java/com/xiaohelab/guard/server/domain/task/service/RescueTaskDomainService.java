package com.xiaohelab.guard.server.domain.task.service;

import com.xiaohelab.guard.server.common.exception.BizException;
import com.xiaohelab.guard.server.domain.task.entity.RescueTaskEntity;

import java.util.Optional;

/**
 * 寻回任务领域服务。
 * 封装跨用例的任务域不变量，无 IO 操作。
 * HC-01 约束：任务状态的唯一权威来源，AI 不得绕过 RescueTaskEntity 直接修改任务状态。
 */
public class RescueTaskDomainService {

    /**
     * 断言患者可以创建新任务。
     * 领域不变量：同一患者至多只能有一个 ACTIVE 任务。
     *
     * @param existingActiveTask 当前激活任务（由 Repository 查询）
     * @throws BizException E_TASK_4091 — 患者已有进行中的任务
     */
    public void assertCanCreate(Optional<RescueTaskEntity> existingActiveTask) {
        if (existingActiveTask.isPresent()) {
            throw BizException.of("E_TASK_4091");
        }
    }

    /**
     * 断言任务当前处于 ACTIVE 状态（可进行关闭、追加线索等操作）。
     *
     * @param task 已加载的任务聚合根
     * @throws BizException E_TASK_4041 — 任务已关闭或不存在
     */
    public void assertActive(RescueTaskEntity task) {
        if (task.getStatus() != RescueTaskEntity.TaskStatus.ACTIVE) {
            throw BizException.of("E_TASK_4041");
        }
    }

    /**
     * 断言任务已关闭（仅用于历史只读操作的前置检查）。
     *
     * @throws BizException E_TASK_4093 — 任务仍为 ACTIVE
     */
    public void assertClosed(RescueTaskEntity task) {
        if (task.getStatus() != RescueTaskEntity.TaskStatus.CLOSED) {
            throw BizException.of("E_TASK_4093");
        }
    }
}
