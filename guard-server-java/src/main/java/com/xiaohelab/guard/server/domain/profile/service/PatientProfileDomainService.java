package com.xiaohelab.guard.server.domain.profile.service;

import com.xiaohelab.guard.server.common.exception.BizException;

import java.util.regex.Pattern;

/**
 * 患者档案领域服务。
 * 封装档案创建、PIN 格式校验、资料完整性校验等纯业务规则，无 IO 操作。
 */
public class PatientProfileDomainService {

    /** PIN 码：4-6 位纯数字 */
    private static final Pattern PIN_PATTERN = Pattern.compile("^[0-9]{4,6}$");

    /**
     * 断言 PIN 码格式合规（4-6 位纯数字）。
     *
     * @throws BizException E_PRO_4001 — PIN 格式不合规
     */
    public void assertValidPinFormat(String pin) {
        if (pin == null || !PIN_PATTERN.matcher(pin).matches()) {
            throw BizException.of("E_PRO_4001");
        }
    }

    /**
     * 判断患者基本资料是否完整（name、gender、birthday 均不为空）。
     */
    public boolean isProfileComplete(String name, String gender, Object birthday) {
        return name != null && !name.trim().isEmpty()
                && gender != null && !gender.trim().isEmpty()
                && birthday != null;
    }

    /**
     * 断言性别值合规（MALE / FEMALE / UNKNOWN）。
     *
     * @throws BizException E_PRO_4002 — 性别枚举值不合规
     */
    public void assertValidGender(String gender) {
        if (!"MALE".equals(gender) && !"FEMALE".equals(gender) && !"UNKNOWN".equals(gender)) {
            throw BizException.of("E_PRO_4002");
        }
    }
}
