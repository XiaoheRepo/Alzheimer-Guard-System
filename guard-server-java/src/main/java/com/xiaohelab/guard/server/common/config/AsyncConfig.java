package com.xiaohelab.guard.server.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 异步线程池配置：用于 {@code @Async} 标注的方法（如 Outbox 分发、AI 会话异步落盘）。
 * 推荐使用 {@link AsyncConfigurer} 接口；旧版 {@code AsyncConfigurerSupport} 已于 Spring 6 标记弃用。
 */
@Configuration
public class AsyncConfig implements AsyncConfigurer {

    @Bean(name = "guardAsyncExecutor")
    public Executor guardAsyncExecutor() {
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setCorePoolSize(4);
        ex.setMaxPoolSize(16);
        ex.setQueueCapacity(200);
        ex.setThreadNamePrefix("guard-async-");
        ex.initialize();
        return ex;
    }

    @Override
    public Executor getAsyncExecutor() {
        return guardAsyncExecutor();
    }
}
