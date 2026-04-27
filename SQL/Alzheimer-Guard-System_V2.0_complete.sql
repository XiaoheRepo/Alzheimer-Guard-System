-- =====================================================================
-- Alzheimer Guard System — 完整建表 DDL（PostGIS 增强 + DBD 全对齐版）
-- 版本：V2.0-postgis
-- 合并：V1~V7 Flyway 脚本 + 本次 PostGIS/DBD 全对齐修订
-- 基线：SRS V2.0 / SADD V2.0 / LLD V2.0 / DBD V2.0 / API V2.0
--
-- ★ 本次修订要点（相对上一版 V2.0_complete.sql）：
--   1) 全面启用 PostGIS：geometry(Point,4326) / geometry 替代 lat/lng
--   2) 移除所有物理 REFERENCES（DBD §1.5：不使用物理外键）
--   3) 严格对齐 DBD §2 每张表字段名/类型/约束，具体偏差见各表注释
--   4) sys_log / sys_outbox_log / consumed_event_log 改为分区表（DBD §2.6）
--   5) vector_store.embedding = vector(1536)，chunk_no 对齐 DBD §2.5.3
--   6) 全部列补充 COMMENT ON COLUMN
-- 目标：PostgreSQL 16 + PostGIS 3.4 + pgvector 0.7+ + pg_partman 5.x
-- =====================================================================

-- ============================================================
-- 0. 扩展启用
-- ============================================================
CREATE EXTENSION IF NOT EXISTS postgis;        -- 空间数据支持（PostGIS 3.4）
CREATE EXTENSION IF NOT EXISTS vector;         -- 向量检索（pgvector 0.7+）
CREATE EXTENSION IF NOT EXISTS pg_partman;     -- 分区自动管理（pg_partman 5.x）

-- =====================================================================
-- === GOV 域 ===
-- 表：sys_user, sys_config, sys_log*, sys_outbox_log*, consumed_event_log*,
--     notification_inbox, user_push_token, ws_ticket
-- * 分区表（PARTITION BY RANGE）
-- 基线：DBD §2.6, LLD §8
-- =====================================================================

-- ============================================================
-- 1. sys_user（用户表）
--    DBD §2.6.1, SRS FR-GOV-001/002/004
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_user (
    id              BIGSERIAL       PRIMARY KEY,
    username        VARCHAR(64)     NOT NULL,
    email           VARCHAR(128)    NOT NULL,
    email_verified  BOOLEAN         NOT NULL DEFAULT FALSE,
    password_hash   VARCHAR(128)    NOT NULL,
    nickname        VARCHAR(64),
    avatar_url      VARCHAR(1024),
    phone           VARCHAR(32),
    role            VARCHAR(32)     NOT NULL DEFAULT 'FAMILY',
    status          VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    last_login_at   TIMESTAMPTZ,
    last_login_ip   VARCHAR(64),
    deactivated_at  TIMESTAMPTZ,
    version         BIGINT          NOT NULL DEFAULT 0,
    trace_id        VARCHAR(64)     NOT NULL,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_sys_user_role   CHECK (role   IN ('FAMILY','ADMIN','SUPER_ADMIN')),
    CONSTRAINT ck_sys_user_status CHECK (status IN ('ACTIVE','DISABLED','DEACTIVATED'))
);
COMMENT ON TABLE  sys_user IS '用户表 — GOV 域聚合根，含注册、登录、角色管理（DBD §2.6.1）';
COMMENT ON COLUMN sys_user.id             IS '用户唯一 ID，自增主键';
COMMENT ON COLUMN sys_user.username       IS '登录用户名，全局唯一';
COMMENT ON COLUMN sys_user.email          IS 'PII: @Desensitize(EMAIL)（HC-07），全局唯一';
COMMENT ON COLUMN sys_user.email_verified IS '邮箱是否已完成验证';
COMMENT ON COLUMN sys_user.password_hash  IS 'BCrypt 哈希，不可逆（DBD §2.6.1）';
COMMENT ON COLUMN sys_user.nickname       IS '用户昵称，可选';
COMMENT ON COLUMN sys_user.avatar_url     IS '头像 URL，可选';
COMMENT ON COLUMN sys_user.phone          IS 'PII: @Desensitize(PHONE)（HC-07）；家属注册必填；ADMIN 种子可 NULL';
COMMENT ON COLUMN sys_user.role           IS '角色枚举：FAMILY / ADMIN / SUPER_ADMIN';
COMMENT ON COLUMN sys_user.status         IS '账户状态：ACTIVE(正常) / DISABLED(禁用) / DEACTIVATED(注销)';
COMMENT ON COLUMN sys_user.last_login_at  IS '最近一次登录时间';
COMMENT ON COLUMN sys_user.last_login_ip  IS '最近一次登录客户端 IP';
COMMENT ON COLUMN sys_user.deactivated_at IS '逻辑注销时间，DEACTIVATED 终态时填入（V2, DBD §2.6.1）';
COMMENT ON COLUMN sys_user.version        IS '行级乐观锁（HC-01），CAS 更新';
COMMENT ON COLUMN sys_user.trace_id       IS '全链路追踪标识（HC-04）';
COMMENT ON COLUMN sys_user.created_at     IS '记录创建时间（UTC）';
COMMENT ON COLUMN sys_user.updated_at     IS '记录最后更新时间（UTC）';

CREATE UNIQUE INDEX IF NOT EXISTS ux_sys_user_username   ON sys_user(username);
CREATE UNIQUE INDEX IF NOT EXISTS ux_sys_user_email      ON sys_user(email);
CREATE UNIQUE INDEX IF NOT EXISTS ux_sys_user_phone      ON sys_user(phone) WHERE phone IS NOT NULL;
CREATE INDEX        IF NOT EXISTS idx_sys_user_role_status ON sys_user(role, status);

-- ============================================================
-- 2. sys_config（系统配置表）
--    DBD §2.6.5, SRS FR-GOV-008, HC-05
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_config (
    config_key      VARCHAR(128)    PRIMARY KEY,
    config_value    TEXT            NOT NULL,
    scope           VARCHAR(32)     NOT NULL DEFAULT 'public',
    description     VARCHAR(512),
    updated_by      BIGINT,           -- FK逻辑: ref sys_user(id)
    updated_reason  VARCHAR(256),
    trace_id        VARCHAR(64)     NOT NULL,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_config_scope CHECK (scope IN ('public','ops','security','ai_policy'))
);
COMMENT ON TABLE  sys_config IS '系统配置表 — 动态配置中心（HC-05），所有业务阈值从此表读取（DBD §2.6.5）';
COMMENT ON COLUMN sys_config.config_key     IS '配置键，全局唯一主键（如 ai.embedding.model_dimension）';
COMMENT ON COLUMN sys_config.config_value   IS '配置值（文本格式，应用层解析为具体类型）';
COMMENT ON COLUMN sys_config.scope          IS '作用域：public / ops / security / ai_policy';
COMMENT ON COLUMN sys_config.description    IS '配置项说明';
COMMENT ON COLUMN sys_config.updated_by     IS '最后修改人 user_id（FK逻辑: ref sys_user.id）';
COMMENT ON COLUMN sys_config.updated_reason IS '本次修改原因';
COMMENT ON COLUMN sys_config.trace_id       IS '全链路追踪标识（HC-04）';
COMMENT ON COLUMN sys_config.created_at     IS '记录创建时间（UTC）';
COMMENT ON COLUMN sys_config.updated_at     IS '记录最后更新时间（UTC）';

CREATE INDEX IF NOT EXISTS idx_sys_config_scope ON sys_config(scope);

-- ============================================================
-- 3. sys_log（审计日志表）— 按 created_at 月份 RANGE 分区
--    DBD §2.6.2, SRS FR-GOV-006/007, FR-AI-011
--    保留 180 天（pg_partman 管理）
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_log (
    id                  BIGINT          GENERATED BY DEFAULT AS IDENTITY,
    module              VARCHAR(64)     NOT NULL,
    action              VARCHAR(64)     NOT NULL,
    action_id           VARCHAR(64),
    result_code         VARCHAR(64),
    executed_at         TIMESTAMPTZ,
    operator_user_id    BIGINT,          -- FK逻辑: ref sys_user(id)
    operator_username   VARCHAR(64),
    object_id           VARCHAR(64),
    result              VARCHAR(20),
    risk_level          VARCHAR(20),
    detail              JSONB,
    action_source       VARCHAR(20),
    agent_profile       VARCHAR(64),
    execution_mode      VARCHAR(20),
    confirm_level       VARCHAR(20),
    blocked_reason      VARCHAR(128),
    ip                  VARCHAR(64),
    request_id          VARCHAR(64),
    trace_id            VARCHAR(64)     NOT NULL,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    PRIMARY KEY (id, created_at),
    CONSTRAINT ck_log_result       CHECK (result       IS NULL OR result       IN ('SUCCESS','FAIL')),
    CONSTRAINT ck_log_risk_level   CHECK (risk_level   IS NULL OR risk_level   IN ('LOW','MEDIUM','HIGH','CRITICAL')),
    CONSTRAINT ck_log_action_source CHECK (action_source IS NULL OR action_source IN ('USER','AI_AGENT'))
) PARTITION BY RANGE (created_at);

COMMENT ON TABLE  sys_log IS '审计日志表 — 按月 RANGE 分区，保留 180 天（DBD §2.6.2，FR-GOV-007）';
COMMENT ON COLUMN sys_log.id               IS '日志 ID（分区表组合主键之一）';
COMMENT ON COLUMN sys_log.module           IS '操作模块（如 TASK / CLUE / PROFILE）';
COMMENT ON COLUMN sys_log.action           IS '操作动作（如 CREATE_TASK / CLOSE_TASK）';
COMMENT ON COLUMN sys_log.action_id        IS '操作流水号，与 request_id 对应';
COMMENT ON COLUMN sys_log.result_code      IS '业务结果码（如 E_TASK_4040）';
COMMENT ON COLUMN sys_log.executed_at      IS '操作执行时间（由应用写入，区别于 created_at 落库时间）';
COMMENT ON COLUMN sys_log.operator_user_id IS '操作用户 ID（FK逻辑: ref sys_user.id）';
COMMENT ON COLUMN sys_log.operator_username IS '操作用户名（冗余，避免 JOIN）';
COMMENT ON COLUMN sys_log.object_id        IS '被操作资源 ID';
COMMENT ON COLUMN sys_log.result           IS '操作结果：SUCCESS / FAIL';
COMMENT ON COLUMN sys_log.risk_level       IS '风险等级：LOW / MEDIUM / HIGH / CRITICAL';
COMMENT ON COLUMN sys_log.detail           IS '操作详情 JSONB（请求参数、变更快照等）';
COMMENT ON COLUMN sys_log.action_source    IS '操作来源：USER（人工）/ AI_AGENT（AI 代理）';
COMMENT ON COLUMN sys_log.agent_profile    IS 'AI Agent 身份标识（action_source=AI_AGENT 时必填）';
COMMENT ON COLUMN sys_log.execution_mode   IS 'AI 执行模式（DIRECT / SUPERVISED）';
COMMENT ON COLUMN sys_log.confirm_level    IS 'AI 确认级别（CONFIRM_1 / CONFIRM_2 / CONFIRM_3）';
COMMENT ON COLUMN sys_log.blocked_reason   IS '操作被阻断的原因（风险拦截时填入）';
COMMENT ON COLUMN sys_log.ip               IS '客户端 IP 地址';
COMMENT ON COLUMN sys_log.request_id       IS '请求 ID（X-Request-Id，HC-04）';
COMMENT ON COLUMN sys_log.trace_id         IS '全链路追踪标识（HC-04）';
COMMENT ON COLUMN sys_log.created_at       IS '落库时间（分区键，UTC）';

-- 初始分区（2026-01 ~ 2027-06，pg_partman 接管后续分区）
CREATE TABLE IF NOT EXISTS sys_log_y2026m01 PARTITION OF sys_log FOR VALUES FROM ('2026-01-01') TO ('2026-02-01');
CREATE TABLE IF NOT EXISTS sys_log_y2026m02 PARTITION OF sys_log FOR VALUES FROM ('2026-02-01') TO ('2026-03-01');
CREATE TABLE IF NOT EXISTS sys_log_y2026m03 PARTITION OF sys_log FOR VALUES FROM ('2026-03-01') TO ('2026-04-01');
CREATE TABLE IF NOT EXISTS sys_log_y2026m04 PARTITION OF sys_log FOR VALUES FROM ('2026-04-01') TO ('2026-05-01');
CREATE TABLE IF NOT EXISTS sys_log_y2026m05 PARTITION OF sys_log FOR VALUES FROM ('2026-05-01') TO ('2026-06-01');
CREATE TABLE IF NOT EXISTS sys_log_y2026m06 PARTITION OF sys_log FOR VALUES FROM ('2026-06-01') TO ('2026-07-01');
CREATE TABLE IF NOT EXISTS sys_log_y2026m07 PARTITION OF sys_log FOR VALUES FROM ('2026-07-01') TO ('2026-08-01');
CREATE TABLE IF NOT EXISTS sys_log_y2026m08 PARTITION OF sys_log FOR VALUES FROM ('2026-08-01') TO ('2026-09-01');
CREATE TABLE IF NOT EXISTS sys_log_y2026m09 PARTITION OF sys_log FOR VALUES FROM ('2026-09-01') TO ('2026-10-01');
CREATE TABLE IF NOT EXISTS sys_log_y2026m10 PARTITION OF sys_log FOR VALUES FROM ('2026-10-01') TO ('2026-11-01');
CREATE TABLE IF NOT EXISTS sys_log_y2026m11 PARTITION OF sys_log FOR VALUES FROM ('2026-11-01') TO ('2026-12-01');
CREATE TABLE IF NOT EXISTS sys_log_y2026m12 PARTITION OF sys_log FOR VALUES FROM ('2026-12-01') TO ('2027-01-01');
CREATE TABLE IF NOT EXISTS sys_log_y2027m01 PARTITION OF sys_log FOR VALUES FROM ('2027-01-01') TO ('2027-02-01');
CREATE TABLE IF NOT EXISTS sys_log_y2027m02 PARTITION OF sys_log FOR VALUES FROM ('2027-02-01') TO ('2027-03-01');
CREATE TABLE IF NOT EXISTS sys_log_y2027m03 PARTITION OF sys_log FOR VALUES FROM ('2027-03-01') TO ('2027-04-01');

CREATE INDEX IF NOT EXISTS idx_sys_log_module_action ON sys_log(module, action, created_at);
CREATE INDEX IF NOT EXISTS idx_sys_log_operator      ON sys_log(operator_user_id, created_at);
CREATE INDEX IF NOT EXISTS idx_sys_log_trace_id      ON sys_log(trace_id);
CREATE INDEX IF NOT EXISTS idx_sys_log_action_source ON sys_log(action_source) WHERE action_source = 'AI_AGENT';

-- ============================================================
-- 4. sys_outbox_log（Outbox 事件表）— 按 created_at 月份 RANGE 分区
--    DBD §2.6.3, SADD HC-02, LLD §8.1.3
--    主键 (event_id, created_at)，保留 90 天
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_outbox_log (
    event_id             VARCHAR(64)     NOT NULL,
    topic                VARCHAR(128)    NOT NULL,
    aggregate_id         VARCHAR(64),
    partition_key        VARCHAR(64),
    payload              JSONB           NOT NULL,
    request_id           VARCHAR(64),
    trace_id             VARCHAR(64)     NOT NULL,
    phase                VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    retry_count          INT             NOT NULL DEFAULT 0,
    next_retry_at        TIMESTAMPTZ,
    lease_owner          VARCHAR(64),
    lease_until          TIMESTAMPTZ,
    sent_at              TIMESTAMPTZ,
    last_error           VARCHAR(512),
    last_intervention_by BIGINT,          -- FK逻辑: ref sys_user(id)
    last_intervention_at TIMESTAMPTZ,
    replay_reason        VARCHAR(256),
    replay_token         VARCHAR(64),
    replayed_at          TIMESTAMPTZ,
    created_at           TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    PRIMARY KEY (event_id, created_at),
    CONSTRAINT ck_outbox_phase CHECK (phase IN ('PENDING','DISPATCHING','SENT','RETRY','DEAD'))
) PARTITION BY RANGE (created_at);

COMMENT ON TABLE  sys_outbox_log IS 'Outbox 事件表 — Local Transaction + Outbox Pattern（HC-02），按月分区，保留 90 天（DBD §2.6.3）';
COMMENT ON COLUMN sys_outbox_log.event_id             IS '事件唯一 ID（UUID v4），组合主键之一';
COMMENT ON COLUMN sys_outbox_log.topic                IS '目标消息 Topic（如 rescue.task.status.changed）';
COMMENT ON COLUMN sys_outbox_log.aggregate_id         IS '聚合根 ID（如 task_id）';
COMMENT ON COLUMN sys_outbox_log.partition_key        IS '消息分区键（保证同聚合根顺序消费）';
COMMENT ON COLUMN sys_outbox_log.payload              IS '事件负载 JSONB（完整事件 envelope）';
COMMENT ON COLUMN sys_outbox_log.request_id           IS '触发该事件的请求 ID（X-Request-Id）';
COMMENT ON COLUMN sys_outbox_log.trace_id             IS '全链路追踪标识（HC-04）';
COMMENT ON COLUMN sys_outbox_log.phase                IS '状态机：PENDING → DISPATCHING → SENT；失败 → RETRY → DEAD';
COMMENT ON COLUMN sys_outbox_log.retry_count          IS '已重试次数';
COMMENT ON COLUMN sys_outbox_log.next_retry_at        IS '下次重试时间（指数退避：min(base*2^n+jitter, 30s)）';
COMMENT ON COLUMN sys_outbox_log.lease_owner          IS '租约持有者标识（Dispatcher 节点 ID）';
COMMENT ON COLUMN sys_outbox_log.lease_until          IS '租约过期时间（超时后可被其他 Dispatcher 接管）';
COMMENT ON COLUMN sys_outbox_log.sent_at              IS '成功发出时间';
COMMENT ON COLUMN sys_outbox_log.last_error           IS '最后一次错误详情';
COMMENT ON COLUMN sys_outbox_log.last_intervention_by IS '最后人工介入操作员（FK逻辑: ref sys_user.id）';
COMMENT ON COLUMN sys_outbox_log.last_intervention_at IS '最后人工介入时间';
COMMENT ON COLUMN sys_outbox_log.replay_reason        IS '重放原因（人工介入触发重放时填入）';
COMMENT ON COLUMN sys_outbox_log.replay_token         IS '重放幂等令牌';
COMMENT ON COLUMN sys_outbox_log.replayed_at          IS '重放执行时间';
COMMENT ON COLUMN sys_outbox_log.created_at           IS '落库时间（分区键，UTC）';
COMMENT ON COLUMN sys_outbox_log.updated_at           IS '最后更新时间（UTC）';

CREATE TABLE IF NOT EXISTS sys_outbox_log_y2026m04 PARTITION OF sys_outbox_log FOR VALUES FROM ('2026-04-01') TO ('2026-05-01');
CREATE TABLE IF NOT EXISTS sys_outbox_log_y2026m05 PARTITION OF sys_outbox_log FOR VALUES FROM ('2026-05-01') TO ('2026-06-01');
CREATE TABLE IF NOT EXISTS sys_outbox_log_y2026m06 PARTITION OF sys_outbox_log FOR VALUES FROM ('2026-06-01') TO ('2026-07-01');
CREATE TABLE IF NOT EXISTS sys_outbox_log_y2026m07 PARTITION OF sys_outbox_log FOR VALUES FROM ('2026-07-01') TO ('2026-08-01');
CREATE TABLE IF NOT EXISTS sys_outbox_log_y2026m08 PARTITION OF sys_outbox_log FOR VALUES FROM ('2026-08-01') TO ('2026-09-01');
CREATE TABLE IF NOT EXISTS sys_outbox_log_y2026m09 PARTITION OF sys_outbox_log FOR VALUES FROM ('2026-09-01') TO ('2026-10-01');
CREATE TABLE IF NOT EXISTS sys_outbox_log_y2026m10 PARTITION OF sys_outbox_log FOR VALUES FROM ('2026-10-01') TO ('2026-11-01');
CREATE TABLE IF NOT EXISTS sys_outbox_log_y2026m11 PARTITION OF sys_outbox_log FOR VALUES FROM ('2026-11-01') TO ('2026-12-01');
CREATE TABLE IF NOT EXISTS sys_outbox_log_y2026m12 PARTITION OF sys_outbox_log FOR VALUES FROM ('2026-12-01') TO ('2027-01-01');
CREATE TABLE IF NOT EXISTS sys_outbox_log_y2027m01 PARTITION OF sys_outbox_log FOR VALUES FROM ('2027-01-01') TO ('2027-02-01');
CREATE TABLE IF NOT EXISTS sys_outbox_log_y2027m02 PARTITION OF sys_outbox_log FOR VALUES FROM ('2027-02-01') TO ('2027-03-01');

CREATE INDEX IF NOT EXISTS idx_outbox_phase_retry   ON sys_outbox_log(phase, next_retry_at) WHERE phase IN ('PENDING','RETRY');
CREATE INDEX IF NOT EXISTS idx_outbox_topic_partition ON sys_outbox_log(topic, partition_key);
CREATE INDEX IF NOT EXISTS idx_outbox_lease         ON sys_outbox_log(lease_owner, lease_until) WHERE phase = 'DISPATCHING';

-- ============================================================
-- 5. consumed_event_log（消费幂等日志表）— 按 processed_at 月份分区
--    DBD §2.6.4，保留 90 天
-- ============================================================
CREATE TABLE IF NOT EXISTS consumed_event_log (
    id            BIGINT          GENERATED BY DEFAULT AS IDENTITY,
    consumer_name VARCHAR(64)     NOT NULL,
    topic         VARCHAR(128)    NOT NULL,
    event_id      VARCHAR(64)     NOT NULL,
    partition_no  INT,
    msg_offset    BIGINT,
    trace_id      VARCHAR(64)     NOT NULL,
    processed_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    PRIMARY KEY (id, processed_at)
) PARTITION BY RANGE (processed_at);

COMMENT ON TABLE  consumed_event_log IS '消费幂等日志表 — 按月分区，保障事件消费者幂等性，保留 90 天（DBD §2.6.4）';
COMMENT ON COLUMN consumed_event_log.id            IS '日志 ID（分区表组合主键之一）';
COMMENT ON COLUMN consumed_event_log.consumer_name IS '消费者服务名称（如 clue-intake-service）';
COMMENT ON COLUMN consumed_event_log.topic         IS '消费的 Topic 名称';
COMMENT ON COLUMN consumed_event_log.event_id      IS '已消费事件 ID，与 sys_outbox_log.event_id 对应';
COMMENT ON COLUMN consumed_event_log.partition_no  IS '消息 Partition 编号（来自 Redis Streams / Kafka）';
COMMENT ON COLUMN consumed_event_log.msg_offset    IS '消息偏移量';
COMMENT ON COLUMN consumed_event_log.trace_id      IS '全链路追踪标识（HC-04）';
COMMENT ON COLUMN consumed_event_log.processed_at  IS '消费处理时间（分区键，UTC）';

CREATE TABLE IF NOT EXISTS consumed_event_log_y2026m04 PARTITION OF consumed_event_log FOR VALUES FROM ('2026-04-01') TO ('2026-05-01');
CREATE TABLE IF NOT EXISTS consumed_event_log_y2026m05 PARTITION OF consumed_event_log FOR VALUES FROM ('2026-05-01') TO ('2026-06-01');
CREATE TABLE IF NOT EXISTS consumed_event_log_y2026m06 PARTITION OF consumed_event_log FOR VALUES FROM ('2026-06-01') TO ('2026-07-01');
CREATE TABLE IF NOT EXISTS consumed_event_log_y2026m07 PARTITION OF consumed_event_log FOR VALUES FROM ('2026-07-01') TO ('2026-08-01');
CREATE TABLE IF NOT EXISTS consumed_event_log_y2026m08 PARTITION OF consumed_event_log FOR VALUES FROM ('2026-08-01') TO ('2026-09-01');
CREATE TABLE IF NOT EXISTS consumed_event_log_y2026m09 PARTITION OF consumed_event_log FOR VALUES FROM ('2026-09-01') TO ('2026-10-01');
CREATE TABLE IF NOT EXISTS consumed_event_log_y2026m10 PARTITION OF consumed_event_log FOR VALUES FROM ('2026-10-01') TO ('2026-11-01');
CREATE TABLE IF NOT EXISTS consumed_event_log_y2026m11 PARTITION OF consumed_event_log FOR VALUES FROM ('2026-11-01') TO ('2026-12-01');
CREATE TABLE IF NOT EXISTS consumed_event_log_y2026m12 PARTITION OF consumed_event_log FOR VALUES FROM ('2026-12-01') TO ('2027-01-01');
CREATE TABLE IF NOT EXISTS consumed_event_log_y2027m01 PARTITION OF consumed_event_log FOR VALUES FROM ('2027-01-01') TO ('2027-02-01');

CREATE UNIQUE INDEX IF NOT EXISTS uq_consumer_event ON consumed_event_log(consumer_name, topic, event_id, processed_at);

-- ============================================================
-- 6. notification_inbox（通知收件箱表）
--    DBD §2.6.6, SRS FR-GOV-010
-- ============================================================
CREATE TABLE IF NOT EXISTS notification_inbox (
    notification_id    BIGSERIAL       PRIMARY KEY,
    user_id            BIGINT          NOT NULL,  -- FK逻辑: ref sys_user(id)
    type               VARCHAR(32)     NOT NULL,
    title              VARCHAR(128)    NOT NULL,
    content            TEXT,
    level              VARCHAR(16)     NOT NULL DEFAULT 'INFO',
    channel            VARCHAR(20)     NOT NULL,
    related_task_id    BIGINT,                    -- FK逻辑: ref rescue_task(id)
    related_patient_id BIGINT,                    -- FK逻辑: ref patient_profile(id)
    read_status        VARCHAR(16)     NOT NULL DEFAULT 'UNREAD',
    read_at            TIMESTAMPTZ,
    source_event_id    VARCHAR(64),
    trace_id           VARCHAR(64)     NOT NULL,
    created_at         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_inbox_type CHECK (type IN (
        'TASK_PROGRESS','FENCE_ALERT','TASK_CLOSED','MISSING_PENDING_ALERT',
        'TAG_SUSPECTED_LOST','TRANSFER_REQUEST','SYSTEM'
    )),
    CONSTRAINT ck_inbox_level       CHECK (level       IN ('INFO','WARN','CRITICAL')),
    CONSTRAINT ck_inbox_channel     CHECK (channel     IN ('WEBSOCKET','JPUSH','EMAIL','INBOX')),
    CONSTRAINT ck_inbox_read_status CHECK (read_status IN ('UNREAD','READ'))
);
COMMENT ON TABLE  notification_inbox IS '通知收件箱表 — 多渠道通知落库，支持已读/未读查询（DBD §2.6.6）';
COMMENT ON COLUMN notification_inbox.notification_id    IS '通知 ID，自增主键';
COMMENT ON COLUMN notification_inbox.user_id            IS '接收用户 ID（FK逻辑: ref sys_user.id）';
COMMENT ON COLUMN notification_inbox.type               IS '通知类型枚举（见 CONSTRAINT）';
COMMENT ON COLUMN notification_inbox.title              IS '通知标题';
COMMENT ON COLUMN notification_inbox.content            IS '通知正文（富文本 / 纯文本）';
COMMENT ON COLUMN notification_inbox.level              IS '紧急程度：INFO / WARN / CRITICAL';
COMMENT ON COLUMN notification_inbox.channel            IS '推送渠道：WEBSOCKET / JPUSH / EMAIL / INBOX';
COMMENT ON COLUMN notification_inbox.related_task_id    IS '关联搜救任务 ID（FK逻辑: ref rescue_task.id）';
COMMENT ON COLUMN notification_inbox.related_patient_id IS '关联患者 ID（FK逻辑: ref patient_profile.id）';
COMMENT ON COLUMN notification_inbox.read_status        IS '阅读状态：UNREAD / READ';
COMMENT ON COLUMN notification_inbox.read_at            IS '已读时间';
COMMENT ON COLUMN notification_inbox.source_event_id    IS '触发该通知的 Outbox 事件 ID（用于溯源）';
COMMENT ON COLUMN notification_inbox.trace_id           IS '全链路追踪标识（HC-04）';
COMMENT ON COLUMN notification_inbox.created_at         IS '通知创建时间（UTC）';
COMMENT ON COLUMN notification_inbox.updated_at         IS '记录最后更新时间（UTC）';

CREATE INDEX IF NOT EXISTS idx_inbox_user_unread ON notification_inbox(user_id, read_status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_inbox_patient     ON notification_inbox(related_patient_id);

-- ============================================================
-- 7. user_push_token（推送令牌表）
--    V4, DBD §2.6.7, API §3.8.5
-- ============================================================
CREATE TABLE IF NOT EXISTS user_push_token (
    push_token_id  BIGSERIAL       PRIMARY KEY,
    user_id        BIGINT          NOT NULL,  -- FK逻辑: ref sys_user(id)
    platform       VARCHAR(24)     NOT NULL,
    device_id      VARCHAR(128)    NOT NULL,
    push_token     VARCHAR(512)    NOT NULL,
    app_version    VARCHAR(32)     NOT NULL,
    os_version     VARCHAR(64),
    device_model   VARCHAR(64),
    locale         VARCHAR(16)     NOT NULL DEFAULT 'zh-CN',
    status         VARCHAR(16)     NOT NULL DEFAULT 'ACTIVE',
    last_active_at TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    revoked_at     TIMESTAMPTZ,
    trace_id       VARCHAR(64)     NOT NULL,
    created_at     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_user_push_token_device UNIQUE (user_id, device_id),
    CONSTRAINT ck_push_platform CHECK (platform IN (
        'ANDROID_FCM','ANDROID_HMS','ANDROID_MIPUSH','IOS_APNS','WEB_PUSH'
    )),
    CONSTRAINT ck_push_status CHECK (status IN ('ACTIVE','REVOKED')),
    CONSTRAINT ck_push_locale CHECK (locale IN ('zh-CN','en-US'))
);
COMMENT ON TABLE  user_push_token IS '推送令牌表 — 按 (user_id, device_id) 唯一；按平台分发推送（DBD §2.6.7）';
COMMENT ON COLUMN user_push_token.push_token_id  IS '令牌表主键，自增';
COMMENT ON COLUMN user_push_token.user_id        IS '所属用户 ID（FK逻辑: ref sys_user.id）';
COMMENT ON COLUMN user_push_token.platform       IS '推送平台枚举：ANDROID_FCM / ANDROID_HMS / ANDROID_MIPUSH / IOS_APNS / WEB_PUSH';
COMMENT ON COLUMN user_push_token.device_id      IS '客户端稳定设备标识（如 Android ID）';
COMMENT ON COLUMN user_push_token.push_token     IS '推送服务商下发的设备 Token，PII: @Desensitize(TOKEN)';
COMMENT ON COLUMN user_push_token.app_version    IS '应用版本号（如 2.1.0）';
COMMENT ON COLUMN user_push_token.os_version     IS '操作系统版本（如 Android 14）';
COMMENT ON COLUMN user_push_token.device_model   IS '设备型号（如 Xiaomi 14 Ultra）';
COMMENT ON COLUMN user_push_token.locale         IS '客户端语言：zh-CN / en-US（HC-I18n）';
COMMENT ON COLUMN user_push_token.status         IS '令牌状态：ACTIVE（有效）/ REVOKED（已注销）';
COMMENT ON COLUMN user_push_token.last_active_at IS '最后活跃时间（每次推送成功后更新）';
COMMENT ON COLUMN user_push_token.revoked_at     IS '注销时间（登出/卸载时填入）';
COMMENT ON COLUMN user_push_token.trace_id       IS '全链路追踪标识（HC-04）';
COMMENT ON COLUMN user_push_token.created_at     IS '令牌首次注册时间（UTC）';
COMMENT ON COLUMN user_push_token.updated_at     IS '令牌最后更新时间（UTC）';

CREATE INDEX IF NOT EXISTS idx_push_user_active     ON user_push_token(user_id, status) WHERE status = 'ACTIVE';
CREATE INDEX IF NOT EXISTS idx_push_platform_active ON user_push_token(platform, status, last_active_at DESC) WHERE status = 'ACTIVE';

-- ============================================================
-- 8. ws_ticket（WebSocket 一次性连接票据）
--    V1, LLD §8.x WebSocket 鉴权
-- ============================================================
CREATE TABLE IF NOT EXISTS ws_ticket (
    id         BIGSERIAL       PRIMARY KEY,
    ticket     VARCHAR(128)    NOT NULL,
    user_id    BIGINT          NOT NULL,  -- FK逻辑: ref sys_user(id)
    expire_at  TIMESTAMPTZ     NOT NULL,
    used_at    TIMESTAMPTZ,
    created_at TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);
COMMENT ON TABLE  ws_ticket IS 'WebSocket 一次性连接票据表（LLD §8.x）';
COMMENT ON COLUMN ws_ticket.id         IS '票据记录 ID，自增主键';
COMMENT ON COLUMN ws_ticket.ticket     IS '一次性票据字符串（UUID v4 签名），used_at 非空即已消费';
COMMENT ON COLUMN ws_ticket.user_id    IS '票据归属用户 ID（FK逻辑: ref sys_user.id）';
COMMENT ON COLUMN ws_ticket.expire_at  IS '票据过期时间（通常 30 秒内有效）';
COMMENT ON COLUMN ws_ticket.used_at    IS '消费时间，非空表示已使用（防重放）';
COMMENT ON COLUMN ws_ticket.created_at IS '票据签发时间（UTC）';

CREATE UNIQUE INDEX IF NOT EXISTS ux_ws_ticket ON ws_ticket(ticket);

-- =====================================================================
-- === PROFILE 域 ===
-- 表：patient_profile, guardian_relation, guardian_invitation,
--     guardian_transfer_request
-- 基线：DBD §2.3, LLD §5
-- ★ 主要对齐修订：
--   patient_profile:  avatar_url→photo_url; 拆分字段→medical_history jsonb
--                     / appearance_tags jsonb; fence_center geometry(PostGIS);
--                     removed id_card_hash/emergency_contact_phone;
--                     added created_by; lost_status_event_time 改为可空
--   guardian_transfer_request: from_user_id→initiator_user_id;
--                     to_user_id→target_user_id; cancel_reason→revoke_reason;
--                     added revoked_by; reason 改为 varchar(256)
--   guardian_invitation: responded_at → accepted_at + rejected_at;
--                     reason 改为 varchar(256); removed version
--   guardian_relation: removed joined_at / revoked_at / version
-- =====================================================================

-- ============================================================
-- 9. patient_profile（患者档案表）
--    DBD §2.3.1, SRS FR-PRO-001~010, SADD §4.6
--    ★ PostGIS: fence_center geometry(Point,4326) 替代 lat/lng 对
-- ============================================================
CREATE TABLE IF NOT EXISTS patient_profile (
    id                     BIGSERIAL        PRIMARY KEY,
    profile_no             VARCHAR(32)      NOT NULL,
    name                   VARCHAR(64)      NOT NULL,
    gender                 VARCHAR(16)      NOT NULL DEFAULT 'UNKNOWN',
    birthday               DATE             NOT NULL,
    short_code             CHAR(6)          NOT NULL,
    photo_url              VARCHAR(1024)    NOT NULL,
    medical_history        JSONB,
    appearance_tags        JSONB,
    long_text_profile      TEXT,
    fence_enabled          BOOLEAN          NOT NULL DEFAULT FALSE,
    fence_center           GEOMETRY(Point, 4326),
    fence_radius_m         INT,
    lost_status            VARCHAR(20)      NOT NULL DEFAULT 'NORMAL',
    lost_status_event_time TIMESTAMPTZ,
    profile_version        BIGINT           NOT NULL DEFAULT 0,
    created_by             BIGINT           NOT NULL,  -- FK逻辑: ref sys_user(id)
    deleted_at             TIMESTAMPTZ,
    trace_id               VARCHAR(64)      NOT NULL,
    created_at             TIMESTAMPTZ      NOT NULL DEFAULT NOW(),
    updated_at             TIMESTAMPTZ      NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_profile_gender      CHECK (gender IN ('MALE','FEMALE','UNKNOWN')),
    CONSTRAINT ck_profile_lost_status CHECK (lost_status IN ('NORMAL','MISSING_PENDING','MISSING')),
    CONSTRAINT ck_profile_fence       CHECK (
        (fence_enabled = FALSE)
        OR (fence_enabled = TRUE AND fence_center IS NOT NULL AND fence_radius_m IS NOT NULL)
    )
);
COMMENT ON TABLE  patient_profile IS '患者档案表 — PROFILE 域聚合根，档案 + 3 态走失状态机（DBD §2.3.1）';
COMMENT ON COLUMN patient_profile.id                     IS '患者档案主键，自增';
COMMENT ON COLUMN patient_profile.profile_no             IS '档案编号，全局唯一（格式：PF+YYYYMMDD+6位序列）';
COMMENT ON COLUMN patient_profile.name                   IS 'PII: @Desensitize(CHINESE_NAME)（HC-07），患者姓名';
COMMENT ON COLUMN patient_profile.gender                 IS '性别枚举：MALE / FEMALE / UNKNOWN';
COMMENT ON COLUMN patient_profile.birthday               IS '出生日期（用于年龄显示及寻人信息）';
COMMENT ON COLUMN patient_profile.short_code             IS '6 位短码，服务端发号，全局唯一，二维码与口头核验使用（FR-PRO-003/004）';
COMMENT ON COLUMN patient_profile.photo_url              IS 'PII: 路人端展示需水印（HC-07）；患者正面照 URL';
COMMENT ON COLUMN patient_profile.medical_history        IS '病史 JSONB（诊断、用药、过敏等非结构化信息，PII: @Encrypt）';
COMMENT ON COLUMN patient_profile.appearance_tags        IS '外貌特征 JSONB（身高、体重、衣着描述标签等，FR-PRO-002）';
COMMENT ON COLUMN patient_profile.long_text_profile      IS '患者自然语言描述，变更触发 profile.updated 事件并同步向量空间';
COMMENT ON COLUMN patient_profile.fence_enabled          IS '是否启用电子围栏（false 时 fence_center/radius 可为 NULL）';
COMMENT ON COLUMN patient_profile.fence_center           IS 'PostGIS WGS84 SRID=4326 围栏中心点，空间查询使用 ST_DWithin（DBD §2.3.1）';
COMMENT ON COLUMN patient_profile.fence_radius_m         IS '围栏半径（米），从配置键 profile.fence.default_radius_m 读取默认值（HC-05）';
COMMENT ON COLUMN patient_profile.lost_status            IS '走失状态机：NORMAL→MISSING_PENDING→MISSING，服务端权威（HC-02）';
COMMENT ON COLUMN patient_profile.lost_status_event_time IS '最后一次状态变更时间（防乱序锚点，SADD §4.6）；可空';
COMMENT ON COLUMN patient_profile.profile_version        IS '乐观锁字段，每次档案更新自增（HC-01）';
COMMENT ON COLUMN patient_profile.created_by             IS '档案创建人（FK逻辑: ref sys_user.id），主监护人 ID';
COMMENT ON COLUMN patient_profile.deleted_at             IS '逻辑删除时间（FR-PRO-009），非空即为已删除';
COMMENT ON COLUMN patient_profile.trace_id               IS '全链路追踪标识（HC-04）';
COMMENT ON COLUMN patient_profile.created_at             IS '记录创建时间（UTC）';
COMMENT ON COLUMN patient_profile.updated_at             IS '记录最后更新时间（UTC）';

CREATE UNIQUE INDEX IF NOT EXISTS ux_patient_profile_no    ON patient_profile(profile_no);
CREATE UNIQUE INDEX IF NOT EXISTS ux_patient_short_code    ON patient_profile(short_code);
CREATE INDEX        IF NOT EXISTS idx_patient_lost_status  ON patient_profile(lost_status) WHERE lost_status != 'NORMAL';
CREATE INDEX        IF NOT EXISTS idx_patient_created_by   ON patient_profile(created_by);
CREATE INDEX        IF NOT EXISTS idx_patient_deleted_at   ON patient_profile(deleted_at) WHERE deleted_at IS NULL;
CREATE INDEX        IF NOT EXISTS gist_patient_fence_center ON patient_profile USING GIST (fence_center);

-- ============================================================
-- 10. guardian_relation（监护关系表）
--     DBD §2.3.2, SRS FR-PRO-006
--     ★ 移除: joined_at / revoked_at / version（不在 DBD §2.3.2）
-- ============================================================
CREATE TABLE IF NOT EXISTS guardian_relation (
    id              BIGSERIAL       PRIMARY KEY,
    user_id         BIGINT          NOT NULL,  -- FK逻辑: ref sys_user(id)
    patient_id      BIGINT          NOT NULL,  -- FK逻辑: ref patient_profile(id)
    relation_role   VARCHAR(32)     NOT NULL,
    relation_status VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    trace_id        VARCHAR(64)     NOT NULL,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_guardian_role   CHECK (relation_role   IN ('PRIMARY_GUARDIAN','GUARDIAN')),
    CONSTRAINT ck_guardian_status CHECK (relation_status IN ('PENDING','ACTIVE','REVOKED'))
);
COMMENT ON TABLE  guardian_relation IS '监护关系表 — 长期存在的用户-患者绑定关系，激活后具备访问权（DBD §2.3.2）';
COMMENT ON COLUMN guardian_relation.id              IS '关系记录主键，自增';
COMMENT ON COLUMN guardian_relation.user_id         IS '监护人用户 ID（FK逻辑: ref sys_user.id）';
COMMENT ON COLUMN guardian_relation.patient_id      IS '被监护患者 ID（FK逻辑: ref patient_profile.id）';
COMMENT ON COLUMN guardian_relation.relation_role   IS '角色：PRIMARY_GUARDIAN（主监护人）/ GUARDIAN（普通监护人）';
COMMENT ON COLUMN guardian_relation.relation_status IS '关系状态：PENDING（待激活）/ ACTIVE（激活）/ REVOKED（撤销）';
COMMENT ON COLUMN guardian_relation.trace_id        IS '全链路追踪标识（HC-04）';
COMMENT ON COLUMN guardian_relation.created_at      IS '记录创建时间（UTC）';
COMMENT ON COLUMN guardian_relation.updated_at      IS '记录最后更新时间（UTC）';

CREATE UNIQUE INDEX IF NOT EXISTS ux_guardian_user_patient ON guardian_relation(user_id, patient_id);
CREATE INDEX        IF NOT EXISTS idx_guardian_patient_active ON guardian_relation(patient_id, relation_status);

-- ============================================================
-- 11. guardian_invitation（监护邀请表）
--     DBD §2.3.4, SRS FR-PRO-006
--     ★ responded_at → accepted_at + rejected_at（DBD §2.3.4）
--     ★ reason varchar(256)（非 500）；moved version（DBD 无此字段）
-- ============================================================
CREATE TABLE IF NOT EXISTS guardian_invitation (
    id               BIGSERIAL       PRIMARY KEY,
    invite_id        VARCHAR(64)     NOT NULL,
    patient_id       BIGINT          NOT NULL,  -- FK逻辑: ref patient_profile(id)
    inviter_user_id  BIGINT          NOT NULL,  -- FK逻辑: ref sys_user(id)
    invitee_user_id  BIGINT          NOT NULL,  -- FK逻辑: ref sys_user(id)
    relation_role    VARCHAR(32)     NOT NULL DEFAULT 'GUARDIAN',
    status           VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    reason           VARCHAR(256),
    reject_reason    VARCHAR(256),
    expire_at        TIMESTAMPTZ     NOT NULL,
    accepted_at      TIMESTAMPTZ,
    rejected_at      TIMESTAMPTZ,
    revoked_at       TIMESTAMPTZ,
    trace_id         VARCHAR(64)     NOT NULL,
    created_at       TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_invitation_role   CHECK (relation_role IN ('GUARDIAN')),
    CONSTRAINT ck_invitation_status CHECK (status IN ('PENDING','ACCEPTED','REJECTED','EXPIRED','REVOKED'))
);
COMMENT ON TABLE  guardian_invitation IS '监护邀请表 — 主监护人邀请成员加入，短生命周期（DBD §2.3.4）';
COMMENT ON COLUMN guardian_invitation.id               IS '邀请记录主键，自增';
COMMENT ON COLUMN guardian_invitation.invite_id        IS '邀请号，全局唯一（UUID v4）';
COMMENT ON COLUMN guardian_invitation.patient_id       IS '被监护患者 ID（FK逻辑: ref patient_profile.id）';
COMMENT ON COLUMN guardian_invitation.inviter_user_id  IS '邀请人（主监护人）用户 ID（FK逻辑: ref sys_user.id）';
COMMENT ON COLUMN guardian_invitation.invitee_user_id  IS '被邀请人用户 ID（FK逻辑: ref sys_user.id）';
COMMENT ON COLUMN guardian_invitation.relation_role    IS '邀请加入的角色，当前固定为 GUARDIAN';
COMMENT ON COLUMN guardian_invitation.status           IS '邀请状态：PENDING / ACCEPTED / REJECTED / EXPIRED / REVOKED';
COMMENT ON COLUMN guardian_invitation.reason           IS '邀请附言（最大 256 字符）';
COMMENT ON COLUMN guardian_invitation.reject_reason    IS '拒绝原因';
COMMENT ON COLUMN guardian_invitation.expire_at        IS '邀请过期时间（从 sys_config: invitation.ttl_hours 配置读取，HC-05）';
COMMENT ON COLUMN guardian_invitation.accepted_at      IS '接受时间（ACCEPTED 状态时填入）';
COMMENT ON COLUMN guardian_invitation.rejected_at      IS '拒绝时间（REJECTED 状态时填入）';
COMMENT ON COLUMN guardian_invitation.revoked_at       IS '撤销时间（REVOKED 状态时填入）';
COMMENT ON COLUMN guardian_invitation.trace_id         IS '全链路追踪标识（HC-04）';
COMMENT ON COLUMN guardian_invitation.created_at       IS '邀请创建时间（UTC）';
COMMENT ON COLUMN guardian_invitation.updated_at       IS '记录最后更新时间（UTC）';

CREATE UNIQUE INDEX IF NOT EXISTS ux_invitation_id           ON guardian_invitation(invite_id);
CREATE INDEX        IF NOT EXISTS idx_invitation_invitee      ON guardian_invitation(invitee_user_id, status);
CREATE INDEX        IF NOT EXISTS idx_invitation_patient      ON guardian_invitation(patient_id, status);

-- ============================================================
-- 12. guardian_transfer_request（监护权转移请求表）
--     DBD §2.3.3, SRS FR-PRO-007, LLD §5.2.6
--     ★ from_user_id → initiator_user_id
--     ★ to_user_id   → target_user_id
--     ★ cancel_reason → revoke_reason；added revoked_by
--     ★ reason varchar(256)（非 500）；moved version（DBD 无此字段）
-- ============================================================
CREATE TABLE IF NOT EXISTS guardian_transfer_request (
    id                  BIGSERIAL       PRIMARY KEY,
    request_id          VARCHAR(64)     NOT NULL,
    patient_id          BIGINT          NOT NULL,  -- FK逻辑: ref patient_profile(id)
    initiator_user_id   BIGINT          NOT NULL,  -- FK逻辑: ref sys_user(id)
    target_user_id      BIGINT          NOT NULL,  -- FK逻辑: ref sys_user(id)
    status              VARCHAR(32)     NOT NULL DEFAULT 'PENDING_CONFIRM',
    reason              VARCHAR(256),
    expire_at           TIMESTAMPTZ     NOT NULL,
    confirmed_at        TIMESTAMPTZ,
    rejected_at         TIMESTAMPTZ,
    reject_reason       VARCHAR(256),
    revoked_by          BIGINT,                    -- FK逻辑: ref sys_user(id)
    revoked_at          TIMESTAMPTZ,
    revoke_reason       VARCHAR(256),
    trace_id            VARCHAR(64)     NOT NULL,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_transfer_status CHECK (status IN ('PENDING_CONFIRM','COMPLETED','REJECTED','REVOKED','EXPIRED'))
);
COMMENT ON TABLE  guardian_transfer_request IS '监护权转移请求表 — 主监护权临时移交，独立生命周期（DBD §2.3.3）';
COMMENT ON COLUMN guardian_transfer_request.id                  IS '转移请求记录主键，自增';
COMMENT ON COLUMN guardian_transfer_request.request_id          IS '转移请求号，全局唯一（UUID v4）';
COMMENT ON COLUMN guardian_transfer_request.patient_id          IS '关联患者 ID（FK逻辑: ref patient_profile.id）';
COMMENT ON COLUMN guardian_transfer_request.initiator_user_id   IS '发起人（当前主监护人）用户 ID（FK逻辑: ref sys_user.id）';
COMMENT ON COLUMN guardian_transfer_request.target_user_id      IS '目标用户 ID（接收方，FK逻辑: ref sys_user.id）';
COMMENT ON COLUMN guardian_transfer_request.status              IS '状态机：PENDING_CONFIRM / COMPLETED / REJECTED / REVOKED / EXPIRED';
COMMENT ON COLUMN guardian_transfer_request.reason              IS '转移原因（最大 256 字符）';
COMMENT ON COLUMN guardian_transfer_request.expire_at           IS '请求过期时间（从 sys_config 配置读取，HC-05）';
COMMENT ON COLUMN guardian_transfer_request.confirmed_at        IS '确认完成时间（COMPLETED 时填入）';
COMMENT ON COLUMN guardian_transfer_request.rejected_at         IS '拒绝时间（REJECTED 时填入）';
COMMENT ON COLUMN guardian_transfer_request.reject_reason       IS '拒绝原因';
COMMENT ON COLUMN guardian_transfer_request.revoked_by          IS '撤销操作人（FK逻辑: ref sys_user.id）';
COMMENT ON COLUMN guardian_transfer_request.revoked_at          IS '撤销时间（REVOKED 时填入）';
COMMENT ON COLUMN guardian_transfer_request.revoke_reason       IS '撤销原因';
COMMENT ON COLUMN guardian_transfer_request.trace_id            IS '全链路追踪标识（HC-04）';
COMMENT ON COLUMN guardian_transfer_request.created_at          IS '请求创建时间（UTC）';
COMMENT ON COLUMN guardian_transfer_request.updated_at          IS '记录最后更新时间（UTC）';

CREATE UNIQUE INDEX IF NOT EXISTS ux_transfer_request_id ON guardian_transfer_request(request_id);
CREATE INDEX        IF NOT EXISTS idx_transfer_patient    ON guardian_transfer_request(patient_id, status);
CREATE INDEX        IF NOT EXISTS idx_transfer_initiator  ON guardian_transfer_request(initiator_user_id, status);
CREATE INDEX        IF NOT EXISTS idx_transfer_target     ON guardian_transfer_request(target_user_id, status);

-- =====================================================================
-- === TASK 域 ===
-- 表：rescue_task
-- 基线：DBD §2.1, LLD §4.2
-- ★ 对齐修订：
--   rescue_task: 移除 found_location_lat/lng / sustained_at；
--                source 枚举修正为 APP/ADMIN_PORTAL/AUTO_UPGRADE；
--                移除多余 version（保留 event_version 作乐观锁）
-- =====================================================================

-- ============================================================
-- 13. rescue_task（寻回任务表）
--     DBD §2.1.1, SRS FR-TASK-001~012, SADD §4.1
-- ============================================================
CREATE TABLE IF NOT EXISTS rescue_task (
    id                   BIGSERIAL       PRIMARY KEY,
    task_no              VARCHAR(32)     NOT NULL,
    patient_id           BIGINT          NOT NULL,  -- FK逻辑: ref patient_profile(id)
    status               VARCHAR(32)     NOT NULL DEFAULT 'CREATED',
    source               VARCHAR(32)     NOT NULL DEFAULT 'APP',
    remark               TEXT,
    daily_appearance     TEXT,
    daily_photo_url      VARCHAR(1024),
    ai_analysis_summary  TEXT,
    poster_url           VARCHAR(1024),
    close_type           VARCHAR(20),
    close_reason         VARCHAR(256),
    event_version        BIGINT          NOT NULL DEFAULT 0,
    created_by           BIGINT          NOT NULL,  -- FK逻辑: ref sys_user(id)
    closed_by            BIGINT,                    -- FK逻辑: ref sys_user(id)
    closed_at            TIMESTAMPTZ,
    trace_id             VARCHAR(64)     NOT NULL,
    created_at           TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_task_status     CHECK (status     IN ('CREATED','ACTIVE','SUSTAINED','CLOSED_FOUND','CLOSED_FALSE_ALARM')),
    CONSTRAINT ck_task_source     CHECK (source     IN ('APP','ADMIN_PORTAL','AUTO_UPGRADE')),
    CONSTRAINT ck_task_close_type CHECK (close_type IS NULL OR close_type IN ('FOUND','FALSE_ALARM'))
);
COMMENT ON TABLE  rescue_task IS '寻回任务表 — TASK 域聚合根，任务状态机唯一权威实体（DBD §2.1.1）';
COMMENT ON COLUMN rescue_task.id                  IS '任务主键，自增';
COMMENT ON COLUMN rescue_task.task_no             IS '任务编号，全局唯一（格式：TK+YYYYMMDD+6位序列）';
COMMENT ON COLUMN rescue_task.patient_id          IS '关联患者 ID（FK逻辑: ref patient_profile.id）';
COMMENT ON COLUMN rescue_task.status              IS '任务状态机（服务端权威，HC-02）：CREATED→ACTIVE→SUSTAINED→CLOSED_*';
COMMENT ON COLUMN rescue_task.source              IS '任务来源：APP（家属）/ ADMIN_PORTAL（管理后台）/ AUTO_UPGRADE（走失状态自动升级）';
COMMENT ON COLUMN rescue_task.remark              IS '任务创建备注（家属填写）';
COMMENT ON COLUMN rescue_task.daily_appearance    IS '当日着装描述（FR-TASK-003），task.created 事件 payload 冗余字段';
COMMENT ON COLUMN rescue_task.daily_photo_url     IS '当日照片 URL（FR-TASK-003）';
COMMENT ON COLUMN rescue_task.ai_analysis_summary IS 'AI 分析摘要（AI Agent 写入，FR-AI-005）';
COMMENT ON COLUMN rescue_task.poster_url          IS '寻人启事 PDF/图片 URL（FR-TASK-012）';
COMMENT ON COLUMN rescue_task.close_type          IS '关闭类型：FOUND（找到）/ FALSE_ALARM（误报）';
COMMENT ON COLUMN rescue_task.close_reason        IS '关闭原因说明';
COMMENT ON COLUMN rescue_task.event_version       IS '乐观锁字段，每次状态变更自增（HC-01）；唯一版本控制字段';
COMMENT ON COLUMN rescue_task.created_by          IS '任务发起人（FK逻辑: ref sys_user.id）';
COMMENT ON COLUMN rescue_task.closed_by           IS '关闭操作人（FK逻辑: ref sys_user.id）';
COMMENT ON COLUMN rescue_task.closed_at           IS '任务关闭时间';
COMMENT ON COLUMN rescue_task.trace_id            IS '全链路追踪标识（HC-04）';
COMMENT ON COLUMN rescue_task.created_at          IS '记录创建时间（UTC）';
COMMENT ON COLUMN rescue_task.updated_at          IS '记录最后更新时间（UTC）';

CREATE UNIQUE INDEX IF NOT EXISTS ux_rescue_task_no        ON rescue_task(task_no);
CREATE INDEX        IF NOT EXISTS idx_task_patient_status  ON rescue_task(patient_id, status);
CREATE INDEX        IF NOT EXISTS idx_task_created_by      ON rescue_task(created_by);
CREATE INDEX        IF NOT EXISTS idx_task_status_updated  ON rescue_task(status, updated_at DESC) WHERE status IN ('ACTIVE','SUSTAINED');

-- =====================================================================
-- === CLUE 域 ===
-- 表：clue_record, patient_trajectory
-- 基线：DBD §2.2, LLD §4.1
-- ★ 主要对齐修订：
--   clue_record: latitude/longitude → location geometry(Point,4326) PostGIS；
--                photo_urls jsonb → photo_url varchar(1024)；
--                移除 reporter_user_id / reporter_type / drift_flag /
--                client_ip / status varchar；
--                is_valid boolean + review_status + override boolean 结构对齐；
--                device_fingerprint NOT NULL；tag_code NOT NULL；
--                added assignee_user_id / assigned_at / rejected_by
--   patient_trajectory: geometry_data jsonb → geometry PostGIS；
--                移除 clue_id / latitude / longitude / source_type / version；
--                task_id NOT NULL；geometry_type 去除 POINT 枚举值；
--                added ck_trajectory_data_consistency 约束
-- =====================================================================

-- ============================================================
-- 14. clue_record（线索记录表）
--     DBD §2.2.1, SRS FR-CLUE-001~010
--     ★ PostGIS: location geometry(Point,4326) 替代 latitude/longitude
-- ============================================================
CREATE TABLE IF NOT EXISTS clue_record (
    id                  BIGSERIAL       PRIMARY KEY,
    clue_no             VARCHAR(32)     NOT NULL,
    patient_id          BIGINT          NOT NULL,  -- FK逻辑: ref patient_profile(id)
    task_id             BIGINT,                    -- FK逻辑: ref rescue_task(id)；首条线索可能先于任务
    tag_code            VARCHAR(100)    NOT NULL,  -- FK逻辑: ref tag_asset(tag_code)
    source_type         VARCHAR(20)     NOT NULL,
    location            GEOMETRY(Point, 4326),
    coord_system        VARCHAR(10)     NOT NULL DEFAULT 'WGS84',
    description         TEXT,
    photo_url           VARCHAR(1024),
    tag_only            BOOLEAN         NOT NULL DEFAULT FALSE,
    risk_score          NUMERIC(5,4),
    suspect_flag        BOOLEAN         NOT NULL DEFAULT FALSE,
    suspect_reason      VARCHAR(256),
    is_valid            BOOLEAN,
    review_status       VARCHAR(20),
    override            BOOLEAN         NOT NULL DEFAULT FALSE,
    override_by         BIGINT,                    -- FK逻辑: ref sys_user(id)
    override_reason     VARCHAR(256),
    reject_reason       VARCHAR(256),
    rejected_by         BIGINT,                    -- FK逻辑: ref sys_user(id)
    assignee_user_id    BIGINT,                    -- FK逻辑: ref sys_user(id)
    assigned_at         TIMESTAMPTZ,
    reviewed_at         TIMESTAMPTZ,
    entry_token_jti     VARCHAR(64),
    device_fingerprint  VARCHAR(128)    NOT NULL,
    trace_id            VARCHAR(64)     NOT NULL,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_clue_source_type    CHECK (source_type    IN ('SCAN','MANUAL','POSTER_SCAN')),
    CONSTRAINT ck_clue_review_status  CHECK (review_status  IS NULL OR review_status IN ('PENDING','OVERRIDDEN','REJECTED'))
);
COMMENT ON TABLE  clue_record IS '线索记录表 — CLUE 域聚合根，路人上报原始数据与研判结果（DBD §2.2.1）';
COMMENT ON COLUMN clue_record.id                IS '线索记录主键，自增';
COMMENT ON COLUMN clue_record.clue_no           IS '线索编号，全局唯一';
COMMENT ON COLUMN clue_record.patient_id        IS '关联患者 ID（FK逻辑: ref patient_profile.id）';
COMMENT ON COLUMN clue_record.task_id           IS '关联任务 ID（FK逻辑: ref rescue_task.id）；首条线索创建时任务可能未建，允许为 NULL';
COMMENT ON COLUMN clue_record.tag_code          IS '报告线索时扫描的标签 code（FK逻辑: ref tag_asset.tag_code），NOT NULL';
COMMENT ON COLUMN clue_record.source_type       IS '线索来源：SCAN（扫码）/ MANUAL（手动）/ POSTER_SCAN（海报扫码）';
COMMENT ON COLUMN clue_record.location          IS 'PostGIS 地理坐标（WGS84 SRID=4326），空间查询使用 ST_DWithin / ST_Distance';
COMMENT ON COLUMN clue_record.coord_system      IS '坐标系标识，默认 WGS84（与 location 字段对应）';
COMMENT ON COLUMN clue_record.description       IS '路人文字描述';
COMMENT ON COLUMN clue_record.photo_url         IS '上报照片 URL（单张，OSSToken 鉴权访问）';
COMMENT ON COLUMN clue_record.tag_only          IS '是否仅有标签扫描而无人工描述';
COMMENT ON COLUMN clue_record.risk_score        IS 'AI 风险评分 [0,1]，高值触发人工复核';
COMMENT ON COLUMN clue_record.suspect_flag      IS '是否标记为可疑线索（AI 判定）';
COMMENT ON COLUMN clue_record.suspect_reason    IS '可疑原因描述';
COMMENT ON COLUMN clue_record.is_valid          IS '线索是否有效（管理员研判结果，NULL=待判定）';
COMMENT ON COLUMN clue_record.review_status     IS '研判状态：PENDING / OVERRIDDEN（无效覆盖）/ REJECTED（人工驳回）；NULL 未审';
COMMENT ON COLUMN clue_record.override          IS '是否被系统覆盖标记为无效（false=正常）';
COMMENT ON COLUMN clue_record.override_by       IS '执行覆盖操作的用户 ID（FK逻辑: ref sys_user.id）';
COMMENT ON COLUMN clue_record.override_reason   IS '覆盖原因';
COMMENT ON COLUMN clue_record.reject_reason     IS '驳回原因';
COMMENT ON COLUMN clue_record.rejected_by       IS '执行驳回的用户 ID（FK逻辑: ref sys_user.id）';
COMMENT ON COLUMN clue_record.assignee_user_id  IS '分配研判的管理员 ID（FK逻辑: ref sys_user.id）';
COMMENT ON COLUMN clue_record.assigned_at       IS '分配研判时间';
COMMENT ON COLUMN clue_record.reviewed_at       IS '研判完成时间';
COMMENT ON COLUMN clue_record.entry_token_jti   IS '匿名上报入口令牌 JTI（与 X-Anonymous-Token 对应，HC-06）';
COMMENT ON COLUMN clue_record.device_fingerprint IS '匿名风险隔离必填字段（HC-06），用于跨会话关联匿名设备';
COMMENT ON COLUMN clue_record.trace_id          IS '全链路追踪标识（HC-04）';
COMMENT ON COLUMN clue_record.created_at        IS '记录创建时间（UTC）';
COMMENT ON COLUMN clue_record.updated_at        IS '记录最后更新时间（UTC）';

CREATE UNIQUE INDEX IF NOT EXISTS ux_clue_no               ON clue_record(clue_no);
CREATE INDEX        IF NOT EXISTS idx_clue_patient_task    ON clue_record(patient_id, task_id, created_at DESC);
CREATE INDEX        IF NOT EXISTS idx_clue_review_status   ON clue_record(review_status) WHERE review_status IS NOT NULL;
CREATE INDEX        IF NOT EXISTS idx_clue_assignee        ON clue_record(assignee_user_id) WHERE assignee_user_id IS NOT NULL;
CREATE INDEX        IF NOT EXISTS idx_clue_device_fp       ON clue_record(device_fingerprint);
CREATE INDEX        IF NOT EXISTS gist_clue_location       ON clue_record USING GIST (location);

-- ============================================================
-- 15. patient_trajectory（患者轨迹表）
--     DBD §2.2.2, SRS FR-CLUE-010, LLD §4.1.2
--     ★ PostGIS: geometry_data geometry 替代 jsonb
--     ★ task_id NOT NULL（DBD §2.2.2）
--     ★ 移除: clue_id / latitude / longitude / source_type / version
--     ★ geometry_type 枚举去除 POINT（DBD: LINESTRING/SPARSE_POINT/EMPTY_WINDOW）
--     ★ 新增 ck_trajectory_data_consistency 约束
-- ============================================================
CREATE TABLE IF NOT EXISTS patient_trajectory (
    id              BIGSERIAL       PRIMARY KEY,
    patient_id      BIGINT          NOT NULL,  -- FK逻辑: ref patient_profile(id)
    task_id         BIGINT          NOT NULL,  -- FK逻辑: ref rescue_task(id)
    window_start    TIMESTAMPTZ     NOT NULL,
    window_end      TIMESTAMPTZ     NOT NULL,
    point_count     INT             NOT NULL DEFAULT 0,
    geometry_type   VARCHAR(32)     NOT NULL,
    geometry_data   GEOMETRY,
    trace_id        VARCHAR(64)     NOT NULL,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_trajectory_geometry_type         CHECK (geometry_type IN ('LINESTRING','SPARSE_POINT','EMPTY_WINDOW')),
    CONSTRAINT ck_trajectory_data_consistency      CHECK (
        (geometry_type = 'EMPTY_WINDOW' AND geometry_data IS NULL)
        OR (geometry_type != 'EMPTY_WINDOW' AND geometry_data IS NOT NULL)
    )
);
COMMENT ON TABLE  patient_trajectory IS '患者轨迹表 — 离散坐标点按时间窗口聚合为连续轨迹（DBD §2.2.2）';
COMMENT ON COLUMN patient_trajectory.id             IS '轨迹记录主键，自增';
COMMENT ON COLUMN patient_trajectory.patient_id     IS '关联患者 ID（FK逻辑: ref patient_profile.id）';
COMMENT ON COLUMN patient_trajectory.task_id        IS '关联任务 ID（FK逻辑: ref rescue_task.id），NOT NULL（DBD §2.2.2）';
COMMENT ON COLUMN patient_trajectory.window_start   IS '时间窗口起始时间';
COMMENT ON COLUMN patient_trajectory.window_end     IS '时间窗口结束时间';
COMMENT ON COLUMN patient_trajectory.point_count    IS '窗口内有效坐标点数量（EMPTY_WINDOW 时为 0）';
COMMENT ON COLUMN patient_trajectory.geometry_type  IS '几何类型：LINESTRING（连线）/ SPARSE_POINT（稀疏点集）/ EMPTY_WINDOW（无数据）';
COMMENT ON COLUMN patient_trajectory.geometry_data  IS 'PostGIS 轨迹几何体，EMPTY_WINDOW 时必须为 NULL（ck_trajectory_data_consistency）';
COMMENT ON COLUMN patient_trajectory.trace_id       IS '全链路追踪标识（HC-04）';
COMMENT ON COLUMN patient_trajectory.created_at     IS '记录创建时间（UTC）';

CREATE INDEX IF NOT EXISTS idx_trajectory_patient_task ON patient_trajectory(patient_id, task_id, window_start);
CREATE INDEX IF NOT EXISTS gist_trajectory_geometry    ON patient_trajectory USING GIST (geometry_data);

-- =====================================================================
-- === MAT 域 ===
-- 表：tag_apply_record, tag_asset, tag_batch_job
-- 基线：DBD §2.4, LLD §6
-- ★ 主要对齐修订：
--   tag_apply_record: shipping_province/city/district → shipping_address varchar(512);
--                     shipping_receiver → shipping_contact;
--                     reviewer_user_id → audit_by; reviewed_at → audit_at;
--                     added audit_remark; shipped_at → ship_at;
--                     added ship_by / ship_remark; exception_desc → exception_reason;
--                     added exception_at / exception_resolved_action /
--                     exception_resolved_by / exception_resolved_at /
--                     exception_resolved_remark;
--                     moved tag_type / tag_codes / logistics_no / logistics_company / remark;
--                     status 仅 PENDING_AUDIT/PENDING_SHIP/SHIPPED/RECEIVED/EXCEPTION/VOIDED;
--                     added rejected boolean / cancelled boolean
--   tag_asset: 移除 tag_type / lost_at / lost_reason / allocated_at;
--              void_at → voided_at; added status_event_time / qr_content /
--              resource_token / bound_by / voided_by / suspected_lost_clue_id /
--              loss_confirmed_at / loss_confirmed_by
-- =====================================================================

-- ============================================================
-- 16. tag_apply_record（标签申领记录表）
--     DBD §2.4.2, SRS FR-MAT-001~005, LLD §6.2
--     ★ 大量字段对齐，见上方修订说明
-- ============================================================
CREATE TABLE IF NOT EXISTS tag_apply_record (
    id                          BIGSERIAL       PRIMARY KEY,
    order_no                    VARCHAR(32)     NOT NULL,
    applicant_user_id           BIGINT          NOT NULL,  -- FK逻辑: ref sys_user(id)
    patient_id                  BIGINT          NOT NULL,  -- FK逻辑: ref patient_profile(id)
    status                      VARCHAR(20)     NOT NULL DEFAULT 'PENDING_AUDIT',
    rejected                    BOOLEAN         NOT NULL DEFAULT FALSE,
    cancelled                   BOOLEAN         NOT NULL DEFAULT FALSE,
    quantity                    INT             NOT NULL,
    shipping_address            VARCHAR(512)    NOT NULL,
    shipping_contact            VARCHAR(64)     NOT NULL,
    shipping_phone              VARCHAR(32)     NOT NULL,
    cancel_reason               VARCHAR(256),
    audit_by                    BIGINT,                    -- FK逻辑: ref sys_user(id)
    audit_at                    TIMESTAMPTZ,
    audit_remark                VARCHAR(256),
    reject_reason               VARCHAR(256),
    ship_at                     TIMESTAMPTZ,
    ship_by                     BIGINT,                    -- FK逻辑: ref sys_user(id)
    ship_remark                 VARCHAR(256),
    received_at                 TIMESTAMPTZ,
    exception_reason            VARCHAR(256),
    exception_at                TIMESTAMPTZ,
    exception_resolved_action   VARCHAR(20),
    exception_resolved_by       BIGINT,                    -- FK逻辑: ref sys_user(id)
    exception_resolved_at       TIMESTAMPTZ,
    exception_resolved_remark   VARCHAR(256),
    version                     BIGINT          NOT NULL DEFAULT 0,
    trace_id                    VARCHAR(64)     NOT NULL,
    created_at                  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_order_status        CHECK (status IN (
        'PENDING_AUDIT','PENDING_SHIP','SHIPPED','RECEIVED','EXCEPTION','VOIDED'
    )),
    CONSTRAINT ck_exception_action    CHECK (
        exception_resolved_action IS NULL
        OR exception_resolved_action IN ('RESEND','VOID')
    )
);
COMMENT ON TABLE  tag_apply_record IS '标签申领记录表 — MAT 域标签发放工单（DBD §2.4.2）';
COMMENT ON COLUMN tag_apply_record.id                         IS '申领记录主键，自增';
COMMENT ON COLUMN tag_apply_record.order_no                   IS '工单编号，全局唯一';
COMMENT ON COLUMN tag_apply_record.applicant_user_id          IS '申请人用户 ID（FK逻辑: ref sys_user.id）';
COMMENT ON COLUMN tag_apply_record.patient_id                 IS '申领关联的患者 ID（FK逻辑: ref patient_profile.id）';
COMMENT ON COLUMN tag_apply_record.status                     IS '工单状态：PENDING_AUDIT/PENDING_SHIP/SHIPPED/RECEIVED/EXCEPTION/VOIDED';
COMMENT ON COLUMN tag_apply_record.rejected                   IS '是否已被审核驳回（逻辑标志，便于统计）';
COMMENT ON COLUMN tag_apply_record.cancelled                  IS '是否已被申请人取消（逻辑标志，便于统计）';
COMMENT ON COLUMN tag_apply_record.quantity                   IS '申领标签数量';
COMMENT ON COLUMN tag_apply_record.shipping_address           IS 'PII: @Desensitize(ADDRESS)；收货地址（省市区+详细地址，最大 512 字符）';
COMMENT ON COLUMN tag_apply_record.shipping_contact           IS 'PII: @Desensitize(CHINESE_NAME)；收货人姓名';
COMMENT ON COLUMN tag_apply_record.shipping_phone             IS 'PII: @Desensitize(PHONE)；收货联系电话';
COMMENT ON COLUMN tag_apply_record.cancel_reason              IS '取消原因（申请人填写）';
COMMENT ON COLUMN tag_apply_record.audit_by                   IS '审核人用户 ID（FK逻辑: ref sys_user.id）';
COMMENT ON COLUMN tag_apply_record.audit_at                   IS '审核时间';
COMMENT ON COLUMN tag_apply_record.audit_remark               IS '审核备注';
COMMENT ON COLUMN tag_apply_record.reject_reason              IS '驳回原因（audit_by 填写）';
COMMENT ON COLUMN tag_apply_record.ship_at                    IS '发货时间';
COMMENT ON COLUMN tag_apply_record.ship_by                    IS '发货操作人（FK逻辑: ref sys_user.id）';
COMMENT ON COLUMN tag_apply_record.ship_remark                IS '发货备注（快递单号等可存此处）';
COMMENT ON COLUMN tag_apply_record.received_at                IS '确认收货时间';
COMMENT ON COLUMN tag_apply_record.exception_reason           IS '异常原因描述';
COMMENT ON COLUMN tag_apply_record.exception_at               IS '异常发生时间';
COMMENT ON COLUMN tag_apply_record.exception_resolved_action  IS '异常处理动作：RESEND（补发）/ VOID（作废）';
COMMENT ON COLUMN tag_apply_record.exception_resolved_by      IS '异常处理操作人（FK逻辑: ref sys_user.id）';
COMMENT ON COLUMN tag_apply_record.exception_resolved_at      IS '异常处理完成时间';
COMMENT ON COLUMN tag_apply_record.exception_resolved_remark  IS '异常处理备注';
COMMENT ON COLUMN tag_apply_record.version                    IS '行级乐观锁（HC-01）';
COMMENT ON COLUMN tag_apply_record.trace_id                   IS '全链路追踪标识（HC-04）';
COMMENT ON COLUMN tag_apply_record.created_at                 IS '记录创建时间（UTC）';
COMMENT ON COLUMN tag_apply_record.updated_at                 IS '记录最后更新时间（UTC）';

CREATE UNIQUE INDEX IF NOT EXISTS ux_tag_order_no          ON tag_apply_record(order_no);
CREATE INDEX        IF NOT EXISTS idx_order_applicant       ON tag_apply_record(applicant_user_id, created_at DESC);
CREATE INDEX        IF NOT EXISTS idx_order_patient         ON tag_apply_record(patient_id);
CREATE INDEX        IF NOT EXISTS idx_order_status          ON tag_apply_record(status) WHERE status NOT IN ('RECEIVED','VOIDED');

-- ============================================================
-- 17. tag_asset（标签资产表）
--     DBD §2.4.1, SRS FR-MAT-002~005, LLD §6.1.1, §5.2.3 标签状态机
--     ★ 移除: tag_type / lost_at / lost_reason / allocated_at
--     ★ void_at → voided_at
--     ★ added: status_event_time / qr_content / resource_token / bound_by /
--              voided_by / suspected_lost_clue_id / loss_confirmed_at /
--              loss_confirmed_by
-- ============================================================
CREATE TABLE IF NOT EXISTS tag_asset (
    id                      BIGSERIAL       PRIMARY KEY,
    tag_code                VARCHAR(100)    NOT NULL,
    short_code              CHAR(6),
    patient_id              BIGINT,                    -- FK逻辑: ref patient_profile(id)；绑定后填充
    status                  VARCHAR(20)     NOT NULL DEFAULT 'UNBOUND',
    status_event_time       TIMESTAMPTZ,
    qr_content              VARCHAR(1024),
    resource_token          VARCHAR(256),
    batch_no                VARCHAR(64),
    order_id                BIGINT,                    -- FK逻辑: ref tag_apply_record(id)
    bound_at                TIMESTAMPTZ,
    bound_by                BIGINT,                    -- FK逻辑: ref sys_user(id)
    voided_at               TIMESTAMPTZ,
    voided_by               BIGINT,                    -- FK逻辑: ref sys_user(id)
    void_reason             VARCHAR(256),
    suspected_lost_at       TIMESTAMPTZ,
    suspected_lost_clue_id  BIGINT,                    -- FK逻辑: ref clue_record(id)
    loss_confirmed_at       TIMESTAMPTZ,
    loss_confirmed_by       BIGINT,                    -- FK逻辑: ref sys_user(id)
    version                 BIGINT          NOT NULL DEFAULT 0,
    trace_id                VARCHAR(64)     NOT NULL,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_tag_status CHECK (status IN (
        'UNBOUND','ALLOCATED','BOUND','SUSPECTED_LOST','LOST','VOIDED'
    ))
);
COMMENT ON TABLE  tag_asset IS '标签资产表 — MAT 域标签生命周期管理（DBD §2.4.1）';
COMMENT ON COLUMN tag_asset.id                     IS '标签资产主键，自增';
COMMENT ON COLUMN tag_asset.tag_code               IS '标签唯一码，全局唯一（如 UUID 或自定义编码）';
COMMENT ON COLUMN tag_asset.short_code             IS '绑定患者的 6 位短码（FK逻辑: ref patient_profile.short_code）；绑定后填充';
COMMENT ON COLUMN tag_asset.patient_id             IS '绑定患者 ID（FK逻辑: ref patient_profile.id）；绑定后填充，NULL=未绑定';
COMMENT ON COLUMN tag_asset.status                 IS '标签状态机（服务端权威 HC-02）：UNBOUND→ALLOCATED→BOUND→SUSPECTED_LOST→LOST/VOIDED';
COMMENT ON COLUMN tag_asset.status_event_time      IS '最后状态变更时间（防乱序锚点，SADD §4.6）';
COMMENT ON COLUMN tag_asset.qr_content             IS 'QR 码内容（路由到公开扫码页的 URL）';
COMMENT ON COLUMN tag_asset.resource_token         IS '资源路由凭据（SADD §3.5），用于公网访问鉴权';
COMMENT ON COLUMN tag_asset.batch_no               IS '批次号（标签批量生产时的生产批次）';
COMMENT ON COLUMN tag_asset.order_id               IS '关联申领工单 ID（FK逻辑: ref tag_apply_record.id）';
COMMENT ON COLUMN tag_asset.bound_at               IS '绑定时间（BOUND 状态时填入）';
COMMENT ON COLUMN tag_asset.bound_by               IS '绑定操作人（FK逻辑: ref sys_user.id）';
COMMENT ON COLUMN tag_asset.voided_at              IS '作废时间（VOIDED 状态时填入）';
COMMENT ON COLUMN tag_asset.voided_by              IS '作废操作人（FK逻辑: ref sys_user.id）';
COMMENT ON COLUMN tag_asset.void_reason            IS '作废原因';
COMMENT ON COLUMN tag_asset.suspected_lost_at      IS '进入 SUSPECTED_LOST 状态时间';
COMMENT ON COLUMN tag_asset.suspected_lost_clue_id IS '触发疑似丢失的线索 ID（FK逻辑: ref clue_record.id）';
COMMENT ON COLUMN tag_asset.loss_confirmed_at      IS '丢失确认时间（LOST 状态时填入）';
COMMENT ON COLUMN tag_asset.loss_confirmed_by      IS '丢失确认操作人（FK逻辑: ref sys_user.id）';
COMMENT ON COLUMN tag_asset.version                IS '行级乐观锁（HC-01）';
COMMENT ON COLUMN tag_asset.trace_id               IS '全链路追踪标识（HC-04）';
COMMENT ON COLUMN tag_asset.created_at             IS '记录创建时间（UTC）';
COMMENT ON COLUMN tag_asset.updated_at             IS '记录最后更新时间（UTC）';

CREATE UNIQUE INDEX IF NOT EXISTS ux_tag_code           ON tag_asset(tag_code);
CREATE INDEX        IF NOT EXISTS idx_tag_patient        ON tag_asset(patient_id) WHERE patient_id IS NOT NULL;
CREATE INDEX        IF NOT EXISTS idx_tag_status         ON tag_asset(status);
CREATE INDEX        IF NOT EXISTS idx_tag_batch_no       ON tag_asset(batch_no) WHERE batch_no IS NOT NULL;
CREATE INDEX        IF NOT EXISTS idx_tag_order_id       ON tag_asset(order_id) WHERE order_id IS NOT NULL;

-- ============================================================
-- 18. tag_batch_job（标签批量生产作业表）
--     V3, LLD §6.1（应用层扩展，DBD 未显式定义，保留）
-- ============================================================
CREATE TABLE IF NOT EXISTS tag_batch_job (
    id             BIGSERIAL       PRIMARY KEY,
    batch_no       VARCHAR(64)     NOT NULL,
    quantity       INT             NOT NULL,
    status         VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    generated_count INT            NOT NULL DEFAULT 0,
    started_at     TIMESTAMPTZ,
    finished_at    TIMESTAMPTZ,
    error_message  TEXT,
    created_by     BIGINT          NOT NULL,  -- FK逻辑: ref sys_user(id)
    trace_id       VARCHAR(64)     NOT NULL,
    created_at     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_batch_status CHECK (status IN ('PENDING','RUNNING','COMPLETED','FAILED'))
);
COMMENT ON TABLE  tag_batch_job IS '标签批量生产作业表 — 记录 Admin 触发的批量 tag_code 生成任务（LLD §6.1，应用层扩展）';
COMMENT ON COLUMN tag_batch_job.id              IS '作业主键，自增';
COMMENT ON COLUMN tag_batch_job.batch_no        IS '批次号，全局唯一';
COMMENT ON COLUMN tag_batch_job.quantity        IS '本批次生成数量';
COMMENT ON COLUMN tag_batch_job.status          IS '作业状态：PENDING / RUNNING / COMPLETED / FAILED';
COMMENT ON COLUMN tag_batch_job.generated_count IS '已成功生成数量（用于断点续传进度查询）';
COMMENT ON COLUMN tag_batch_job.started_at      IS '作业开始时间';
COMMENT ON COLUMN tag_batch_job.finished_at     IS '作业完成时间（COMPLETED 或 FAILED 时填入）';
COMMENT ON COLUMN tag_batch_job.error_message   IS '失败时的错误信息';
COMMENT ON COLUMN tag_batch_job.created_by      IS '发起作业的管理员（FK逻辑: ref sys_user.id）';
COMMENT ON COLUMN tag_batch_job.trace_id        IS '全链路追踪标识（HC-04）';
COMMENT ON COLUMN tag_batch_job.created_at      IS '记录创建时间（UTC）';
COMMENT ON COLUMN tag_batch_job.updated_at      IS '记录最后更新时间（UTC）';

CREATE UNIQUE INDEX IF NOT EXISTS ux_tag_batch_no ON tag_batch_job(batch_no);

-- =====================================================================
-- === AI 域 ===
-- 表：ai_session, ai_quota_ledger, patient_memory_note, vector_store,
--     ai_intent
-- 基线：DBD §2.5, LLD §7, SADD §3.6/HC-AI
-- ★ 主要对齐修订：
--   ai_session:  prompt_tokens → request_tokens；
--                completion_tokens → response_tokens；
--                total_tokens → token_used；
--                移除 feedback_rating / feedback_comment（DBD 仅有 feedback varchar(20)）
--   ai_quota_ledger: trace_id 移除 DEFAULT ''，严格 NOT NULL（DBD §1.3）
--   vector_store: chunk_index → chunk_no（DBD §2.5.3）；
--                 embedding vector(1536)，HNSW m=32 ef_construction=256
-- =====================================================================

-- ============================================================
-- 19. ai_session（AI 对话会话表）
--     DBD §2.5.1, SRS FR-AI-001~006, LLD §7.2
--     ★ 字段名对齐 DBD §2.5.1
-- ============================================================
CREATE TABLE IF NOT EXISTS ai_session (
    id                      BIGSERIAL       PRIMARY KEY,
    session_id              VARCHAR(64)     NOT NULL,
    user_id                 BIGINT          NOT NULL,  -- FK逻辑: ref sys_user(id)
    patient_id              BIGINT,                    -- FK逻辑: ref patient_profile(id)
    task_id                 BIGINT,                    -- FK逻辑: ref rescue_task(id)
    messages                JSONB           NOT NULL DEFAULT '[]',
    request_tokens          INT             NOT NULL DEFAULT 0,
    response_tokens         INT             NOT NULL DEFAULT 0,
    token_usage             JSONB,
    token_used              INT             NOT NULL DEFAULT 0,
    model_name              VARCHAR(128)    NOT NULL,
    prompt_template_version VARCHAR(32),
    status                  VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    archived_at             TIMESTAMPTZ,
    feedback                VARCHAR(20),
    feedback_at             TIMESTAMPTZ,
    version                 BIGINT          NOT NULL DEFAULT 0,
    trace_id                VARCHAR(64)     NOT NULL,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_session_status   CHECK (status   IN ('ACTIVE','ARCHIVED')),
    CONSTRAINT ck_session_feedback CHECK (feedback IS NULL OR feedback IN ('HELPFUL','NOT_HELPFUL','UNSAFE'))
);
COMMENT ON TABLE  ai_session IS 'AI 对话会话表 — 多轮对话上下文存储（DBD §2.5.1，FR-AI-001~006）';
COMMENT ON COLUMN ai_session.id                     IS '会话主键，自增';
COMMENT ON COLUMN ai_session.session_id             IS '会话 UUID，全局唯一';
COMMENT ON COLUMN ai_session.user_id                IS '会话发起用户 ID（FK逻辑: ref sys_user.id）';
COMMENT ON COLUMN ai_session.patient_id             IS '关联患者 ID（FK逻辑: ref patient_profile.id），可为 NULL（通用会话）';
COMMENT ON COLUMN ai_session.task_id                IS '关联任务 ID（FK逻辑: ref rescue_task.id），可为 NULL（非任务对话）';
COMMENT ON COLUMN ai_session.messages               IS '多轮对话消息列表 JSONB（[{role, content, ts}]）';
COMMENT ON COLUMN ai_session.request_tokens         IS '本会话累计请求 Token 数（prompt tokens）';
COMMENT ON COLUMN ai_session.response_tokens        IS '本会话累计响应 Token 数（completion tokens）';
COMMENT ON COLUMN ai_session.token_usage            IS 'Token 使用明细 JSONB（含各轮次 breakdown）';
COMMENT ON COLUMN ai_session.token_used             IS '本会话总计 Token 使用量（request + response）';
COMMENT ON COLUMN ai_session.model_name             IS '使用的 AI 模型名称（如 gpt-4o / claude-3-5-sonnet）';
COMMENT ON COLUMN ai_session.prompt_template_version IS 'Prompt 模板版本（用于溯源和 A/B 测试）';
COMMENT ON COLUMN ai_session.status                 IS '会话状态：ACTIVE / ARCHIVED';
COMMENT ON COLUMN ai_session.archived_at            IS '归档时间（ARCHIVED 时填入）';
COMMENT ON COLUMN ai_session.feedback               IS '用户反馈：HELPFUL / NOT_HELPFUL / UNSAFE';
COMMENT ON COLUMN ai_session.feedback_at            IS '反馈提交时间';
COMMENT ON COLUMN ai_session.version                IS '行级乐观锁（HC-01）';
COMMENT ON COLUMN ai_session.trace_id               IS '全链路追踪标识（HC-04）';
COMMENT ON COLUMN ai_session.created_at             IS '会话创建时间（UTC）';
COMMENT ON COLUMN ai_session.updated_at             IS '会话最后更新时间（UTC）';

CREATE UNIQUE INDEX IF NOT EXISTS ux_ai_session_id       ON ai_session(session_id);
CREATE INDEX        IF NOT EXISTS idx_ai_session_user     ON ai_session(user_id, created_at DESC);
CREATE INDEX        IF NOT EXISTS idx_ai_session_patient  ON ai_session(patient_id) WHERE patient_id IS NOT NULL;
CREATE INDEX        IF NOT EXISTS idx_ai_session_task     ON ai_session(task_id) WHERE task_id IS NOT NULL;

-- ============================================================
-- 20. ai_quota_ledger（AI 配额分类账表）
--     DBD §2.5.4, SRS FR-AI-011, LLD §7.5
-- ============================================================
CREATE TABLE IF NOT EXISTS ai_quota_ledger (
    id           BIGSERIAL       PRIMARY KEY,
    ledger_type  VARCHAR(32)     NOT NULL,
    owner_id     BIGINT          NOT NULL,  -- FK逻辑: ref sys_user(id)
    period       VARCHAR(20)     NOT NULL,
    quota_limit  INT             NOT NULL,
    used         INT             NOT NULL DEFAULT 0,
    reserved     INT             NOT NULL DEFAULT 0,
    status       VARCHAR(20)     NOT NULL DEFAULT 'NORMAL',
    version      BIGINT          NOT NULL DEFAULT 0,
    trace_id     VARCHAR(64)     NOT NULL,
    created_at   TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_ledger_type   CHECK (ledger_type IN ('USER_DAILY','USER_MONTHLY','SYSTEM_DAILY')),
    CONSTRAINT ck_ledger_status CHECK (status       IN ('NORMAL','EXHAUSTED','SUSPENDED'))
);
COMMENT ON TABLE  ai_quota_ledger IS 'AI 配额分类账表 — 记录各用户/系统的 AI 调用配额使用情况（DBD §2.5.4）';
COMMENT ON COLUMN ai_quota_ledger.id          IS '分类账主键，自增';
COMMENT ON COLUMN ai_quota_ledger.ledger_type IS '账本类型：USER_DAILY（用户日度）/ USER_MONTHLY（月度）/ SYSTEM_DAILY（系统日度）';
COMMENT ON COLUMN ai_quota_ledger.owner_id    IS '配额归属用户 ID（FK逻辑: ref sys_user.id）；SYSTEM_DAILY 时为系统用户 ID';
COMMENT ON COLUMN ai_quota_ledger.period      IS '账期，格式：YYYY-MM-DD（日度）或 YYYY-MM（月度）';
COMMENT ON COLUMN ai_quota_ledger.quota_limit IS '配额上限（Token 数或请求次数，含义由 ledger_type 决定）';
COMMENT ON COLUMN ai_quota_ledger.used        IS '已消耗量';
COMMENT ON COLUMN ai_quota_ledger.reserved    IS '已预留但未确认的量（乐观锁期间保留）';
COMMENT ON COLUMN ai_quota_ledger.status      IS '账本状态：NORMAL / EXHAUSTED（用尽）/ SUSPENDED（管理员暂停）';
COMMENT ON COLUMN ai_quota_ledger.version     IS '行级乐观锁（HC-01），CAS 扣减配额';
COMMENT ON COLUMN ai_quota_ledger.trace_id    IS '全链路追踪标识（HC-04），NOT NULL（DBD §1.3）';
COMMENT ON COLUMN ai_quota_ledger.created_at  IS '记录创建时间（UTC）';
COMMENT ON COLUMN ai_quota_ledger.updated_at  IS '记录最后更新时间（UTC）';

CREATE UNIQUE INDEX IF NOT EXISTS ux_ai_quota_owner_period ON ai_quota_ledger(ledger_type, owner_id, period);

-- ============================================================
-- 21. patient_memory_note（患者记忆笔记表）
--     DBD §2.5.2, SRS FR-AI-007/008, LLD §7.3
-- ============================================================
CREATE TABLE IF NOT EXISTS patient_memory_note (
    id               BIGSERIAL       PRIMARY KEY,
    note_id          VARCHAR(64)     NOT NULL,
    patient_id       BIGINT          NOT NULL,  -- FK逻辑: ref patient_profile(id)
    kind             VARCHAR(32)     NOT NULL,
    content          TEXT            NOT NULL,
    tags             JSONB,
    source_version   BIGINT,
    source_event_id  VARCHAR(64),
    created_by       BIGINT,                    -- FK逻辑: ref sys_user(id)
    trace_id         VARCHAR(64)     NOT NULL,
    created_at       TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_memory_kind CHECK (kind IN ('HABIT','PREFERENCE','HISTORY','ALERT','OTHER'))
);
COMMENT ON TABLE  patient_memory_note IS '患者记忆笔记表 — 结构化记忆条目，供 AI Agent 检索（DBD §2.5.2）';
COMMENT ON COLUMN patient_memory_note.id              IS '笔记主键，自增';
COMMENT ON COLUMN patient_memory_note.note_id         IS '笔记 UUID，全局唯一';
COMMENT ON COLUMN patient_memory_note.patient_id      IS '关联患者 ID（FK逻辑: ref patient_profile.id）';
COMMENT ON COLUMN patient_memory_note.kind            IS '笔记类型：HABIT / PREFERENCE / HISTORY / ALERT / OTHER';
COMMENT ON COLUMN patient_memory_note.content         IS '笔记正文（自然语言描述，写入向量空间）';
COMMENT ON COLUMN patient_memory_note.tags            IS '分类标签 JSONB（如 ["饮食","作息"]）';
COMMENT ON COLUMN patient_memory_note.source_version  IS '来源档案版本（profile_version 快照）';
COMMENT ON COLUMN patient_memory_note.source_event_id IS '生成该笔记的 Outbox 事件 ID（溯源）';
COMMENT ON COLUMN patient_memory_note.created_by      IS '创建人（FK逻辑: ref sys_user.id）；NULL 表示 AI 自动生成';
COMMENT ON COLUMN patient_memory_note.trace_id        IS '全链路追踪标识（HC-04）';
COMMENT ON COLUMN patient_memory_note.created_at      IS '笔记创建时间（UTC）';
COMMENT ON COLUMN patient_memory_note.updated_at      IS '记录最后更新时间（UTC）';

CREATE UNIQUE INDEX IF NOT EXISTS ux_memory_note_id    ON patient_memory_note(note_id);
CREATE INDEX        IF NOT EXISTS idx_memory_patient    ON patient_memory_note(patient_id, kind);

-- ============================================================
-- 22. vector_store（向量存储表）
--     DBD §2.5.3, SRS FR-AI-007/008/009, LLD §7.4
--     ★ chunk_index → chunk_no（DBD §2.5.3）
--     ★ embedding vector(1536)，HNSW m=32 ef_construction=256
-- ============================================================
CREATE TABLE IF NOT EXISTS vector_store (
    id           BIGSERIAL       PRIMARY KEY,
    patient_id   BIGINT          NOT NULL,  -- FK逻辑: ref patient_profile(id)
    source_type  VARCHAR(32)     NOT NULL,
    source_id    BIGINT          NOT NULL,
    chunk_no     INT             NOT NULL DEFAULT 0,
    content      TEXT            NOT NULL,
    embedding    VECTOR(1536)    NOT NULL,
    metadata     JSONB,
    trace_id     VARCHAR(64)     NOT NULL,
    created_at   TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_vector_source_type CHECK (source_type IN (
        'PATIENT_PROFILE','MEMORY_NOTE','TASK_SUMMARY','CLUE_DESC'
    ))
);
COMMENT ON TABLE  vector_store IS '向量存储表 — pgvector 1536 维 embedding，HNSW 索引，支持 RAG 召回（DBD §2.5.3）';
COMMENT ON COLUMN vector_store.id          IS '向量记录主键，自增';
COMMENT ON COLUMN vector_store.patient_id  IS '关联患者 ID（FK逻辑: ref patient_profile.id）';
COMMENT ON COLUMN vector_store.source_type IS '来源类型：PATIENT_PROFILE / MEMORY_NOTE / TASK_SUMMARY / CLUE_DESC';
COMMENT ON COLUMN vector_store.source_id   IS '来源实体 ID（如 patient_profile.id 或 patient_memory_note.id）';
COMMENT ON COLUMN vector_store.chunk_no    IS '分块序号（同一 source_id 按 chunk_no 排序，从 0 开始）';
COMMENT ON COLUMN vector_store.content     IS '分块原始文本（用于 RAG 返回上下文片段）';
COMMENT ON COLUMN vector_store.embedding   IS '1536 维向量（OpenAI text-embedding-3-small 或同规格模型）；HNSW 检索';
COMMENT ON COLUMN vector_store.metadata    IS '附加元数据 JSONB（如 created_at_source, profile_version 等）';
COMMENT ON COLUMN vector_store.trace_id    IS '全链路追踪标识（HC-04）';
COMMENT ON COLUMN vector_store.created_at  IS '向量写入时间（UTC）';
COMMENT ON COLUMN vector_store.updated_at  IS '向量最后更新时间（UTC）';

CREATE UNIQUE INDEX IF NOT EXISTS ux_vector_source_chunk ON vector_store(patient_id, source_type, source_id, chunk_no);
CREATE INDEX        IF NOT EXISTS idx_vector_patient      ON vector_store(patient_id);
-- HNSW 索引（pgvector 0.7+，余弦距离，m=32 ef_construction=256）
CREATE INDEX        IF NOT EXISTS hnsw_vector_embedding   ON vector_store
    USING HNSW (embedding vector_cosine_ops)
    WITH (m = 32, ef_construction = 256);

-- ============================================================
-- 23. ai_intent（AI 意图识别记录表）
--     V6, LLD §7.6（应用层扩展，DBD 未显式定义，保留）
-- ============================================================
CREATE TABLE IF NOT EXISTS ai_intent (
    id               BIGSERIAL       PRIMARY KEY,
    session_id       VARCHAR(64)     NOT NULL,  -- FK逻辑: ref ai_session(session_id)
    user_id          BIGINT          NOT NULL,  -- FK逻辑: ref sys_user(id)
    raw_input        TEXT            NOT NULL,
    intent_type      VARCHAR(64)     NOT NULL,
    confidence       NUMERIC(5,4),
    extracted_params JSONB,
    action_taken     VARCHAR(64),
    action_result    JSONB,
    model_name       VARCHAR(128),
    latency_ms       INT,
    trace_id         VARCHAR(64)     NOT NULL,
    created_at       TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);
COMMENT ON TABLE  ai_intent IS 'AI 意图识别记录表 — 记录每次意图识别结果及执行情况（LLD §7.6，应用层扩展）';
COMMENT ON COLUMN ai_intent.id               IS '意图记录主键，自增';
COMMENT ON COLUMN ai_intent.session_id       IS '所属会话 ID（FK逻辑: ref ai_session.session_id）';
COMMENT ON COLUMN ai_intent.user_id          IS '发起意图的用户 ID（FK逻辑: ref sys_user.id）';
COMMENT ON COLUMN ai_intent.raw_input        IS '用户原始输入文本';
COMMENT ON COLUMN ai_intent.intent_type      IS '识别出的意图类型（如 SEARCH_CLUE / CREATE_TASK / QUERY_STATUS）';
COMMENT ON COLUMN ai_intent.confidence       IS '意图置信度 [0,1]';
COMMENT ON COLUMN ai_intent.extracted_params IS '提取的结构化参数 JSONB（如 patient_id, time_range 等）';
COMMENT ON COLUMN ai_intent.action_taken     IS '实际执行的动作（如 API 调用名称）';
COMMENT ON COLUMN ai_intent.action_result    IS '动作执行结果摘要 JSONB';
COMMENT ON COLUMN ai_intent.model_name       IS '执行识别的 AI 模型名称';
COMMENT ON COLUMN ai_intent.latency_ms       IS '意图识别耗时（毫秒）';
COMMENT ON COLUMN ai_intent.trace_id         IS '全链路追踪标识（HC-04）';
COMMENT ON COLUMN ai_intent.created_at       IS '意图记录时间（UTC）';

CREATE INDEX IF NOT EXISTS idx_intent_session   ON ai_intent(session_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_intent_user       ON ai_intent(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_intent_type       ON ai_intent(intent_type, created_at DESC);

-- =====================================================================
-- === 种子数据（sys_config）
-- =====================================================================
INSERT INTO sys_config (config_key, config_value, scope, description, trace_id)
VALUES
-- 围栏默认参数
('profile.fence.default_radius_m',         '200',                  'public',    '电子围栏默认半径（米），家属可在 [50,5000] 范围内调整',           'seed-0001'),
-- 寻回任务自动升级阈值
('task.auto_upgrade.missing_pending_hours', '2',                    'ops',       '走失挂起状态超过此小时数自动升级为 ACTIVE 任务',                   'seed-0002'),
('task.auto_upgrade.missing_hours',         '24',                   'ops',       '走失状态超过此小时数自动升级为 MISSING',                           'seed-0003'),
-- 限流
('api.rate_limit.clue_submit.per_device',   '10',                   'security',  '单设备每分钟线索上报上限（HC-05）',                               'seed-0004'),
('api.rate_limit.login.per_ip',             '5',                    'security',  '单 IP 每分钟登录尝试上限',                                       'seed-0005'),
-- 令牌 TTL
('auth.invitation.ttl_hours',               '72',                   'ops',       '监护邀请有效期（小时）',                                         'seed-0006'),
('auth.transfer.ttl_hours',                 '48',                   'ops',       '监护权转移请求有效期（小时）',                                   'seed-0007'),
('auth.ws_ticket.ttl_seconds',              '30',                   'ops',       'WebSocket 票据有效期（秒）',                                     'seed-0008'),
-- AI 配置
('ai.embedding.model',                      'text-embedding-3-small', 'ai_policy','Embedding 模型名称',                                           'seed-0009'),
('ai.embedding.model_dimension',            '1536',                 'ai_policy', 'Embedding 向量维度（需与 vector_store.embedding VECTOR(n) 一致）','seed-0010'),
('ai.chat.model',                           'gpt-4o',               'ai_policy', '对话 AI 模型名称',                                              'seed-0011'),
('ai.chat.max_tokens_per_turn',             '2048',                 'ai_policy', '单轮对话最大 Token 数',                                         'seed-0012'),
('ai.quota.user_daily_tokens',              '50000',                'ai_policy', '用户每日 Token 配额上限',                                       'seed-0013'),
('ai.quota.user_monthly_tokens',            '500000',               'ai_policy', '用户每月 Token 配额上限',                                       'seed-0014'),
-- 坐标系
('map.coord_system.upload',                 'GCJ-02',               'ops',       '客户端上报坐标系（HC-Coord，GCJ-02 加密坐标）',                   'seed-0015'),
('map.coord_system.storage',                'WGS84',                'ops',       '服务端存储坐标系（WGS84，PostGIS 标准）',                         'seed-0016')
ON CONFLICT (config_key) DO UPDATE
    SET config_value = EXCLUDED.config_value,
        description  = EXCLUDED.description,
        updated_at   = NOW();

-- =====================================================================
-- === 完成
-- =====================================================================
-- 表清单（共 23 张）：
--   GOV域:     sys_user, sys_config, sys_log*, sys_outbox_log*,
--              consumed_event_log*, notification_inbox, user_push_token, ws_ticket
--   PROFILE域: patient_profile, guardian_relation, guardian_invitation,
--              guardian_transfer_request
--   TASK域:    rescue_task
--   CLUE域:    clue_record, patient_trajectory
--   MAT域:     tag_apply_record, tag_asset, tag_batch_job
--   AI域:      ai_session, ai_quota_ledger, patient_memory_note,
--              vector_store, ai_intent
--   (* 分区表，pg_partman 管理)
--
-- PostGIS 空间字段汇总：
--   patient_profile.fence_center    geometry(Point, 4326)  GIST 索引
--   clue_record.location            geometry(Point, 4326)  GIST 索引
--   patient_trajectory.geometry_data geometry              GIST 索引
--
-- 生成时间：2026-04-27
-- =====================================================================
