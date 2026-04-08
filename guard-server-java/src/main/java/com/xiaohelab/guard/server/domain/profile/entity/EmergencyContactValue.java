package com.xiaohelab.guard.server.domain.profile.entity;

/**
 * 紧急联系人值对象（不可变）。
 * 在患者 QR 码 / NFC 扫码页面展示给第一响应者，
 * 包含：监护人 ID、姓名、联系电话、与患者的关系描述。
 */
public record EmergencyContactValue(
        Long guardianUserId,
        String name,
        String phone,
        String relation) {

    /**
     * 构建紧急联系人值对象。
     * phone 可为 null，表示监护人未留联系方式。
     */
    public static EmergencyContactValue of(Long userId, String name, String phone, String relation) {
        return new EmergencyContactValue(userId, name, phone, relation);
    }

    /** 是否有可拨打的联系电话 */
    public boolean hasPhone() {
        return phone != null && !phone.trim().isEmpty();
    }
}
