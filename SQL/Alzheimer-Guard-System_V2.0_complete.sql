-- =====================================================================
-- Alzheimer Guard System — 完整 DDL（V2.0 基线合并版）
-- 合并范围：V1__init_schema + V2__admin_governance + V3__material_exception
--           + V4__v2_1_push_token + V5__v2_1_client_version_config
--           + V6__sys_user_phone_unique + V7__pgvector_rag
-- 基线依据：SRS V2.0 / SADD V2.0 / LLD V2.0 / DBD V2.0 / API V2.0
--
-- ⚠ 变更说明（相对于 V7 Flyway 脚本）：
--   1) vector_store.embedding 维度由 vector(1024) 升至 vector(1536)
--      — 兼容 OpenAI text-embedding-3-small/large、Cohere embed-v3 等主流模型
--      — 配置键 ai.embedding.model_dimension 同步更新为 1536
--   2) HNSW 参数修正为 m=32, ef_construction=256（与 LLD §7.1.3 L2895 对齐）
--   3) ai_quota_ledger 补充 trace_id / status 字段（对齐 DBD §2.5.4）
--
-- ⚠ 已知基线偏差（保留 V1 实现，不改基线）：
--   · sys_log/sys_outbox_log/consumed_event_log 为普通表（DBD 设计为分区表）
--   · patient_profile.fence_center 为独立 lat/lng（DBD 设计为 PostGIS Point）
--   · guardian_transfer_request 字段名 from_user_id/to_user_id
--     （DBD 命名 initiator_user_id/target_user_id）
--   · vector_store.chunk_index（DBD 命名 chunk_no）
--
-- 目标数据库：PostgreSQL 16（需预装 PostGIS 3.4、pgvector 0.7+）
-- =====================================================================

-- ============================================================
-- 0. 扩展启用
-- ============================================================
CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS vector;   -- pgvector 0.7+

-- =====================================================================
-- === GOV 域 ===
-- 表：sys_user, sys_config, sys_log, sys_outbox_log,
--      consumed_event_log, notification_inbox, user_push_token, ws_ticket
-- 基线：DBD §2.6, LLD §8
-- =====================================================================

-- ============================================================
-- 1. sys_user（用户表）
--    DBD §2.6.1, SRS FR-GOV-001/002/004
--    V2 补充 deactivated_at
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_user (
    id                BIGSERIAL       PRIMARY KEY,
    username          VARCHAR(64)     NOT NULL,
    email             VARCHAR(128)    NOT NULL,
    email_verified    BOOLEAN         NOT NULL DEFAULT FALSE,
    password_hash     VARCHAR(128)    NOT NULL,                -- BCrypt (DBD §2.6.1)
    nickname          VARCHAR(64),
    avatar_url        VARCHAR(1024),
    phone             VARCHAR(32),                             -- PII: @Desensitize(PHONE)（HC-07）; 家属注册必填; ADMIN 可 NULL
    role              VARCHAR(32)     NOT NULL DEFAULT 'FAMILY',
    status            VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    last_login_at     TIMESTAMPTZ,
    last_login_ip     VARCHAR(64),
    deactivated_at    TIMESTAMPTZ,                             -- V2: 逻辑注销时间（DEACTIVATED 终态）
    version           BIGINT          NOT NULL DEFAULT 0,      -- 乐观锁（HC-01）
    trace_id          VARCHAR(64),                             -- HC-04
    created_at        TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_sys_user_role   CHECK (role   IN ('FAMILY','ADMIN','SUPER_ADMIN')),
    CONSTRAINT ck_sys_user_status CHECK (status IN ('ACTIVE','DISABLED','DEACTIVATED'))
);
COMMENT ON TABLE  sys_user IS '用户表 — GOV 域聚合根，含注册、登录、角色管理 (DBD §2.6.1)';
COMMENT ON COLUMN sys_user.id            IS '用户唯一 ID，自增主键';
COMMENT ON COLUMN sys_user.username      IS '登录用户名，全局唯一';
COMMENT ON COLUMN sys_user.email         IS 'PII: @Desensitize(EMAIL)（HC-07），全局唯一';
COMMENT ON COLUMN sys_user.password_hash IS 'BCrypt 哈希，不可逆（DBD §2.6.1）';
COMMENT ON COLUMN sys_user.phone         IS 'PII: @Desensitize(PHONE)（HC-07）; 家属注册必填; 后台种子 / ADMIN 可 NULL';
COMMENT ON COLUMN sys_user.role          IS '角色：FAMILY / ADMIN / SUPER_ADMIN';
COMMENT ON COLUMN sys_user.status        IS '账户状态：ACTIVE / DISABLED / DEACTIVATED';
COMMENT ON COLUMN sys_user.deactivated_at IS '逻辑注销时间，DEACTIVATED 终态时写入（V2, DBD §2.6.1）';
COMMENT ON COLUMN sys_user.version       IS '乐观锁版本号（HC-01）';
COMMENT ON COLUMN sys_user.trace_id      IS '全链路追踪标识（HC-04）';

-- sys_user 索引
CREATE UNIQUE INDEX IF NOT EXISTS ux_sys_user_username   ON sys_user(username);
CREATE UNIQUE INDEX IF NOT EXISTS ux_sys_user_email      ON sys_user(email);
-- V6: phone 部分唯一索引（仅对非 NULL 行去重，ADMIN 种子兼容）
-- 对应 DBD §2.6.1 备注 + 错误码 E_USR_4095
CREATE UNIQUE INDEX IF NOT EXISTS ux_sys_user_phone      ON sys_user(phone) WHERE phone IS NOT NULL;
CREATE INDEX        IF NOT EXISTS idx_sys_user_role       ON sys_user(role, status);
-- V2 索引
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
    updated_by      BIGINT          REFERENCES sys_user(id),
    updated_reason  VARCHAR(256),
    version         BIGINT          NOT NULL DEFAULT 0,        -- 乐观锁（HC-01）
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_config_scope CHECK (scope IN ('public','ops','security','ai_policy'))
);
COMMENT ON TABLE  sys_config IS '系统配置表 — 动态配置中心（HC-05），所有业务阈值从此表读取 (DBD §2.6.5)';
COMMENT ON COLUMN sys_config.config_key   IS '配置键（全局唯一主键）';
COMMENT ON COLUMN sys_config.scope        IS '作用域：public / ops / security / ai_policy';
COMMENT ON COLUMN sys_config.version      IS '乐观锁（HC-01）';

CREATE INDEX IF NOT EXISTS idx_sys_config_scope ON sys_config(scope);

-- ============================================================
-- 3. sys_log（审计日志表）
--    DBD §2.6.2（基线为分区表；此处保留非分区实现，减少运维复杂度）
--    SRS FR-GOV-006/007, FR-AI-011
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_log (
    id                 BIGSERIAL       PRIMARY KEY,
    module             VARCHAR(64)     NOT NULL,
    action             VARCHAR(64)     NOT NULL,
    action_id          VARCHAR(64),
    result_code        VARCHAR(64),
    executed_at        TIMESTAMPTZ,
    operator_user_id   BIGINT          REFERENCES sys_user(id),
    operator_username  VARCHAR(64),
    object_id          VARCHAR(64),
    result             VARCHAR(20)     NOT NULL,
    risk_level         VARCHAR(20),
    detail             JSONB,
    action_source      VARCHAR(20)     NOT NULL DEFAULT 'USER',
    agent_profile      VARCHAR(64),
    execution_mode     VARCHAR(20),
    confirm_level      VARCHAR(20),
    blocked_reason     VARCHAR(128),
    client_ip          VARCHAR(64),
    request_id         VARCHAR(64),
    trace_id           VARCHAR(64)     NOT NULL,               -- HC-04
    created_at         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_log_result       CHECK (result       IN ('SUCCESS','FAIL')),
    CONSTRAINT ck_log_risk_level   CHECK (risk_level   IS NULL OR risk_level IN ('LOW','MEDIUM','HIGH','CRITICAL')),
    CONSTRAINT ck_log_action_source CHECK (action_source IN ('USER','AI_AGENT','SYSTEM'))
);
COMMENT ON TABLE  sys_log IS '审计日志表 — 按月保留 180 天（FR-GOV-007）(DBD §2.6.2)';
COMMENT ON COLUMN sys_log.action_source IS 'AI_AGENT 时 action_id/result_code/executed_at 必须落库（DBD §2.6.2）';
COMMENT ON COLUMN sys_log.trace_id      IS '全链路追踪标识（HC-04）';

CREATE INDEX IF NOT EXISTS idx_sys_log_module_action ON sys_log(module, action, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_sys_log_operator      ON sys_log(operator_user_id, created_at DESC);
-- V3 补充索引（FR-GOV-007 审计导出范围查询）
CREATE INDEX IF NOT EXISTS idx_sys_log_created_at    ON sys_log(created_at);
CREATE INDEX IF NOT EXISTS idx_sys_log_trace_id      ON sys_log(trace_id);
CREATE INDEX IF NOT EXISTS idx_sys_log_action_source ON sys_log(action_source) WHERE action_source = 'AI_AGENT';

-- ============================================================
-- 4. sys_outbox_log（Outbox 事件表）
--    DBD §2.6.3（基线为分区表；此处保留非分区实现）
--    SADD HC-02, LLD §8.1.3
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_outbox_log (
    id                    BIGSERIAL       PRIMARY KEY,
    event_id              VARCHAR(64)     NOT NULL,
    topic                 VARCHAR(128)    NOT NULL,
    aggregate_id          VARCHAR(64)     NOT NULL,
    partition_key         VARCHAR(64)     NOT NULL,
    payload               JSONB           NOT NULL,
    request_id            VARCHAR(64),
    trace_id              VARCHAR(64)     NOT NULL,            -- HC-04
    phase                 VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    retry_count           INT             NOT NULL DEFAULT 0,
    next_retry_at         TIMESTAMPTZ,
    lease_owner           VARCHAR(64),
    lease_until           TIMESTAMPTZ,
    sent_at               TIMESTAMPTZ,
    last_error            VARCHAR(512),
    last_intervention_by  BIGINT          REFERENCES sys_user(id),
    last_intervention_at  TIMESTAMPTZ,
    replay_reason         VARCHAR(256),
    replay_token          VARCHAR(64),
    replayed_at           TIMESTAMPTZ,
    created_at            TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_outbox_phase CHECK (phase IN ('PENDING','DISPATCHING','SENT','RETRY','DEAD'))
);
COMMENT ON TABLE  sys_outbox_log IS 'Outbox 事件表 — Local Transaction + Outbox Pattern（HC-02）(DBD §2.6.3)';
COMMENT ON COLUMN sys_outbox_log.phase    IS '状态机：PENDING → DISPATCHING → SENT；失败路径 DISPATCHING → RETRY → DEAD';
COMMENT ON COLUMN sys_outbox_log.trace_id IS '全链路追踪标识（HC-04）';

CREATE UNIQUE INDEX IF NOT EXISTS uq_outbox_event_id       ON sys_outbox_log(event_id);
CREATE INDEX        IF NOT EXISTS idx_outbox_phase_retry   ON sys_outbox_log(phase, next_retry_at)
    WHERE phase IN ('PENDING','RETRY');
CREATE INDEX        IF NOT EXISTS idx_outbox_topic_partition ON sys_outbox_log(topic, partition_key);
CREATE INDEX        IF NOT EXISTS idx_outbox_lease         ON sys_outbox_log(lease_owner, lease_until)
    WHERE phase = 'DISPATCHING';
CREATE INDEX        IF NOT EXISTS idx_outbox_dead          ON sys_outbox_log(phase, updated_at DESC)
    WHERE phase = 'DEAD';

-- ============================================================
-- 5. consumed_event_log（消费幂等日志表）
--    DBD §2.6.4（基线为分区表；此处保留非分区实现）
-- ============================================================
CREATE TABLE IF NOT EXISTS consumed_event_log (
    id             BIGSERIAL       PRIMARY KEY,
    consumer_name  VARCHAR(64)     NOT NULL,
    topic          VARCHAR(128)    NOT NULL,
    event_id       VARCHAR(64)     NOT NULL,
    partition_no   INT,
    msg_offset     BIGINT,                                     -- DBD 命名为 "offset"（保留 V1 命名避免保留字冲突）
    trace_id       VARCHAR(64)     NOT NULL,                   -- HC-04
    processed_at   TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);
COMMENT ON TABLE  consumed_event_log IS '消费幂等日志表 — 保障事件消费者幂等性 (DBD §2.6.4)';
COMMENT ON COLUMN consumed_event_log.trace_id IS '全链路追踪标识（HC-04）';

CREATE UNIQUE INDEX IF NOT EXISTS uq_consumer_event ON consumed_event_log(consumer_name, topic, event_id);
CREATE INDEX        IF NOT EXISTS idx_consumed_proc  ON consumed_event_log(processed_at);

-- ============================================================
-- 6. notification_inbox（通知收件箱表）
--    DBD §2.6.6, SRS FR-GOV-010
-- ============================================================
CREATE TABLE IF NOT EXISTS notification_inbox (
    notification_id     BIGSERIAL       PRIMARY KEY,
    user_id             BIGINT          NOT NULL REFERENCES sys_user(id),
    type                VARCHAR(32)     NOT NULL,
    title               VARCHAR(128)    NOT NULL,
    content             TEXT            NOT NULL,
    level               VARCHAR(16)     NOT NULL DEFAULT 'INFO',
    channel             VARCHAR(32)     NOT NULL DEFAULT 'INBOX',
    related_task_id     BIGINT,                                -- FK逻辑: ref rescue_task(id)（无物理 FK，避免跨域依赖）
    related_patient_id  BIGINT,                                -- FK逻辑: ref patient_profile(id)
    related_object_id   VARCHAR(64),                           -- 通用关联对象 ID（DBD 命名 source_event_id，保留 V1 命名）
    read_status         VARCHAR(16)     NOT NULL DEFAULT 'UNREAD',
    read_at             TIMESTAMPTZ,
    source_event_id     VARCHAR(64),                           -- DBD §2.6.6 source_event_id
    trace_id            VARCHAR(64)     NOT NULL,              -- HC-04
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_noti_type        CHECK (type IN (
        'TASK_PROGRESS','FENCE_ALERT','TASK_CLOSED','MISSING_PENDING_ALERT',
        'TAG_SUSPECTED_LOST','TRANSFER_REQUEST','INVITATION','SYSTEM'
    )),
    CONSTRAINT ck_noti_level       CHECK (level       IN ('INFO','WARN','CRITICAL')),
    CONSTRAINT ck_noti_channel     CHECK (channel     IN ('WEBSOCKET','JPUSH','EMAIL','INBOX')),
    CONSTRAINT ck_noti_read_status CHECK (read_status IN ('UNREAD','READ'))
);
COMMENT ON TABLE  notification_inbox IS '通知收件箱表 — 多渠道通知落库，支持已读/未读查询 (DBD §2.6.6)';
COMMENT ON COLUMN notification_inbox.trace_id IS '全链路追踪标识（HC-04）';

CREATE INDEX IF NOT EXISTS idx_noti_user_unread ON notification_inbox(user_id, read_status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_noti_patient     ON notification_inbox(related_patient_id);
CREATE INDEX IF NOT EXISTS idx_noti_user_type   ON notification_inbox(user_id, type, created_at DESC);

-- ============================================================
-- 7. user_push_token（推送令牌表）
--    V4, DBD §2.6.7, API §3.8.5
-- ============================================================
CREATE TABLE IF NOT EXISTS user_push_token (
    push_token_id   BIGSERIAL       PRIMARY KEY,
    user_id         BIGINT          NOT NULL REFERENCES sys_user(id),
    platform        VARCHAR(24)     NOT NULL,
    device_id       VARCHAR(128)    NOT NULL,
    push_token      VARCHAR(512)    NOT NULL,                  -- PII: @Desensitize(TOKEN)
    app_version     VARCHAR(32)     NOT NULL,
    os_version      VARCHAR(64),
    device_model    VARCHAR(64),
    locale          VARCHAR(16)     NOT NULL DEFAULT 'zh-CN',
    status          VARCHAR(16)     NOT NULL DEFAULT 'ACTIVE',
    last_active_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    revoked_at      TIMESTAMPTZ,
    trace_id        VARCHAR(64)     NOT NULL,                  -- HC-04
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_user_push_token_device UNIQUE (user_id, device_id),
    CONSTRAINT ck_push_platform CHECK (platform IN (
        'ANDROID_FCM','ANDROID_HMS','ANDROID_MIPUSH','IOS_APNS','WEB_PUSH'
    )),
    CONSTRAINT ck_push_status CHECK (status IN ('ACTIVE','REVOKED')),
    CONSTRAINT ck_push_locale CHECK (locale IN ('zh-CN','en-US'))
);
COMMENT ON TABLE  user_push_token IS '推送令牌表 — 按 (user_id, device_id) 唯一；按平台分发 FCM/HMS/MiPush/APNs/WebPush (DBD §2.6.7)';
COMMENT ON COLUMN user_push_token.push_token IS '推送服务商下发的设备 token，PII: @Desensitize(TOKEN)';
COMMENT ON COLUMN user_push_token.trace_id   IS '全链路追踪标识（HC-04）';

CREATE INDEX IF NOT EXISTS idx_push_user_active      ON user_push_token(user_id, status) WHERE status = 'ACTIVE';
CREATE INDEX IF NOT EXISTS idx_push_platform_active  ON user_push_token(platform, status, last_active_at DESC) WHERE status = 'ACTIVE';

-- ============================================================
-- 8. ws_ticket（一次性 WebSocket 票据）
--    V1, LLD §8.x WebSocket 鉴权
-- ============================================================
CREATE TABLE IF NOT EXISTS ws_ticket (
    id          BIGSERIAL       PRIMARY KEY,
    ticket      VARCHAR(128)    NOT NULL,
    user_id     BIGINT          NOT NULL REFERENCES sys_user(id),
    expire_at   TIMESTAMPTZ     NOT NULL,
    used_at     TIMESTAMPTZ,
    created_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);
COMMENT ON TABLE  ws_ticket IS 'WebSocket 一次性连接票据表';
COMMENT ON COLUMN ws_ticket.ticket IS '票据字符串，一次性使用（used_at 非空即已消费）';

CREATE UNIQUE INDEX IF NOT EXISTS ux_ws_ticket ON ws_ticket(ticket);

-- =====================================================================
-- === PROFILE 域 ===
-- 表：patient_profile, guardian_relation, guardian_invitation,
--      guardian_transfer_request
-- 基线：DBD §2.3, LLD §5
-- =====================================================================

-- ============================================================
-- 9. patient_profile（患者档案表）
--    DBD §2.3.1, SRS FR-PRO-001~010
--    注：fence_center 使用独立 lat/lng（PostGIS 可选部署；DBD 设计为 geometry(Point,4326)）
-- ============================================================
CREATE SEQUENCE IF NOT EXISTS patient_short_code_seq START 1 INCREMENT 1;

CREATE TABLE IF NOT EXISTS patient_profile (
    id                      BIGSERIAL       PRIMARY KEY,
    profile_no              VARCHAR(32)     UNIQUE,
    name                    VARCHAR(64)     NOT NULL,           -- PII: @Desensitize(CHINESE_NAME)（HC-07）
    gender                  VARCHAR(16)     NOT NULL DEFAULT 'UNKNOWN',
    birthday                DATE            NOT NULL,
    short_code              CHAR(6)         NOT NULL,           -- 6 位短码，全局唯一（FR-PRO-003/004）
    id_card_hash            VARCHAR(128),                       -- 身份证哈希（用于重复校验）
    -- 病史拆分字段（V1 实现；DBD §2.3.1 设计为 medical_history jsonb）
    chronic_diseases        VARCHAR(500),
    medication              VARCHAR(500),
    allergy                 VARCHAR(500),
    emergency_contact_phone VARCHAR(32),                        -- PII: @Desensitize(PHONE)
    avatar_url              VARCHAR(1024)   NOT NULL,           -- DBD: photo_url（含义相同，保留 V1 命名）
    long_text_profile       TEXT,                               -- 同步写入向量空间，变更触发 profile.updated 事件（FR-PRO-002）
    -- 外貌特征（DBD §2.3.1 设计为 appearance_tags jsonb）
    appearance_height_cm    INT,
    appearance_weight_kg    INT,
    appearance_clothing     VARCHAR(500),
    appearance_features     VARCHAR(500),
    -- 电子围栏（DBD 设计为 PostGIS geometry；V1 保留 lat/lng 兼容无 PostGIS 部署）
    fence_enabled           BOOLEAN         NOT NULL DEFAULT FALSE,
    fence_center_lat        DOUBLE PRECISION,                   -- HC-Coord: WGS84
    fence_center_lng        DOUBLE PRECISION,
    fence_radius_m          INT,                                -- 配置键 profile.fence.default_radius_m（HC-05）
    fence_coord_system      VARCHAR(10)     DEFAULT 'WGS84',
    lost_status             VARCHAR(20)     NOT NULL DEFAULT 'NORMAL',
    lost_status_event_time  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),  -- 防乱序锚点（SADD §4.6）
    profile_version         BIGINT          NOT NULL DEFAULT 0, -- 乐观锁（HC-01）
    deleted_at              TIMESTAMPTZ,                        -- 逻辑删除（FR-PRO-009）
    trace_id                VARCHAR(64),                        -- HC-04
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_profile_lost_status CHECK (lost_status IN ('NORMAL','MISSING_PENDING','MISSING')),
    CONSTRAINT ck_profile_gender      CHECK (gender IN ('MALE','FEMALE','UNKNOWN')),
    CONSTRAINT ck_fence_logic CHECK (
        (fence_enabled = FALSE)
        OR (fence_enabled = TRUE AND fence_center_lat IS NOT NULL AND fence_center_lng IS NOT NULL
            AND fence_radius_m BETWEEN 100 AND 50000)
    )
);
COMMENT ON TABLE  patient_profile IS '患者档案表 — PROFILE 域聚合根，档案 + 3 态走失状态机 (DBD §2.3.1)';
COMMENT ON COLUMN patient_profile.short_code           IS '6 位短码，全局唯一（FR-PRO-003/004）';
COMMENT ON COLUMN patient_profile.long_text_profile    IS '长文本档案，变更触发 ai-vectorizer-service 重建向量（FR-PRO-002）';
COMMENT ON COLUMN patient_profile.lost_status_event_time IS '防乱序锚点（SADD §4.6）';
COMMENT ON COLUMN patient_profile.profile_version      IS '乐观锁（HC-01）';
COMMENT ON COLUMN patient_profile.deleted_at           IS '逻辑删除（FR-PRO-009），90 天后物理清理';
COMMENT ON COLUMN patient_profile.trace_id             IS '全链路追踪标识（HC-04）';

CREATE UNIQUE INDEX IF NOT EXISTS uq_patient_profile_no   ON patient_profile(profile_no) WHERE profile_no IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uq_patient_short_code   ON patient_profile(short_code);
CREATE INDEX        IF NOT EXISTS idx_patient_lost_status ON patient_profile(lost_status);
CREATE INDEX        IF NOT EXISTS idx_patient_deleted_at  ON patient_profile(deleted_at) WHERE deleted_at IS NULL;

-- ============================================================
-- 10. guardian_relation（监护关系表）
--     DBD §2.3.2, SRS FR-PRO-006
-- ============================================================
CREATE TABLE IF NOT EXISTS guardian_relation (
    id               BIGSERIAL       PRIMARY KEY,
    user_id          BIGINT          NOT NULL REFERENCES sys_user(id),
    patient_id       BIGINT          NOT NULL REFERENCES patient_profile(id),
    relation_role    VARCHAR(32)     NOT NULL,
    relation_status  VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    joined_at        TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    revoked_at       TIMESTAMPTZ,
    version          BIGINT          NOT NULL DEFAULT 0,        -- 乐观锁（HC-01）
    trace_id         VARCHAR(64),                               -- HC-04
    created_at       TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_gr_role   CHECK (relation_role   IN ('PRIMARY_GUARDIAN','GUARDIAN')),
    CONSTRAINT ck_gr_status CHECK (relation_status IN ('PENDING','ACTIVE','REVOKED'))
);
COMMENT ON TABLE  guardian_relation IS '监护关系表 — 长期存在的用户-患者绑定关系 (DBD §2.3.2)';
COMMENT ON COLUMN guardian_relation.trace_id IS '全链路追踪标识（HC-04）';

CREATE UNIQUE INDEX IF NOT EXISTS uq_gr_active_user_patient ON guardian_relation(user_id, patient_id) WHERE relation_status = 'ACTIVE';
CREATE UNIQUE INDEX IF NOT EXISTS uq_gr_primary_per_patient ON guardian_relation(patient_id) WHERE relation_role = 'PRIMARY_GUARDIAN' AND relation_status = 'ACTIVE';
CREATE INDEX        IF NOT EXISTS idx_gr_patient            ON guardian_relation(patient_id, relation_status);
CREATE INDEX        IF NOT EXISTS idx_gr_user               ON guardian_relation(user_id, relation_status);

-- ============================================================
-- 11. guardian_invitation（监护邀请表）
--     DBD §2.3.4, SRS FR-PRO-006
-- ============================================================
CREATE TABLE IF NOT EXISTS guardian_invitation (
    id               BIGSERIAL       PRIMARY KEY,
    invite_id        VARCHAR(64)     NOT NULL,
    patient_id       BIGINT          NOT NULL REFERENCES patient_profile(id),
    inviter_user_id  BIGINT          NOT NULL REFERENCES sys_user(id),
    invitee_user_id  BIGINT          NOT NULL REFERENCES sys_user(id),
    relation_role    VARCHAR(32)     NOT NULL DEFAULT 'GUARDIAN',
    status           VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    reason           VARCHAR(500),
    reject_reason    VARCHAR(256),
    expire_at        TIMESTAMPTZ     NOT NULL,
    accepted_at      TIMESTAMPTZ,
    rejected_at      TIMESTAMPTZ,
    revoked_at       TIMESTAMPTZ,
    version          BIGINT          NOT NULL DEFAULT 0,        -- 乐观锁（HC-01）
    trace_id         VARCHAR(64),                               -- HC-04
    created_at       TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_inv_role   CHECK (relation_role IN ('GUARDIAN')),
    CONSTRAINT ck_inv_status CHECK (status IN ('PENDING','ACCEPTED','REJECTED','EXPIRED','REVOKED'))
);
COMMENT ON TABLE  guardian_invitation IS '监护邀请表 — 主监护人邀请成员加入 (DBD §2.3.4)';
COMMENT ON COLUMN guardian_invitation.invite_id IS '邀请号，全局唯一';
COMMENT ON COLUMN guardian_invitation.trace_id  IS '全链路追踪标识（HC-04）';

CREATE UNIQUE INDEX IF NOT EXISTS uq_inv_invite_id        ON guardian_invitation(invite_id);
CREATE UNIQUE INDEX IF NOT EXISTS uq_inv_pending_per_pair ON guardian_invitation(patient_id, invitee_user_id) WHERE status = 'PENDING';
CREATE INDEX        IF NOT EXISTS idx_inv_invitee          ON guardian_invitation(invitee_user_id, status);
CREATE INDEX        IF NOT EXISTS idx_inv_patient          ON guardian_invitation(patient_id, status);

-- ============================================================
-- 12. guardian_transfer_request（监护权转移请求表）
--     DBD §2.3.3, SRS FR-PRO-007, LLD §5.2.6 监护权协同状态机
--     ⚠ 字段命名偏差：V1 使用 from_user_id/to_user_id
--       DBD 命名 initiator_user_id/target_user_id（保留 V1 实现）
-- ============================================================
CREATE TABLE IF NOT EXISTS guardian_transfer_request (
    id                  BIGSERIAL       PRIMARY KEY,
    request_id          VARCHAR(64)     NOT NULL,
    patient_id          BIGINT          NOT NULL REFERENCES patient_profile(id),
    from_user_id        BIGINT          NOT NULL REFERENCES sys_user(id),  -- DBD: initiator_user_id
    to_user_id          BIGINT          NOT NULL REFERENCES sys_user(id),  -- DBD: target_user_id
    status              VARCHAR(32)     NOT NULL DEFAULT 'PENDING_CONFIRM',
    reason              VARCHAR(500),
    reject_reason       VARCHAR(256),
    cancel_reason       VARCHAR(256),                           -- DBD: revoke_reason
    expire_at           TIMESTAMPTZ     NOT NULL,
    confirmed_at        TIMESTAMPTZ,
    rejected_at         TIMESTAMPTZ,
    revoked_at          TIMESTAMPTZ,
    version             BIGINT          NOT NULL DEFAULT 0,     -- 乐观锁（HC-01）
    trace_id            VARCHAR(64),                            -- HC-04
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_gt_status CHECK (status IN ('PENDING_CONFIRM','COMPLETED','REJECTED','REVOKED','EXPIRED'))
);
COMMENT ON TABLE  guardian_transfer_request IS '监护权转移请求表 — 临时性主监护权转移 (DBD §2.3.3)';
COMMENT ON COLUMN guardian_transfer_request.request_id IS '转移请求号，全局唯一';
COMMENT ON COLUMN guardian_transfer_request.from_user_id IS '当前主监护人（DBD 命名 initiator_user_id）';
COMMENT ON COLUMN guardian_transfer_request.to_user_id   IS '目标主监护人（DBD 命名 target_user_id）';
COMMENT ON COLUMN guardian_transfer_request.trace_id     IS '全链路追踪标识（HC-04）';

CREATE UNIQUE INDEX IF NOT EXISTS uq_gt_request_id      ON guardian_transfer_request(request_id);
CREATE UNIQUE INDEX IF NOT EXISTS uq_gt_pending_patient ON guardian_transfer_request(patient_id) WHERE status = 'PENDING_CONFIRM';
CREATE INDEX        IF NOT EXISTS idx_gt_from           ON guardian_transfer_request(from_user_id, status);
CREATE INDEX        IF NOT EXISTS idx_gt_to             ON guardian_transfer_request(to_user_id, status);

-- =====================================================================
-- === TASK 域 ===
-- 表：rescue_task
-- 基线：DBD §2.1, LLD §3, SRS FR-TASK-001~005
-- =====================================================================

-- ============================================================
-- 13. rescue_task（寻回任务表）
--     DBD §2.1.1, SRS FR-TASK-001~005
-- ============================================================
CREATE TABLE IF NOT EXISTS rescue_task (
    id                    BIGSERIAL       PRIMARY KEY,
    task_no               VARCHAR(32)     UNIQUE,
    patient_id            BIGINT          NOT NULL REFERENCES patient_profile(id),
    status                VARCHAR(32)     NOT NULL DEFAULT 'CREATED',
    source                VARCHAR(32)     NOT NULL,
    remark                VARCHAR(500),
    daily_appearance      TEXT,                               -- 当日着装特征描述，task.created payload 冗余（FR-TASK-003）
    daily_photo_url       VARCHAR(1024),
    ai_analysis_summary   TEXT,
    poster_url            VARCHAR(1024),                      -- AI 海报地址（ai.poster.generated 回写，FR-AI-013）
    close_type            VARCHAR(20),
    close_reason          VARCHAR(256),
    found_location_lat    DOUBLE PRECISION,                   -- 找到时坐标（WGS84，HC-Coord）
    found_location_lng    DOUBLE PRECISION,
    event_version         BIGINT          NOT NULL DEFAULT 0, -- 乐观锁（HC-01），每次状态变更自增
    created_by            BIGINT          NOT NULL REFERENCES sys_user(id),
    closed_by             BIGINT          REFERENCES sys_user(id),
    closed_at             TIMESTAMPTZ,
    sustained_at          TIMESTAMPTZ,                        -- 转为 SUSTAINED 状态的时间
    version               BIGINT          NOT NULL DEFAULT 0, -- 行级乐观锁（HC-01）
    trace_id              VARCHAR(64),                        -- HC-04
    created_at            TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_task_status     CHECK (status IN ('CREATED','ACTIVE','SUSTAINED','CLOSED_FOUND','CLOSED_FALSE_ALARM')),
    -- V1 包含 MINI_PROGRAM（DBD §2.1.1 未列入；保留应用层已有值）
    CONSTRAINT ck_task_source     CHECK (source IN ('APP','MINI_PROGRAM','ADMIN_PORTAL','AUTO_UPGRADE')),
    CONSTRAINT ck_task_close_type CHECK (close_type IS NULL OR close_type IN ('FOUND','FALSE_ALARM'))
);
COMMENT ON TABLE  rescue_task IS '寻回任务表 — TASK 域聚合根，任务状态机唯一权威实体 (DBD §2.1.1)';
COMMENT ON COLUMN rescue_task.event_version IS '乐观锁字段，每次状态变更自增（HC-01）';
COMMENT ON COLUMN rescue_task.daily_appearance IS '当日着装特征描述，task.created 事件 payload 冗余字段（FR-TASK-003）';
COMMENT ON COLUMN rescue_task.trace_id IS '全链路追踪标识（HC-04）';

CREATE UNIQUE INDEX IF NOT EXISTS uq_task_active_per_patient ON rescue_task(patient_id)
    WHERE status NOT IN ('CLOSED_FOUND','CLOSED_FALSE_ALARM');
CREATE UNIQUE INDEX IF NOT EXISTS uq_rescue_task_task_no     ON rescue_task(task_no) WHERE task_no IS NOT NULL;
CREATE INDEX        IF NOT EXISTS idx_task_patient            ON rescue_task(patient_id, created_at DESC);
CREATE INDEX        IF NOT EXISTS idx_task_status             ON rescue_task(status, created_at DESC);

-- =====================================================================
-- === CLUE 域 ===
-- 表：clue_record, patient_trajectory
-- 基线：DBD §2.2, LLD §4, SRS FR-CLUE-001~010
-- =====================================================================

-- ============================================================
-- 14. clue_record（线索记录表）
--     DBD §2.2.1, SRS FR-CLUE-001~010
--     注：location 使用 lat/lng（PostGIS 可选；DBD 设计为 geometry(Point,4326)）
-- ============================================================
CREATE TABLE IF NOT EXISTS clue_record (
    id                  BIGSERIAL       PRIMARY KEY,
    clue_no             VARCHAR(32)     UNIQUE,
    patient_id          BIGINT          NOT NULL REFERENCES patient_profile(id),
    task_id             BIGINT          REFERENCES rescue_task(id),      -- 首条线索可能先于任务
    tag_code            VARCHAR(100),                            -- FK逻辑: ref tag_asset(tag_code)
    source_type         VARCHAR(20)     NOT NULL,
    reporter_user_id    BIGINT          REFERENCES sys_user(id),
    reporter_type       VARCHAR(20)     NOT NULL,
    latitude            DOUBLE PRECISION NOT NULL,              -- HC-Coord: 上报 GCJ-02；服务端返回 WGS84
    longitude           DOUBLE PRECISION NOT NULL,
    coord_system        VARCHAR(10)     NOT NULL DEFAULT 'WGS84',
    description         TEXT,
    photo_urls          JSONB           NOT NULL DEFAULT '[]',  -- 多图 URL 数组（DBD §2.2.1 设计为 photo_url varchar）
    tag_only            BOOLEAN         NOT NULL DEFAULT FALSE,
    risk_score          NUMERIC(5,4),                           -- 风险评分 0-1
    status              VARCHAR(20)     NOT NULL DEFAULT 'VALID',
    suspect_flag        BOOLEAN         NOT NULL DEFAULT FALSE,
    suspect_reason      VARCHAR(256),
    drift_flag          BOOLEAN         NOT NULL DEFAULT FALSE,
    review_status       VARCHAR(20),
    override_reason     VARCHAR(256),
    reject_reason       VARCHAR(256),
    reviewed_by         BIGINT          REFERENCES sys_user(id),
    reviewed_at         TIMESTAMPTZ,
    device_fingerprint  VARCHAR(128),                           -- HC-06 匿名风险隔离
    entry_token_jti     VARCHAR(64),
    client_ip           VARCHAR(64),
    version             BIGINT          NOT NULL DEFAULT 0,     -- 乐观锁（HC-01）
    trace_id            VARCHAR(64),                            -- HC-04
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_clue_source_type   CHECK (source_type   IN ('SCAN','MANUAL','POSTER_SCAN')),
    CONSTRAINT ck_clue_reporter_type CHECK (reporter_type IN ('FAMILY','ANONYMOUS','ADMIN')),
    CONSTRAINT ck_clue_status        CHECK (status        IN ('VALID','OVERRIDDEN','REJECTED','INVALID')),
    CONSTRAINT ck_clue_coord         CHECK (coord_system  IN ('WGS84','GCJ-02','BD-09')),
    CONSTRAINT ck_clue_review_status CHECK (review_status IS NULL OR review_status IN ('PENDING','OVERRIDDEN','REJECTED')),
    CONSTRAINT ck_clue_risk_score    CHECK (risk_score IS NULL OR (risk_score >= 0 AND risk_score <= 1)),
    CONSTRAINT ck_clue_suspect_review CHECK (
        (suspect_flag = FALSE AND review_status IS NULL)
        OR (suspect_flag = TRUE AND review_status IS NOT NULL)
    )
);
COMMENT ON TABLE  clue_record IS '线索记录表 — CLUE 域聚合根，记录路人上报的线索原始数据与研判结果 (DBD §2.2.1)';
COMMENT ON COLUMN clue_record.device_fingerprint IS '匿名风险隔离必填字段（HC-06）';
COMMENT ON COLUMN clue_record.trace_id           IS '全链路追踪标识（HC-04）';

CREATE UNIQUE INDEX IF NOT EXISTS uq_clue_record_clue_no    ON clue_record(clue_no) WHERE clue_no IS NOT NULL;
CREATE INDEX        IF NOT EXISTS idx_clue_patient          ON clue_record(patient_id, created_at DESC);
CREATE INDEX        IF NOT EXISTS idx_clue_task             ON clue_record(task_id, created_at DESC);
CREATE INDEX        IF NOT EXISTS idx_clue_tag_code         ON clue_record(tag_code);
CREATE INDEX        IF NOT EXISTS idx_clue_suspect          ON clue_record(suspect_flag, review_status)
    WHERE suspect_flag = TRUE AND review_status = 'PENDING';
CREATE INDEX        IF NOT EXISTS idx_clue_review           ON clue_record(review_status, created_at DESC)
    WHERE review_status = 'PENDING';
CREATE INDEX        IF NOT EXISTS idx_clue_created_at       ON clue_record(created_at);

-- ============================================================
-- 15. patient_trajectory（患者轨迹表）
--     DBD §2.2.2, SRS FR-CLUE-010
--     注：geometry_data 使用 JSONB（PostGIS 可选；DBD 设计为 geometry）
-- ============================================================
CREATE TABLE IF NOT EXISTS patient_trajectory (
    id              BIGSERIAL       PRIMARY KEY,
    patient_id      BIGINT          NOT NULL REFERENCES patient_profile(id),
    task_id         BIGINT          REFERENCES rescue_task(id),
    clue_id         BIGINT          REFERENCES clue_record(id),
    window_start    TIMESTAMPTZ     NOT NULL,
    window_end      TIMESTAMPTZ     NOT NULL,
    point_count     INT             NOT NULL DEFAULT 0,
    geometry_type   VARCHAR(32)     NOT NULL,
    geometry_data   JSONB,                                      -- DBD 设计为 PostGIS geometry；V1 保留 JSONB
    latitude        DOUBLE PRECISION,                          -- POINT 类型时的单点坐标
    longitude       DOUBLE PRECISION,
    source_type     VARCHAR(32),
    version         BIGINT          NOT NULL DEFAULT 0,
    trace_id        VARCHAR(64),                                -- HC-04
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_traj_geom_type CHECK (geometry_type IN ('LINESTRING','SPARSE_POINT','EMPTY_WINDOW','POINT'))
);
COMMENT ON TABLE  patient_trajectory IS '患者轨迹表 — 将离散有效坐标点按时间窗口聚合为连续轨迹数据对象 (DBD §2.2.2)';
COMMENT ON COLUMN patient_trajectory.geometry_data IS 'JSONB 轨迹几何，EMPTY_WINDOW 时为 NULL（DBD §2.2.2 设计为 PostGIS geometry）';
COMMENT ON COLUMN patient_trajectory.trace_id      IS '全链路追踪标识（HC-04）';

CREATE INDEX IF NOT EXISTS idx_traj_patient_window ON patient_trajectory(patient_id, window_start DESC);
CREATE INDEX IF NOT EXISTS idx_traj_task           ON patient_trajectory(task_id, created_at DESC);

-- =====================================================================
-- === MAT 域 ===
-- 表：tag_asset, tag_apply_record, tag_batch_job
-- 基线：DBD §2.4, LLD §6, SRS FR-MAT-001~005
-- =====================================================================

-- ============================================================
-- 16. tag_apply_record（物资申领工单表）
--     DBD §2.4.2, SRS FR-MAT-001/004, LLD §5.2.5 工单状态机
--     V3 补充 resolve_reason / resolved_by / resolved_at
--     先建工单表，因为 tag_asset.order_id FK 指向此表
-- ============================================================
CREATE TABLE IF NOT EXISTS tag_apply_record (
    id                      BIGSERIAL       PRIMARY KEY,
    order_no                VARCHAR(32)     UNIQUE,
    patient_id              BIGINT          NOT NULL REFERENCES patient_profile(id),
    applicant_user_id       BIGINT          NOT NULL REFERENCES sys_user(id),
    tag_type                VARCHAR(20)     NOT NULL,
    quantity                INT             NOT NULL,
    remark                  VARCHAR(500),
    status                  VARCHAR(32)     NOT NULL DEFAULT 'PENDING_AUDIT',
    -- 地址（DBD §2.4.2 设计为 shipping_address varchar(512)；V1 拆分为省市区）
    shipping_province       VARCHAR(64),
    shipping_city           VARCHAR(64),
    shipping_district       VARCHAR(64),
    shipping_detail         VARCHAR(512),
    shipping_receiver       VARCHAR(64),                        -- DBD: shipping_contact（PII: @Desensitize(CHINESE_NAME)）
    shipping_phone          VARCHAR(32),                        -- DBD: shipping_phone（PII: @Desensitize(PHONE)）
    reviewer_user_id        BIGINT          REFERENCES sys_user(id),   -- DBD: audit_by
    reviewed_at             TIMESTAMPTZ,                        -- DBD: audit_at
    reject_reason           VARCHAR(256),
    tag_codes               JSONB,                              -- 分配的标签码列表
    logistics_no            VARCHAR(64),
    logistics_company       VARCHAR(64),
    shipped_at              TIMESTAMPTZ,
    received_at             TIMESTAMPTZ,
    cancel_reason           VARCHAR(256),
    cancelled_at            TIMESTAMPTZ,
    exception_desc          VARCHAR(512),                       -- DBD: exception_reason（V1 简化为单字段）
    -- V3 物流异常处置字段（LLD §6.3.8, API §3.4.12）
    resolve_reason          VARCHAR(256),                       -- DBD: exception_resolved_remark
    resolved_by             BIGINT          REFERENCES sys_user(id),
    resolved_at             TIMESTAMPTZ,
    version                 BIGINT          NOT NULL DEFAULT 0, -- 乐观锁（HC-01）
    trace_id                VARCHAR(64),                        -- HC-04
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_apply_status  CHECK (status IN ('PENDING_AUDIT','PENDING_SHIP','SHIPPED','RECEIVED','EXCEPTION','REJECTED','CANCELLED','VOIDED')),
    CONSTRAINT ck_apply_tag_type CHECK (tag_type IN ('QR_CODE','NFC'))
);
COMMENT ON TABLE  tag_apply_record IS '物资申领工单表 — MAT 域聚合根，6 主态 + REJECTED/CANCELLED 终态 (DBD §2.4.2)';
COMMENT ON COLUMN tag_apply_record.shipping_receiver IS 'PII: @Desensitize(CHINESE_NAME)（HC-07）';
COMMENT ON COLUMN tag_apply_record.shipping_phone    IS 'PII: @Desensitize(PHONE)（HC-07）';
COMMENT ON COLUMN tag_apply_record.version           IS '乐观锁（HC-01）';
COMMENT ON COLUMN tag_apply_record.trace_id          IS '全链路追踪标识（HC-04）';

CREATE UNIQUE INDEX IF NOT EXISTS uq_tag_apply_record_order_no    ON tag_apply_record(order_no) WHERE order_no IS NOT NULL;
-- V3: 物流单号唯一约束（LLD §6.3.3）
CREATE UNIQUE INDEX IF NOT EXISTS uq_tag_apply_record_logistics_no ON tag_apply_record(logistics_no) WHERE logistics_no IS NOT NULL;
CREATE INDEX        IF NOT EXISTS idx_apply_patient               ON tag_apply_record(patient_id, status, created_at DESC);
CREATE INDEX        IF NOT EXISTS idx_apply_applicant             ON tag_apply_record(applicant_user_id, created_at DESC);
CREATE INDEX        IF NOT EXISTS idx_apply_status                ON tag_apply_record(status, created_at DESC);

-- ============================================================
-- 17. tag_asset（标签资产表）
--     DBD §2.4.1, SRS FR-MAT-002~005, LLD §5.2.3 标签状态机
-- ============================================================
CREATE TABLE IF NOT EXISTS tag_asset (
    id                BIGSERIAL       PRIMARY KEY,
    tag_code          VARCHAR(100)    NOT NULL,                -- 全局唯一
    tag_type          VARCHAR(20)     NOT NULL DEFAULT 'QR_CODE',
    status            VARCHAR(20)     NOT NULL DEFAULT 'UNBOUND',
    patient_id        BIGINT          REFERENCES patient_profile(id),
    short_code        CHAR(6),                                 -- FK逻辑: ref patient_profile(short_code)
    order_id          BIGINT          REFERENCES tag_apply_record(id),
    resource_token    VARCHAR(512),                            -- SADD §3.5 路由凭据
    batch_no          VARCHAR(64),
    void_reason       VARCHAR(256),
    lost_reason       VARCHAR(256),
    lost_at           TIMESTAMPTZ,
    void_at           TIMESTAMPTZ,
    suspected_lost_at TIMESTAMPTZ,
    bound_at          TIMESTAMPTZ,
    allocated_at      TIMESTAMPTZ,
    version           BIGINT          NOT NULL DEFAULT 0,      -- 乐观锁（HC-01）
    trace_id          VARCHAR(64),                             -- HC-04
    created_at        TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_tag_status CHECK (status IN ('UNBOUND','ALLOCATED','BOUND','SUSPECTED_LOST','LOST','VOIDED')),
    CONSTRAINT ck_tag_type   CHECK (tag_type IN ('QR_CODE','NFC'))
);
COMMENT ON TABLE  tag_asset IS '标签资产表 — MAT 域聚合根，防走失标签全生命周期主数据（6 态状态机）(DBD §2.4.1)';
COMMENT ON COLUMN tag_asset.resource_token IS 'SADD §3.5 路由凭据，内含签名参数';
COMMENT ON COLUMN tag_asset.version        IS '乐观锁字段，所有状态变更必须通过聚合根方法 + CAS 更新（HC-01）';
COMMENT ON COLUMN tag_asset.trace_id       IS '全链路追踪标识（HC-04）';

CREATE UNIQUE INDEX IF NOT EXISTS uq_tag_asset_tag_code ON tag_asset(tag_code);
CREATE INDEX        IF NOT EXISTS idx_tag_status        ON tag_asset(status);
CREATE INDEX        IF NOT EXISTS idx_tag_patient       ON tag_asset(patient_id, status);
CREATE INDEX        IF NOT EXISTS idx_tag_short_code    ON tag_asset(short_code);
CREATE INDEX        IF NOT EXISTS idx_tag_batch         ON tag_asset(batch_no);
CREATE INDEX        IF NOT EXISTS idx_tag_order_id      ON tag_asset(order_id);

-- ============================================================
-- 18. tag_batch_job（批量发号任务表）
--     V1, LLD §6.x 批量标签生成
-- ============================================================
CREATE TABLE IF NOT EXISTS tag_batch_job (
    id              BIGSERIAL       PRIMARY KEY,
    job_id          VARCHAR(64)     NOT NULL,
    tag_type        VARCHAR(20)     NOT NULL,
    quantity        INT             NOT NULL,
    success_count   INT             NOT NULL DEFAULT 0,
    fail_count      INT             NOT NULL DEFAULT 0,
    status          VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    batch_key_id    VARCHAR(64),
    created_by      BIGINT          NOT NULL REFERENCES sys_user(id),
    completed_at    TIMESTAMPTZ,
    trace_id        VARCHAR(64),                               -- HC-04
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_batch_status CHECK (status IN ('PENDING','RUNNING','COMPLETED','FAILED'))
);
COMMENT ON TABLE  tag_batch_job IS '标签批量生成任务表';
COMMENT ON COLUMN tag_batch_job.trace_id IS '全链路追踪标识（HC-04）';

CREATE UNIQUE INDEX IF NOT EXISTS ux_tag_batch_job_id ON tag_batch_job(job_id);

-- =====================================================================
-- === AI 域 ===
-- 表：ai_session, ai_quota_ledger, patient_memory_note,
--      vector_store, ai_intent
-- 基线：DBD §2.5, LLD §7, SRS FR-AI-001~014
-- =====================================================================

-- ============================================================
-- 19. ai_session（AI 会话表）
--     DBD §2.5.1, SRS FR-AI-001/009/011/014
-- ============================================================
CREATE TABLE IF NOT EXISTS ai_session (
    id                       BIGSERIAL       PRIMARY KEY,
    session_id               VARCHAR(64)     NOT NULL,
    user_id                  BIGINT          NOT NULL REFERENCES sys_user(id),
    patient_id               BIGINT          NOT NULL REFERENCES patient_profile(id),
    task_id                  BIGINT          REFERENCES rescue_task(id),   -- FK逻辑，可空
    messages                 JSONB           NOT NULL DEFAULT '[]',        -- 含 tool_calls 结构
    -- Token 计量（DBD 字段名：request_tokens/response_tokens/token_used）
    prompt_tokens            INT             NOT NULL DEFAULT 0,           -- DBD: request_tokens
    completion_tokens        INT             NOT NULL DEFAULT 0,           -- DBD: response_tokens
    total_tokens             INT             NOT NULL DEFAULT 0,           -- DBD: token_used
    token_usage              JSONB           NOT NULL DEFAULT '{}',        -- 细粒度计费明细
    model_name               VARCHAR(64)     NOT NULL DEFAULT 'qwen-max-latest',
    prompt_template_version  VARCHAR(32),                                  -- DBD §2.5.1
    status                   VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    feedback                 VARCHAR(20),                                   -- DBD §2.5.1 ADOPTED/USELESS
    feedback_rating          INT,                                           -- V1 扩展评分
    feedback_comment         VARCHAR(1000),
    feedback_at              TIMESTAMPTZ,
    archived_at              TIMESTAMPTZ,
    version                  BIGINT          NOT NULL DEFAULT 0,           -- 乐观锁，禁止全量覆盖（HC-01）
    trace_id                 VARCHAR(64),                                   -- HC-04
    created_at               TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at               TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_ai_status   CHECK (status   IN ('ACTIVE','ARCHIVED')),
    CONSTRAINT ck_ai_feedback CHECK (feedback IS NULL OR feedback IN ('ADOPTED','USELESS'))
);
COMMENT ON TABLE  ai_session IS 'AI 会话表 — 存储家属 AI 对话的完整上下文与 Token 消耗 (DBD §2.5.1)';
COMMENT ON COLUMN ai_session.messages   IS 'JSONB 消息数组，禁止全量读改写覆盖，必须 version CAS 更新';
COMMENT ON COLUMN ai_session.token_usage IS '细粒度计费明细 {prompt_tokens, completion_tokens, total_tokens, model_name, estimated_cost}';
COMMENT ON COLUMN ai_session.version    IS '乐观锁（HC-01）';
COMMENT ON COLUMN ai_session.trace_id   IS '全链路追踪标识（HC-04）';

CREATE UNIQUE INDEX IF NOT EXISTS ux_ai_session_id       ON ai_session(session_id);
CREATE UNIQUE INDEX IF NOT EXISTS uq_ai_session_active   ON ai_session(patient_id, task_id) WHERE status = 'ACTIVE';
CREATE INDEX        IF NOT EXISTS idx_ai_session_user    ON ai_session(user_id, created_at DESC);
CREATE INDEX        IF NOT EXISTS idx_ai_session_patient ON ai_session(patient_id, created_at DESC);
CREATE INDEX        IF NOT EXISTS idx_ai_session_task    ON ai_session(task_id);
CREATE INDEX        IF NOT EXISTS idx_ai_session_status  ON ai_session(status);
CREATE INDEX        IF NOT EXISTS gin_ai_session_messages ON ai_session USING gin (messages);

-- ============================================================
-- 20. ai_quota_ledger（AI 配额台账表）
--     DBD §2.5.4, SRS FR-AI-009
--     补充 trace_id / status（DBD §2.5.4 要求字段，V1 缺失）
-- ============================================================
CREATE TABLE IF NOT EXISTS ai_quota_ledger (
    id           BIGSERIAL       PRIMARY KEY,
    ledger_type  VARCHAR(20)     NOT NULL,
    owner_id     BIGINT          NOT NULL,                     -- user_id 或 patient_id
    period       VARCHAR(20)     NOT NULL,                     -- 计费周期，如 '2026-04'
    quota_limit  INT             NOT NULL,                     -- 配额上限（Token），配置键 ai.quota.{ledger_type}.monthly_limit（HC-05）
    used         INT             NOT NULL DEFAULT 0,
    reserved     INT             NOT NULL DEFAULT 0,
    status       VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',    -- DBD §2.5.4（V1 补充）
    version      BIGINT          NOT NULL DEFAULT 0,           -- 乐观锁（HC-01）
    trace_id     VARCHAR(64)     NOT NULL DEFAULT '',          -- HC-04（DBD §2.5.4 要求，V1 补充）
    created_at   TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_quota_type   CHECK (ledger_type IN ('USER','PATIENT')),
    CONSTRAINT ck_quota_status CHECK (status       IN ('ACTIVE','EXHAUSTED'))
);
COMMENT ON TABLE  ai_quota_ledger IS 'AI 配额台账表 — 双维度（用户/患者）配额账本，预占-确认-回滚状态机 (DBD §2.5.4)';
COMMENT ON COLUMN ai_quota_ledger.quota_limit IS '配额上限（HC-05），从配置中心 ai.quota.{ledger_type}.monthly_limit 读取';
COMMENT ON COLUMN ai_quota_ledger.status      IS 'ACTIVE / EXHAUSTED（DBD §2.5.4）';
COMMENT ON COLUMN ai_quota_ledger.trace_id    IS '全链路追踪标识（HC-04）';

CREATE UNIQUE INDEX IF NOT EXISTS uq_quota_key ON ai_quota_ledger(ledger_type, owner_id, period);

-- ============================================================
-- 21. patient_memory_note（患者记忆条目表）
--     DBD §2.5.2, LLD §7.1.2, SRS BR-003
-- ============================================================
CREATE TABLE IF NOT EXISTS patient_memory_note (
    id               BIGSERIAL       PRIMARY KEY,
    note_id          VARCHAR(64)     NOT NULL,
    patient_id       BIGINT          NOT NULL REFERENCES patient_profile(id),
    created_by       BIGINT          NOT NULL REFERENCES sys_user(id),
    kind             VARCHAR(32)     NOT NULL,
    content          TEXT            NOT NULL,
    tags             JSONB,
    source_version   BIGINT,                                    -- DBD §2.5.2
    source_event_id  VARCHAR(64),
    version          BIGINT          NOT NULL DEFAULT 0,        -- 乐观锁（HC-01）
    trace_id         VARCHAR(64),                               -- HC-04
    created_at       TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_memory_kind CHECK (kind IN ('HABIT','PLACE','PREFERENCE','SAFETY_CUE','RESCUE_CASE'))
);
COMMENT ON TABLE  patient_memory_note IS '患者记忆条目表 — AI 从对话/任务中沉淀的结构化患者记忆 (DBD §2.5.2)';
COMMENT ON COLUMN patient_memory_note.kind IS 'RESCUE_CASE 仅由 task.closed.found 触发，false_alarm 禁止沉淀（BR-003）';
COMMENT ON COLUMN patient_memory_note.trace_id IS '全链路追踪标识（HC-04）';

CREATE UNIQUE INDEX IF NOT EXISTS ux_memory_note_id  ON patient_memory_note(note_id);
CREATE INDEX        IF NOT EXISTS idx_memory_patient  ON patient_memory_note(patient_id, created_at DESC);
CREATE INDEX        IF NOT EXISTS idx_memory_kind     ON patient_memory_note(patient_id, kind);

-- ============================================================
-- 22. vector_store（向量存储表）
--     DBD §2.5.3, LLD §7.1.3, SADD ADR-003
--
--  ★ 关键变更：embedding vector(1536)
--      — 原始基线 DBD §2.5.3 / LLD §7.1.3 L2883 设计为 vector(1024)
--      — 用户请求升至 1536（兼容 OpenAI text-embedding-3-small/large、
--        Cohere embed-v3 等主流模型的 1536 维输出）
--      — 配置键 ai.embedding.model_dimension 同步更新为 1536
--
--  ★ HNSW 参数修正：m=32, ef_construction=256
--      — V7 原实现为 m=16, ef_construction=64
--      — 与 LLD §7.1.3 L2895 基线对齐
--
--  ⚠ 字段命名偏差：chunk_index（DBD §2.5.3 命名 chunk_no；保留 V7 实现）
-- ============================================================
CREATE TABLE IF NOT EXISTS vector_store (
    id              BIGSERIAL       PRIMARY KEY,
    patient_id      BIGINT          NOT NULL REFERENCES patient_profile(id),  -- 检索隔离键（DBD §2.5.3）
    source_type     VARCHAR(32)     NOT NULL,
    source_id       VARCHAR(64)     NOT NULL,
    source_version  BIGINT          NOT NULL DEFAULT 1,
    chunk_index     INT             NOT NULL DEFAULT 0,        -- V7: DBD 命名 chunk_no（保留 V7 实现）
    chunk_hash      VARCHAR(64),                               -- 切片内容 Hash（去重）
    content         TEXT            NOT NULL,
    embedding       vector(1536)    NOT NULL,                  -- ★ 1536 维（用户变更；DBD §2.5.3 设计为 1024）
    valid           BOOLEAN         NOT NULL DEFAULT TRUE,
    superseded_at   TIMESTAMPTZ,
    deleted_at      TIMESTAMPTZ,                               -- 逻辑删除（profile.deleted.logical 时物理删除）
    expired_at      TIMESTAMPTZ,
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),    -- V7 新增
    trace_id        VARCHAR(64),                               -- HC-04
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_vector_source_type CHECK (source_type IN ('PROFILE','MEMORY','RESCUE_CASE'))
);
COMMENT ON TABLE  vector_store IS '向量存储表 — 存储患者档案/记忆/线索的 Embedding 向量，供 RAG 语义检索 (DBD §2.5.3)';
COMMENT ON COLUMN vector_store.embedding  IS '★ 1536 维向量（用户变更；兼容 OpenAI text-embedding-3 / Cohere embed-v3）；HNSW 索引，cosine 距离。配置键 ai.embedding.model_dimension=1536';
COMMENT ON COLUMN vector_store.patient_id IS '检索隔离键：WHERE patient_id=:pid AND valid=true，禁止全局 ANN 后过滤（DBD §2.5.3）';
COMMENT ON COLUMN vector_store.chunk_index IS '长文本切片序号（0..N），DBD 命名 chunk_no，保留 V7 实现（LLD §7.1.3）';
COMMENT ON COLUMN vector_store.trace_id   IS '全链路追踪标识（HC-04）';

-- HNSW 索引（★ 修正：m=32, ef_construction=256，与 LLD §7.1.3 L2895 对齐）
-- V7 原实现为 m=16, ef_construction=64（参数偏差，已修正）
CREATE INDEX IF NOT EXISTS hnsw_vector_store_embedding ON vector_store
    USING hnsw (embedding vector_cosine_ops)
    WITH (m = 32, ef_construction = 256)
    WHERE valid = TRUE;

CREATE UNIQUE INDEX IF NOT EXISTS uk_vector_store_source ON vector_store
    (patient_id, source_type, source_id, chunk_index)
    WHERE valid = TRUE;

CREATE INDEX IF NOT EXISTS idx_vector_store_patient ON vector_store(patient_id, valid);
CREATE INDEX IF NOT EXISTS idx_vector_store_source  ON vector_store(source_type, source_id);

-- ============================================================
-- 23. ai_intent（Agent 意图待确认表）
--     V1, LLD §7.x AI Agent 高风险意图确认
-- ============================================================
CREATE TABLE IF NOT EXISTS ai_intent (
    id               BIGSERIAL       PRIMARY KEY,
    intent_id        VARCHAR(64)     NOT NULL,
    session_id       VARCHAR(64)     NOT NULL,
    user_id          BIGINT          NOT NULL REFERENCES sys_user(id),
    action           VARCHAR(64)     NOT NULL,
    description      VARCHAR(500),
    parameters       JSONB,
    execution_level  VARCHAR(20)     NOT NULL DEFAULT 'CONFIRM_1',
    requires_confirm BOOLEAN         NOT NULL DEFAULT TRUE,
    status           VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    execution_result JSONB,
    expire_at        TIMESTAMPTZ     NOT NULL,
    processed_at     TIMESTAMPTZ,
    version          BIGINT          NOT NULL DEFAULT 0,
    trace_id         VARCHAR(64),                               -- HC-04
    created_at       TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_intent_status CHECK (status IN ('PENDING','APPROVED','REJECTED','EXPIRED'))
);
COMMENT ON TABLE  ai_intent IS 'AI 意图待确认表 — 存储需要用户确认的高风险操作意图';
COMMENT ON COLUMN ai_intent.trace_id IS '全链路追踪标识（HC-04）';

CREATE UNIQUE INDEX IF NOT EXISTS ux_ai_intent_id     ON ai_intent(intent_id);
CREATE INDEX        IF NOT EXISTS idx_ai_intent_session ON ai_intent(session_id, status);

-- =====================================================================
-- === 种子数据（V5 client version + 系统基础配置）===
-- 基线：API §3.8.6.1, backend_handbook §25.7, HC-05
-- =====================================================================

-- 客户端版本配置（V5）
INSERT INTO sys_config (config_key, config_value, scope, description, created_at, updated_at, version)
VALUES
  ('client.version.android.latest',             '2.1.0',  'public', '当前最新 Android 版本',   now(), now(), 0),
  ('client.version.android.min_compatible',     '2.0.0',  'public', 'Android 最低兼容版本',   now(), now(), 0),
  ('client.version.android.force_upgrade',      'false',  'public', 'Android 是否强升',        now(), now(), 0),
  ('client.version.android.release_notes_url',
    'https://static.example.com/release/android/2.1.0.html', 'public', '发版说明', now(), now(), 0),
  ('client.version.android.download_url',
    'https://static.example.com/apk/android/mashang-2.1.0.apk', 'public', 'APK 下载', now(), now(), 0),
  ('client.version.h5.latest',                  '2.1.0',  'public', '当前最新 H5 版本',         now(), now(), 0),
  ('client.version.h5.min_compatible',          '2.0.0',  'public', 'H5 最低兼容版本',          now(), now(), 0),
  ('client.version.h5.force_upgrade',           'false',  'public', 'H5 是否强升',              now(), now(), 0),
  ('client.version.web_admin.latest',           '2.1.0',  'public', '当前最新管理端版本',        now(), now(), 0),
  ('client.version.web_admin.min_compatible',   '2.0.0',  'public', '管理端最低兼容版本',        now(), now(), 0),
  ('client.version.web_admin.force_upgrade',    'false',  'public', '管理端是否强升',             now(), now(), 0),
-- AI 向量化模型配置（★ 1536 维，与 embedding 列对齐）
  ('ai.embedding.model_dimension',   '1536',   'ai_policy', 'Embedding 向量维度（★ 升至 1536，兼容 OpenAI text-embedding-3）', now(), now(), 0),
  ('ai.embedding.model_name',        'text-embedding-3-small', 'ai_policy', 'Embedding 模型名称', now(), now(), 0),
-- AI HNSW 检索配置（LLD §7.1.3）
  ('ai.vector.hnsw_m',               '32',     'ai_policy', 'HNSW m 参数（LLD §7.1.3）',         now(), now(), 0),
  ('ai.vector.hnsw_ef_construction', '256',    'ai_policy', 'HNSW ef_construction（LLD §7.1.3）', now(), now(), 0),
  ('ai.vector.ef_search',            '80',     'ai_policy', 'HNSW 查询 ef_search，范围 40-200（LLD §7.1.3）', now(), now(), 0),
-- 围栏默认半径（HC-05）
  ('profile.fence.default_radius_m', '500',    'public', '电子围栏默认半径（米），HC-05',         now(), now(), 0),
-- AI 配额（HC-05）
  ('ai.quota.USER.monthly_limit',    '100000', 'ai_policy', '用户月度 Token 配额上限',            now(), now(), 0),
  ('ai.quota.PATIENT.monthly_limit', '50000',  'ai_policy', '患者月度 Token 配额上限',            now(), now(), 0)
ON CONFLICT (config_key) DO NOTHING;

-- =====================================================================
-- === 完成提示 ===
-- 执行完成后请验证：
--   SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public';
--   -- 预期约 23 张表
--   SELECT column_name, udt_name FROM information_schema.columns
--     WHERE table_name = 'vector_store' AND column_name = 'embedding';
--   -- 预期 udt_name = 'vector'，typmod = 1536
-- =====================================================================

