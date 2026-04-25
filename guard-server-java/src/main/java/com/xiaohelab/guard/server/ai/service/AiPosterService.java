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
    private final DashScopeClient dashScopeClient;

    public AiPosterService(RescueTaskRepository taskRepository,
                           GuardianAuthorizationService authorizationService,
                           OutboxService outboxService,
                           DashScopeClient dashScopeClient) {
        this.taskRepository = taskRepository;
        this.authorizationService = authorizationService;
        this.outboxService = outboxService;
        this.dashScopeClient = dashScopeClient;
    }

    @Transactional(rollbackFor = Exception.class)
    public String generate(Long taskId, String template) {
        AuthUser user = SecurityUtil.current();
        RescueTaskEntity task = taskRepository.findById(taskId)
                .orElseThrow(() -> BizException.of(ErrorCode.E_TASK_4041));
        authorizationService.assertGuardian(user, task.getPatientId());

        String url = null;
        if (dashScopeClient != null && dashScopeClient.isEnabled()) {
            String prompt = buildPosterPrompt(task, template);
            url = dashScopeClient.generateImage(prompt).orElse(null);
        }
        if (url == null || url.isBlank()) {
            // 降级：占位 URL
            url = "https://assets.guard.local/poster/" + task.getTaskNo() + ".png?tpl="
                    + (template != null ? template : "default");
        }
        task.setPosterUrl(url);
        taskRepository.save(task);

        outboxService.publish(OutboxTopics.AI_POSTER_GENERATED, task.getTaskNo(),
                String.valueOf(task.getPatientId()),
                Map.of("task_id", taskId, "task_no", task.getTaskNo(), "poster_url", url));
        return url;
    }

    private String buildPosterPrompt(RescueTaskEntity task, String template) {
        String tpl = (template != null && !template.isBlank()) ? template : "default";
        return "为阿尔兹海默症走失患者生成一张寻人海报背景图：温暖、希望、易识别，"
                + "风格 [" + tpl + "]，竖版构图，留出顶部 1/3 文字位，颜色稳重不刺眼。"
                + "任务编号：" + task.getTaskNo() + "。";
    }
}
