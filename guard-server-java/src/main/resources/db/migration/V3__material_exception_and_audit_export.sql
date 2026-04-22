-- V3: 物流异常处置字段（LLD V2.0 §6.3.8, API §3.4.12）
-- FR-MAT-004 / SRS AC-07：记录 EXCEPTION 工单的补发/作废处置人、时间、行政理由。
ALTER TABLE tag_apply_record
    ADD COLUMN IF NOT EXISTS resolve_reason VARCHAR(256),
    ADD COLUMN IF NOT EXISTS resolved_by    BIGINT,
    ADD COLUMN IF NOT EXISTS resolved_at    TIMESTAMPTZ;

-- 审计日志导出 (FR-GOV-007) 的查询索引：按 created_at 范围 + operator_user_id 过滤
CREATE INDEX IF NOT EXISTS idx_sys_log_created_at ON sys_log(created_at);
CREATE INDEX IF NOT EXISTS idx_sys_log_operator   ON sys_log(operator_user_id);

-- 物流单号唯一性约束（LLD §6.3.3 / §6.3.8 要求校验全局唯一）
CREATE UNIQUE INDEX IF NOT EXISTS uq_tag_apply_record_logistics_no
    ON tag_apply_record(logistics_no) WHERE logistics_no IS NOT NULL;
