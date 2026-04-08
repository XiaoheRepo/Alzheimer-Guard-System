package com.xiaohelab.guard.server.application.auth;

import com.xiaohelab.guard.server.common.exception.BizException;
import com.xiaohelab.guard.server.domain.governance.entity.SysUserEntity;
import com.xiaohelab.guard.server.domain.governance.repository.SysUserRepository;
import com.xiaohelab.guard.server.security.service.JwtTokenProvider;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;


/**
 * 用户登录用例。
 * 校验用户名/密码，验证通过后更新最后登录信息，返回 JWT。
 */
@Service
@RequiredArgsConstructor
public class LoginUseCase {

    private final SysUserRepository sysUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 执行登录。
     *
     * @param username 用户名
     * @param password 明文密码
     * @return 登录结果（JWT token + 用户基本信息）
     */
    public LoginResult execute(String username, String password) {
        // 1. 查询用户
        SysUserEntity user = sysUserRepository.findByUsername(username)
                .orElseThrow(() -> BizException.of("E_AUTH_4011"));

        // 2. 检查账号状态
        if ("BANNED".equals(user.getStatus())) {
            throw BizException.of("E_AUTH_4013");
        }

        // 3. 校验密码
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw BizException.of("E_AUTH_4011");
        }

        // 4. 更新最后登录时间与 IP
        sysUserRepository.updateLoginInfo(user.getId(), "");

        // 5. 生成 JWT
        String token = jwtTokenProvider.generate(
                user.getId(), user.getUsername(), user.getRole().name());

        return new LoginResult(token, user);
    }

    /** 登录结果 DTO */
    @Getter
    public static class LoginResult {
        private final String accessToken;
        private final Long userId;
        private final String username;
        private final String role;

        public LoginResult(String token, SysUserEntity user) {
            this.accessToken = token;
            this.userId = user.getId();
            this.username = user.getUsername();
            this.role = user.getRole().name();
        }
    }
}
