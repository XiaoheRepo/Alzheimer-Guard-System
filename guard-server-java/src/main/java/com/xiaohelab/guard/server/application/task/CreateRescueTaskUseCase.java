package com.xiaohelab.guard.server.application.task;

import com.xiaohelab.guard.server.common.exception.BizException;
import com.xiaohelab.guard.server.common.util.IdGenerator;
import com.xiaohelab.guard.server.domain.event.DomainEvent;
import com.xiaohelab.guard.server.domain.guardian.repository.GuardianRepository;
import com.xiaohelab.guard.server.domain.task.entity.RescueTaskEntity;
import com.xiaohelab.guard.server.domain.task.repository.RescueTaskRepository;
import com.xiaohelab.guard.server.infrastructure.outbox.OutboxWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 创建寻回任务用例。
 * 职责边界：幂等校验 → 归属权校验 → 唯一 ACTIVE 任务校验 → 持久化 + Outbox 同提交。
 * HC-01、HC-02、HC-03 约束均在此处落地。
 */
@Service
@RequiredArgsConstructor
public class CreateRescueTaskUseCase {

    private final RescueTaskRepository rescueTaskRepository;
    private final GuardianRepository guardianRepository;
    private final OutboxWriter outboxWriter;
    private final StringRedisTemplate redisTemplate;

    /**
     * 执行用例。
     *
     * @param requestId  幂等键（X-Request-Id）
     * @param userId     当前操作用户 ID
     * @param patientId  患者 ID
     * @param taskSource APP / MINI_PROGRAM / ADMIN_PORTAL
     * @param remark     可选备注
     * @return 新建的任务聚合根
     */
    @Transactional
    public RescueTaskEntity execute(String requestId, Long userId, Long patientId,
                                    String taskSource, String remark) {
        // 1. Redis 幂等拦截（HC-03）
        String idempotentKey = "idempotent:task:create:" + requestId;
        Boolean absent = redisTemplate.opsForValue()
                .setIfAbsent(idempotentKey, "1", 5, TimeUnit.MINUTES);
        if (Boolean.FALSE.equals(absent)) {
            throw BizException.of("E_REQ_4003");
        }

        // 2. 归属权校验：当前用户必须对患者有 ACTIVE 关联
        long relCount = guardianRepository.countActiveRelation(userId, patientId);
        if (relCount == 0) {
            throw BizException.of("E_TASK_4030");
        }

        // 3. 唯一进行中任务校验（同一患者至多 1 个 ACTIVE）
        rescueTaskRepository.findActiveByPatientId(patientId).ifPresent(t -> {
            throw BizException.of("E_TASK_4091");
        });

        // 4. 构建聚合根（内置事件生成）
        String taskNo = IdGenerator.taskNo();
        String eventId = IdGenerator.eventId();
        RescueTaskEntity task = RescueTaskEntity.create(taskNo, patientId, userId, taskSource, eventId);

        // 5. 持久化任务（Mapper 层 useGeneratedKeys 回填 id）
        rescueTaskRepository.save(task);

        // 6. 同事务写 Outbox（HC-02）
        List<DomainEvent> events = task.popPendingEvents();
        outboxWriter.writeAll(events);

        return task;
    }
}
