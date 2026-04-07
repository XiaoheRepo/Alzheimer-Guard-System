package com.xiaohelab.guard.server.application.auth;

import com.xiaohelab.guard.server.common.exception.BizException;
import com.xiaohelab.guard.server.infrastructure.persistence.do_.SysUserDO;
import com.xiaohelab.guard.server.infrastructure.persistence.mapper.SysUserMapper;
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

    private final SysUserMapper sysUserMapper;
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
        SysUserDO user = sysUserMapper.findByUsername(username);
        if (user == null) {
            throw BizException.of("E_AUTH_4011");
        }

        // 2. 检查账号状态
        if ("BANNED".equals(user.getStatus())) {
            throw BizException.of("E_AUTH_4013");
        }

        // 3. 校验密码
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw BizException.of("E_AUTH_4011");
        }

        // 4. 更新最后登录时间与 IP（异步也可，此处同步写入）
        sysUserMapper.updateLoginInfo(user.getId(), "");

        // 5. 生成 JWT（userId 为 Long，与 JwtTokenProvider.generate 签名一致）
        String token = jwtTokenProvider.generate(
                user.getId(), user.getUsername(), user.getRole());

        return new LoginResult(token, user);
    }

    /** 登录结果 DTO */
    @Getter
    public static class LoginResult {
        private final String accessToken;
        private final Long userId;
        private final String username;
        private final String role;

        public LoginResult(String token, SysUserDO user) {
            this.accessToken = token;
            this.userId = user.getId();
            this.username = user.getUsername();
            this.role = user.getRole();
        }
    }
}
