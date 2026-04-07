package com.xiaohelab.guard.server.application.auth;

import com.xiaohelab.guard.server.common.exception.BizException;
import com.xiaohelab.guard.server.infrastructure.persistence.do_.SysUserDO;
import com.xiaohelab.guard.server.infrastructure.persistence.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户注册用例。
 * 注册后不自动登录，返回账号信息；登录由 LoginUseCase 处理。
 */
@Service
@RequiredArgsConstructor
public class RegisterUseCase {

    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;

    /**
     * 执行注册。
     *
     * @param username 用户名（2-32 位字母数字）
     * @param password 明文密码（8-64 位）
     * @param phone    手机号（已脱敏存储）
     * @param realName 真实姓名
     */
    @Transactional
    public SysUserDO execute(String username, String password, String phone, String realName) {
        // 检查用户名是否已存在
        if (sysUserMapper.countByUsername(username) > 0) {
            throw BizException.of("E_USR_4090");
        }

        SysUserDO user = new SysUserDO();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setPhone(phone);
        user.setDisplayName(realName);  // displayName 对应界面显示名
        user.setRole("FAMILY");         // 默认角色：家属/监护人（枚举值 FAMILY）
        user.setStatus("NORMAL");

        sysUserMapper.insert(user);
        return user;
    }
}
