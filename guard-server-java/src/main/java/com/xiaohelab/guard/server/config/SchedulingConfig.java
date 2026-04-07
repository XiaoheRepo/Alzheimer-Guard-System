package com.xiaohelab.guard.server.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 调度任务配置。
 * 启用 Spring @Scheduled 支持（OutboxDispatcher 依赖此配置）。
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
