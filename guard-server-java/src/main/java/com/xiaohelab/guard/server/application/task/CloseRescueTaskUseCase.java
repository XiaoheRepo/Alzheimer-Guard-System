package com.xiaohelab.guard.server.application.task;

import com.xiaohelab.guard.server.common.exception.BizException;
import com.xiaohelab.guard.server.common.util.IdGenerator;
import com.xiaohelab.guard.server.domain.event.DomainEvent;
import com.xiaohelab.guard.server.domain.guardian.repository.GuardianRepository;
import com.xiaohelab.guard.server.domain.task.RescueTaskEntity;
import com.xiaohelab.guard.server.domain.task.RescueTaskEntity.CloseType;
import com.xiaohelab.guard.server.domain.task.RescueTaskRepository;
import com.xiaohelab.guard.server.infrastructure.outbox.OutboxWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 关闭寻回任务用例（RESOLVED / FALSE_ALARM）。
 * HC-01：只有 ACTIVE 任务可关闭；事件类型取决于关闭类型。
 * HC-02：任务状态更新 + Outbox 事件必须同事务提交。
 */
@Service
@RequiredArgsConstructor
public class CloseRescueTaskUseCase {

    private final RescueTaskRepository rescueTaskRepository;
    private final GuardianRepository guardianRepository;
    private final OutboxWriter outboxWriter;

    /**
     * 执行关闭。
     *
     * @param taskId       任务 ID
     * @param userId       操作用户 ID
     * @param userRole     操作用户角色（ADMIN 可强制关闭）
     * @param closeType    RESOLVED / FALSE_ALARM
     * @param reason       关闭原因（FALSE_ALARM 时 5-256 字符）
     * @param operatorNote 操作备注（可选）
     */
    @Transactional
    public RescueTaskEntity execute(Long taskId, Long userId, String userRole,
                                    CloseType closeType, String reason, String operatorNote) {
        // 1. 加载聚合根
        RescueTaskEntity task = rescueTaskRepository.findById(taskId)
                .orElseThrow(() -> BizException.of("E_TASK_4041"));

        // 2. 归属权校验（ADMIN 可跳过）
        boolean isAdmin = "ADMIN".equals(userRole) || "SUPER_ADMIN".equals(userRole);
        if (!isAdmin) {
            if (guardianRepository.countActiveRelation(userId, task.getPatientId()) == 0) {
                throw BizException.of("E_TASK_4030");
            }
        }

        // 3. 调用聚合根状态机方法（内部校验 ACTIVE 状态）
        task.close(closeType, reason, operatorNote,
                IdGenerator.eventId(), IdGenerator.eventId());

        // 4. 条件更新（WHERE status='ACTIVE' AND event_version=#{version}）
        int affected = rescueTaskRepository.closeConditionally(task);
        if (affected == 0) {
            // 并发冲突：状态已被其他线程变更
            throw BizException.of("E_TASK_4093");
        }

        // 5. 同事务写 Outbox（HC-02）
        List<DomainEvent> events = task.popPendingEvents();
        outboxWriter.writeAll(events);

        return task;
    }

    /**
     * 超级管理员强制关闭（不使用乐观锁，不写 Outbox）。
     * SUPERADMIN 专属操作，调用方须完成权限校验。
     *
     * @return 受影响行数（0 表示已关闭或不存在）
     */
    @Transactional
    public int forceCloseAdmin(Long taskId, String reason) {
        RescueTaskEntity task = rescueTaskRepository.findById(taskId)
                .orElseThrow(() -> BizException.of("E_TASK_4041"));
        if (task.getStatus() != RescueTaskEntity.TaskStatus.ACTIVE) {
            throw BizException.of("E_TASK_4093");
        }
        return rescueTaskRepository.forceClose(taskId, "SUPER_FORCE_CLOSE", reason);
    }
}
