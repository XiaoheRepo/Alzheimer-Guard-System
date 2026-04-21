package com.xiaohelab.guard.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 阿尔兹海默症患者协同寻回系统 - 后端服务主入口。
 *
 * <p>配置说明：
 * <ul>
 *   <li>{@link EnableJpaRepositories} 显式指定 JPA Repository 扫描根包，避免与 Spring Data Redis
 *       的 Repository 基础设施在组件扫描阶段发生 Strict Mode 误判（表现为 "Could not safely identify
 *       store assignment for repository candidate interface ..." 的 INFO/WARN 日志）。</li>
 *   <li>通过 {@code exclude = RedisRepositoriesAutoConfiguration.class} 关闭 Redis Repository 自动装配：
 *       本项目仅使用 {@code RedisTemplate} 直接操作缓存，不存在 {@code @RedisHash} 实体/Redis Repository，
 *       关闭后可消除扫描冲突并缩短启动时间。</li>
 * </ul>
 */
@SpringBootApplication(exclude = { RedisRepositoriesAutoConfiguration.class })
@EnableJpaAuditing
@EnableJpaRepositories(basePackages = "com.xiaohelab.guard.server")
@EnableScheduling
@EnableAsync
public class GuardServerApplication {

    /**
     * 应用启动入口。
     * @param args JVM 启动参数（例如 {@code --spring.profiles.active=prod}）
     */
    public static void main(String[] args) {
        SpringApplication.run(GuardServerApplication.class, args);
    }
}
