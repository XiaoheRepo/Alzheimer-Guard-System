package com.xiaohelab.guard.server.common.util;

public final class MaskUtil {

    private MaskUtil() {}

    /**
     * 脱敏姓名：保留首字，其余用 * 替代
     */
    public static String maskName(String name) {
        if (name == null || name.isBlank()) return "**";
        if (name.length() == 1) return name;
        return name.charAt(0) + "*".repeat(name.length() - 1);
    }

    /**
     * 脱敏手机号：保留前3位和后4位
     */
    public static String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) return "***";
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
