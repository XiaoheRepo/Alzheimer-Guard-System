-- =====================================================================
-- V2 管理员治理增量（对应 SRS FR-GOV-011 ~ FR-GOV-014 / FR-PRO-011 / FR-PRO-012）
-- 参考：DBD §2.6.1、LLD §8.3.13（注销字段 #DEL_{ts} 后缀释放唯一约束）
-- =====================================================================

-- 1. sys_user 增加 deactivated_at 字段，用于记录逻辑注销时间
ALTER TABLE sys_user
    ADD COLUMN IF NOT EXISTS deactivated_at TIMESTAMPTZ;

COMMENT ON COLUMN sys_user.deactivated_at IS '逻辑注销时间（DEACTIVATED 终态时填入）';

-- 2. role + status 组合索引，配合管理员列表的强制过滤查询
CREATE INDEX IF NOT EXISTS idx_sys_user_role_status
    ON sys_user(role, status);
