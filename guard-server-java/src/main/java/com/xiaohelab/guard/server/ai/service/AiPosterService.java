package com.xiaohelab.guard.server.ai.service;

import com.xiaohelab.guard.server.common.error.ErrorCode;
import com.xiaohelab.guard.server.common.event.OutboxTopics;
import com.xiaohelab.guard.server.common.exception.BizException;
import com.xiaohelab.guard.server.common.security.AuthUser;
import com.xiaohelab.guard.server.common.security.SecurityUtil;
import com.xiaohelab.guard.server.outbox.service.OutboxService;
import com.xiaohelab.guard.server.patient.service.GuardianAuthorizationService;
import com.xiaohelab.guard.server.rescue.entity.RescueTaskEntity;
import com.xiaohelab.guard.server.rescue.repository.RescueTaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * AI 寻人海报生成（毕设阶段为占位实现：返回占位图 URL）。
 * 真正生产可接入 DashScope WanX / Stable Diffusion 并落到对象存储。
 */
@Service
public class AiPosterService {

    private final RescueTaskRepository taskRepository;
    private final GuardianAuthorizationService authorizationService;
    private final OutboxService outboxService;

    public AiPosterService(RescueTaskRepository taskRepository,
                           GuardianAuthorizationService authorizationService,
                           OutboxService outboxService) {
        this.taskRepository = taskRepository;
        this.authorizationService = authorizationService;
        this.outboxService = outboxService;
    }

    @Transactional(rollbackFor = Exception.class)
    public String generate(Long taskId, String template) {
        AuthUser user = SecurityUtil.current();
        RescueTaskEntity task = taskRepository.findById(taskId)
                .orElseThrow(() -> BizException.of(ErrorCode.E_TASK_4041));
        authorizationService.assertGuardian(user, task.getPatientId());

        // 生成占位 URL（真实项目应调用模型 + OSS 上传）
        String url = "https://assets.guard.local/poster/" + task.getTaskNo() + ".png?tpl="
                + (template != null ? template : "default");
        task.setPosterUrl(url);
        taskRepository.save(task);

        outboxService.publish(OutboxTopics.AI_POSTER_GENERATED, task.getTaskNo(),
                String.valueOf(task.getPatientId()),
                Map.of("task_id", taskId, "task_no", task.getTaskNo(), "poster_url", url));
        return url;
    }
}
