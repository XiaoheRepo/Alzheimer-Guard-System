package com.xiaohelab.guard.server.common.config;

import com.xiaohelab.guard.server.user.entity.UserEntity;
import com.xiaohelab.guard.server.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 应用启动时自动初始化超级管理员账户。
 * 若数据库中已存在同名用户则跳过，保证幂等安全。
 * 用户名/密码/邮箱可通过环境变量覆盖（见 application.yml guard.init.*）。
 */
@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${guard.init.super-admin-username:superadmin}")
    private String username;

    @Value("${guard.init.super-admin-password:Admin@2026!}")
    private String password;

    @Value("${guard.init.super-admin-email:admin@xiaohelab.com}")
    private String email;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 启动时检查超级管理员是否存在，不存在则自动创建。
     * @param args 启动参数（不使用）
     */
    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.existsByUsername(username)) {
            log.info("[DataInitializer] 超级管理员 '{}' 已存在，跳过初始化", username);
            return;
        }
        UserEntity admin = new UserEntity();
        admin.setUsername(username);
        admin.setEmail(email);
        admin.setEmailVerified(true);
        admin.setPasswordHash(passwordEncoder.encode(password));
        admin.setNickname("超级管理员");
        admin.setRole("SUPER_ADMIN");
        admin.setStatus("ACTIVE");
        userRepository.save(admin);
        log.info("[DataInitializer] 超级管理员 '{}' 初始化成功", username);
    }
}