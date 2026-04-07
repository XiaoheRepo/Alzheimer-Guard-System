package com.xiaohelab.guard.server.application.user;

import com.xiaohelab.guard.server.common.exception.BizException;
import com.xiaohelab.guard.server.infrastructure.persistence.do_.SysUserDO;
import com.xiaohelab.guard.server.infrastructure.persistence.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户账户服务。
 * 当前提供修改密码能力；后续可扩展账号信息编辑。
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;

    /**
     * 修改密码。
     * 验证旧密码通过后更新 password_hash；不允许新旧密码相同。
     */
    @Transactional
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        SysUserDO user = sysUserMapper.findById(userId);
        if (user == null) {
            throw BizException.of("E_USER_4041");
        }

        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw BizException.of("E_AUTH_4001");
        }

        if (oldPassword.equals(newPassword)) {
            throw BizException.of("E_AUTH_4002", "新密码不能与旧密码相同");
        }

        sysUserMapper.updatePassword(userId, passwordEncoder.encode(newPassword));
    }
}
