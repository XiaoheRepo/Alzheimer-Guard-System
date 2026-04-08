package com.xiaohelab.guard.server.domain.governance.service;

import com.xiaohelab.guard.server.common.exception.BizException;

import java.util.regex.Pattern;

/**
 * 用户策略领域服务。
 * 封装账号、密码、角色相关的纯业务规则，无 IO 操作。
 */
public class UserPolicyDomainService {

    /** 用户名：3-30 位字母、数字、下划线 */
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{3,30}$");

    /** PIN 码：4-6 位数字 */
    private static final Pattern PIN_PATTERN = Pattern.compile("^[0-9]{4,6}$");

    /** 密码最小长度 */
    private static final int MIN_PASSWORD_LENGTH = 8;

    /**
     * 断言用户名格式合规（3-30 位字母/数字/下划线）。
     *
     * @throws BizException E_AUTH_4001 — 用户名格式不合规
     */
    public void assertValidUsername(String username) {
        if (username == null || !USERNAME_PATTERN.matcher(username).matches()) {
            throw BizException.of("E_AUTH_4001");
        }
    }

    /**
     * 断言密码长度满足最低要求（≥ 8 位）。
     *
     * @throws BizException E_AUTH_4002 — 密码长度不足
     */
    public void assertValidPassword(String password) {
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            throw BizException.of("E_AUTH_4002");
        }
    }

    /**
     * 校验 PIN 码格式（4-6 位纯数字）。
     */
    public boolean isValidPin(String pin) {
        return pin != null && PIN_PATTERN.matcher(pin).matches();
    }
}
