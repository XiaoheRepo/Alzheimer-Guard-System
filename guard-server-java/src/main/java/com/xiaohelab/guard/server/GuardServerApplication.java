package com.xiaohelab.guard.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 阿尔兹海默症患者协同寻回系统 - 后端服务主入口。
 */
@SpringBootApplication
@EnableJpaAuditing
@EnableScheduling
@EnableAsync
public class GuardServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(GuardServerApplication.class, args);
    }
}
