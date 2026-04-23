-- =====================================================================
-- V6__sys_user_phone_unique.sql
-- 家属注册接入 phone 为必填字段（API V2.0 §3.6.1）。
-- 对 sys_user.phone 建立部分唯一索引：仅对非 NULL 行去重，保留历史
-- ADMIN / SUPER_ADMIN 种子账号（phone 允许为空）的兼容性。
-- 依据：DBD §2.6.1（phone 备注已同步更新）
-- 错误码：E_USR_4095 手机号已存在（409）
-- =====================================================================

CREATE UNIQUE INDEX IF NOT EXISTS ux_sys_user_phone
    ON sys_user(phone)
    WHERE phone IS NOT NULL;
