-- V1__init_schema.sql
-- Alzheimer Guard System - Full Schema
-- PostgreSQL 16, requires: PostGIS, pgvector (extensions installed manually or via init)

-- ============================================================
-- 1. sys_user（系统用户）
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_user (
    id               BIGSERIAL PRIMARY KEY,
    username         VARCHAR(64)  NOT NULL,
    password_hash    VARCHAR(128) NOT NULL,
    display_name     VARCHAR(64),
    phone            VARCHAR(20),
    role             VARCHAR(20)  NOT NULL DEFAULT 'FAMILY',
    status           VARCHAR(20)  NOT NULL DEFAULT 'NORMAL',
    last_login_at    TIMESTAMPTZ,
    last_login_ip    VARCHAR(64),
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_user_role   CHECK (role   IN ('FAMILY','ADMIN','SUPERADMIN')),
    CONSTRAINT chk_user_status CHECK (status IN ('NORMAL','BANNED'))
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_sys_user_username    ON sys_user(username);
CREATE INDEX        IF NOT EXISTS idx_sys_user_phone       ON sys_user(phone);
CREATE INDEX        IF NOT EXISTS idx_sys_user_role_status ON sys_user(role, status);

-- ============================================================
-- 2. patient_profile（患者档案）
-- ============================================================
CREATE SEQUENCE IF NOT EXISTS patient_short_code_seq START 1 INCREMENT 1;

CREATE TABLE IF NOT EXISTS patient_profile (
    id                      BIGSERIAL    PRIMARY KEY,
    profile_no              VARCHAR(32)  UNIQUE,
    name                    VARCHAR(64)  NOT NULL,
    gender                  VARCHAR(16)  NOT NULL,
    birthday                DATE         NOT NULL,
    short_code              CHAR(6)      NOT NULL,
    pin_code_hash           VARCHAR(128) NOT NULL,
    pin_code_salt           VARCHAR(64)  NOT NULL,
    photo_url               VARCHAR(1024) NOT NULL,
    medical_history         JSONB        NOT NULL DEFAULT '{}',
    fence_enabled           BOOLEAN      NOT NULL DEFAULT FALSE,
    fence_center_lat        DOUBLE PRECISION,
    fence_center_lng        DOUBLE PRECISION,
    fence_radius_m          INT,
    lost_status             VARCHAR(20)  NOT NULL DEFAULT 'NORMAL',
    lost_status_event_time  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    profile_version         BIGINT       NOT NULL DEFAULT 1,
    created_at              TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_patient_lost_status CHECK (lost_status IN ('NORMAL','MISSING')),
    CONSTRAINT chk_patient_gender      CHECK (gender IN ('MALE','FEMALE','UNKNOWN')),
    CONSTRAINT chk_fence_logic CHECK (
        (fence_enabled = FALSE AND fence_center_lat IS NULL AND fence_center_lng IS NULL AND fence_radius_m IS NULL)
        OR
        (fence_enabled = TRUE AND fence_center_lat IS NOT NULL AND fence_center_lng IS NOT NULL
            AND fence_radius_m BETWEEN 50 AND 5000)
    )
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_patient_profile_no  ON patient_profile(profile_no) WHERE profile_no IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS ux_patient_short_code  ON patient_profile(short_code);
CREATE INDEX        IF NOT EXISTS idx_patient_lost_status ON patient_profile(lost_status);
CREATE INDEX        IF NOT EXISTS gin_patient_med_hist    ON patient_profile USING GIN(medical_history);

-- ============================================================
-- 3. sys_user_patient（用户-患者关系）
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_user_patient (
    id                      BIGSERIAL    PRIMARY KEY,
    user_id                 BIGINT       NOT NULL REFERENCES sys_user(id),
    patient_id              BIGINT       NOT NULL REFERENCES patient_profile(id),
    relation_role           VARCHAR(32)  NOT NULL,
    relation_status         VARCHAR(20)  NOT NULL,
    transfer_state          VARCHAR(32)  NOT NULL DEFAULT 'NONE',
    transfer_request_id     VARCHAR(64),
    transfer_target_user_id BIGINT       REFERENCES sys_user(id),
    transfer_requested_by   BIGINT       REFERENCES sys_user(id),
    transfer_requested_at   TIMESTAMPTZ,
    transfer_reason         VARCHAR(256),
    transfer_cancelled_by   BIGINT       REFERENCES sys_user(id),
    transfer_cancelled_at   TIMESTAMPTZ,
    transfer_cancel_reason  VARCHAR(256),
    transfer_expire_at      TIMESTAMPTZ,
    transfer_confirmed_at   TIMESTAMPTZ,
    transfer_rejected_at    TIMESTAMPTZ,
    transfer_reject_reason  VARCHAR(256),
    created_at              TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_rup_relation_status CHECK (relation_status IN ('PENDING','ACTIVE','REVOKED')),
    CONSTRAINT chk_rup_transfer_state  CHECK (transfer_state  IN ('NONE','PENDING_CONFIRM','ACCEPTED','REJECTED','CANCELLED','EXPIRED'))
);

CREATE INDEX IF NOT EXISTS idx_user_patient_user           ON sys_user_patient(user_id, relation_status);
CREATE INDEX IF NOT EXISTS idx_user_patient_patient        ON sys_user_patient(patient_id, relation_status);
CREATE INDEX IF NOT EXISTS idx_user_patient_transfer_req   ON sys_user_patient(transfer_request_id);

-- 同患者唯一 PENDING_CONFIRM
CREATE UNIQUE INDEX IF NOT EXISTS uq_transfer_pending      ON sys_user_patient(patient_id) WHERE transfer_state = 'PENDING_CONFIRM';
-- 同患者唯一 PRIMARY_GUARDIAN ACTIVE
CREATE UNIQUE INDEX IF NOT EXISTS uq_primary_guardian      ON sys_user_patient(patient_id) WHERE relation_role = 'PRIMARY_GUARDIAN' AND relation_status = 'ACTIVE';
-- transfer_request_id 全局唯一
CREATE UNIQUE INDEX IF NOT EXISTS uq_transfer_request_id   ON sys_user_patient(transfer_request_id) WHERE transfer_request_id IS NOT NULL;

-- ============================================================
-- 4. guardian_invitation（监护邀请）
-- ============================================================
CREATE TABLE IF NOT EXISTS guardian_invitation (
    id               BIGSERIAL    PRIMARY KEY,
    invite_id        VARCHAR(64)  NOT NULL,
    patient_id       BIGINT       NOT NULL REFERENCES patient_profile(id),
    inviter_user_id  BIGINT       NOT NULL REFERENCES sys_user(id),
    invitee_user_id  BIGINT       NOT NULL REFERENCES sys_user(id),
    relation_role    VARCHAR(32)  NOT NULL,
    status           VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    reason           VARCHAR(256),
    reject_reason    VARCHAR(256),
    expire_at        TIMESTAMPTZ  NOT NULL,
    accepted_at      TIMESTAMPTZ,
    rejected_at      TIMESTAMPTZ,
    revoked_at       TIMESTAMPTZ,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_inv_status CHECK (status IN ('PENDING','ACCEPTED','REJECTED','EXPIRED','REVOKED'))
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_guardian_invite_id      ON guardian_invitation(invite_id);
CREATE UNIQUE INDEX IF NOT EXISTS uq_guardian_invite_pending ON guardian_invitation(patient_id, invitee_user_id) WHERE status = 'PENDING';
CREATE INDEX        IF NOT EXISTS idx_guardian_inv_patient   ON guardian_invitation(patient_id, status, created_at DESC);
CREATE INDEX        IF NOT EXISTS idx_guardian_inv_invitee   ON guardian_invitation(invitee_user_id, status, created_at DESC);
CREATE INDEX        IF NOT EXISTS idx_guardian_inv_expire    ON guardian_invitation(status, expire_at) WHERE status = 'PENDING';

-- ============================================================
-- 5. rescue_task（寻回任务）
-- ============================================================
CREATE TABLE IF NOT EXISTS rescue_task (
    id                  BIGSERIAL    PRIMARY KEY,
    task_no             VARCHAR(32)  UNIQUE,
    patient_id          BIGINT       NOT NULL REFERENCES patient_profile(id),
    status              VARCHAR(20)  NOT NULL,
    source              VARCHAR(32)  NOT NULL,
    remark              VARCHAR(500),
    ai_analysis_summary TEXT,
    poster_url          VARCHAR(1024),
    close_reason        VARCHAR(256),
    event_version       BIGINT       NOT NULL DEFAULT 0,
    created_by          BIGINT       NOT NULL REFERENCES sys_user(id),
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    closed_at           TIMESTAMPTZ,
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_task_status CHECK (status IN ('ACTIVE','RESOLVED','FALSE_ALARM')),
    CONSTRAINT chk_task_source CHECK (source IN ('APP','MINI_PROGRAM','ADMIN_PORTAL')),
    CONSTRAINT chk_task_closed CHECK (
        (status = 'ACTIVE' AND closed_at IS NULL)
        OR (status IN ('RESOLVED','FALSE_ALARM') AND closed_at IS NOT NULL)
    )
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_task_active_per_patient ON rescue_task(patient_id) WHERE status = 'ACTIVE';
CREATE INDEX        IF NOT EXISTS idx_rescue_task_patient     ON rescue_task(patient_id, created_at DESC);
CREATE INDEX        IF NOT EXISTS idx_rescue_task_status      ON rescue_task(status, created_at DESC);

-- ============================================================
-- 6. clue_record（线索记录）
-- ============================================================
CREATE TABLE IF NOT EXISTS clue_record (
    id                BIGSERIAL     PRIMARY KEY,
    clue_no           VARCHAR(32)   UNIQUE,
    patient_id        BIGINT        NOT NULL REFERENCES patient_profile(id),
    task_id           BIGINT        REFERENCES rescue_task(id),
    tag_code          VARCHAR(100)  NOT NULL,
    source_type       VARCHAR(20)   NOT NULL,
    risk_score        NUMERIC(5,4),
    location_lat      DOUBLE PRECISION NOT NULL,
    location_lng      DOUBLE PRECISION NOT NULL,
    coord_system      VARCHAR(10)   NOT NULL DEFAULT 'WGS84',
    description       TEXT,
    photo_url         VARCHAR(1024),
    is_valid          BOOLEAN       NOT NULL,
    suspect_flag      BOOLEAN       NOT NULL,
    suspect_reason    VARCHAR(256),
    review_status     VARCHAR(20),
    assignee_user_id  BIGINT        REFERENCES sys_user(id),
    assigned_at       TIMESTAMPTZ,
    reviewed_at       TIMESTAMPTZ,
    override          BOOLEAN       NOT NULL DEFAULT FALSE,
    override_by       BIGINT        REFERENCES sys_user(id),
    override_reason   VARCHAR(256),
    reject_reason     VARCHAR(256),
    rejected_by       BIGINT        REFERENCES sys_user(id),
    created_at        TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_clue_source_type   CHECK (source_type IN ('SCAN','MANUAL')),
    CONSTRAINT chk_clue_coord_system  CHECK (coord_system = 'WGS84'),
    CONSTRAINT chk_clue_review_status CHECK (review_status IS NULL OR review_status IN ('PENDING','OVERRIDDEN','REJECTED')),
    CONSTRAINT chk_clue_risk_score    CHECK (risk_score IS NULL OR (risk_score >= 0 AND risk_score <= 1)),
    CONSTRAINT chk_clue_suspect_review CHECK (
        (suspect_flag = FALSE AND review_status IS NULL)
        OR (suspect_flag = TRUE AND review_status IN ('PENDING','OVERRIDDEN','REJECTED'))
    )
);

CREATE INDEX IF NOT EXISTS idx_clue_patient_created  ON clue_record(patient_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_clue_task_created     ON clue_record(task_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_clue_suspected        ON clue_record(suspect_flag, is_valid);
CREATE INDEX IF NOT EXISTS idx_clue_review_pending   ON clue_record(review_status, created_at DESC) WHERE review_status = 'PENDING';
CREATE INDEX IF NOT EXISTS idx_clue_assignee_pending ON clue_record(assignee_user_id, created_at DESC) WHERE review_status = 'PENDING';

-- ============================================================
-- 7. patient_trajectory（轨迹聚合）
-- ============================================================
CREATE TABLE IF NOT EXISTS patient_trajectory (
    id              BIGSERIAL    PRIMARY KEY,
    patient_id      BIGINT       NOT NULL REFERENCES patient_profile(id),
    task_id         BIGINT       REFERENCES rescue_task(id),
    window_start    TIMESTAMPTZ  NOT NULL,
    window_end      TIMESTAMPTZ  NOT NULL,
    point_count     INT          NOT NULL,
    geometry_type   VARCHAR(32)  NOT NULL,
    geometry_data   JSONB,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_traj_geom_type CHECK (geometry_type IN ('LINESTRING','SPARSE_POINT','EMPTY_WINDOW'))
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_traj_patient_window ON patient_trajectory(patient_id, window_start, window_end);
CREATE INDEX        IF NOT EXISTS idx_traj_patient_window ON patient_trajectory(patient_id, window_start DESC);

-- ============================================================
-- 8. tag_asset（标签资产）
-- ============================================================
CREATE TABLE IF NOT EXISTS tag_asset (
    id              BIGSERIAL    PRIMARY KEY,
    tag_code        VARCHAR(100) NOT NULL,
    tag_type        VARCHAR(20)  NOT NULL,
    status          VARCHAR(20)  NOT NULL,
    patient_id      BIGINT       REFERENCES patient_profile(id),
    apply_record_id BIGINT,
    import_batch_no VARCHAR(64),
    void_reason     VARCHAR(256),
    lost_at         TIMESTAMPTZ,
    void_at         TIMESTAMPTZ,
    reset_at        TIMESTAMPTZ,
    recovered_at    TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_tag_status   CHECK (status IN ('UNBOUND','ALLOCATED','BOUND','LOST','VOID')),
    CONSTRAINT chk_tag_type     CHECK (tag_type IN ('QR_CODE','NFC')),
    CONSTRAINT chk_tag_patient  CHECK ((status NOT IN ('BOUND','LOST')) OR patient_id IS NOT NULL),
    CONSTRAINT chk_tag_lost_at  CHECK ((status <> 'LOST') OR lost_at IS NOT NULL),
    CONSTRAINT chk_tag_void     CHECK ((status <> 'VOID') OR (void_at IS NOT NULL AND void_reason IS NOT NULL))
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_tag_asset_code  ON tag_asset(tag_code);
CREATE INDEX        IF NOT EXISTS idx_tag_status      ON tag_asset(status);
CREATE INDEX        IF NOT EXISTS idx_tag_patient     ON tag_asset(patient_id, status);
CREATE INDEX        IF NOT EXISTS idx_tag_batch       ON tag_asset(import_batch_no);

-- ============================================================
-- 9. tag_apply_record（物资申领工单）
-- ============================================================
CREATE TABLE IF NOT EXISTS tag_apply_record (
    id                   BIGSERIAL    PRIMARY KEY,
    order_no             VARCHAR(32)  UNIQUE,
    patient_id           BIGINT       NOT NULL REFERENCES patient_profile(id),
    applicant_user_id    BIGINT       NOT NULL REFERENCES sys_user(id),
    quantity             INT          NOT NULL,
    apply_note           VARCHAR(256),
    tag_code             VARCHAR(100),
    status               VARCHAR(32)  NOT NULL,
    delivery_address     VARCHAR(512) NOT NULL,
    tracking_number      VARCHAR(64),
    courier_name         VARCHAR(64),
    resource_link        VARCHAR(1024),
    cancel_reason        VARCHAR(256),
    approved_at          TIMESTAMPTZ,
    reject_reason        VARCHAR(256),
    rejected_at          TIMESTAMPTZ,
    exception_desc       VARCHAR(512),
    closed_at            TIMESTAMPTZ,
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_apply_status CHECK (status IN ('PENDING','PROCESSING','CANCEL_PENDING','CANCELLED','SHIPPED','EXCEPTION','COMPLETED'))
);

CREATE INDEX IF NOT EXISTS idx_apply_patient_status ON tag_apply_record(patient_id, status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_apply_applicant       ON tag_apply_record(applicant_user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_apply_status          ON tag_apply_record(status, created_at DESC);

-- tag_asset.apply_record_id FK (forward reference resolved)
ALTER TABLE tag_asset ADD CONSTRAINT fk_tag_apply_record FOREIGN KEY (apply_record_id) REFERENCES tag_apply_record(id);

-- ============================================================
-- 10. ai_session（AI会话审计）
-- ============================================================
CREATE TABLE IF NOT EXISTS ai_session (
    id               BIGSERIAL    PRIMARY KEY,
    session_id       VARCHAR(64)  NOT NULL,
    user_id          BIGINT       NOT NULL REFERENCES sys_user(id),
    patient_id       BIGINT       NOT NULL REFERENCES patient_profile(id),
    task_id          BIGINT       REFERENCES rescue_task(id),
    messages         JSONB        NOT NULL DEFAULT '[]',
    request_tokens   INT          NOT NULL DEFAULT 0,
    response_tokens  INT          NOT NULL DEFAULT 0,
    token_usage      JSONB        NOT NULL DEFAULT '{}',
    token_used       INT          NOT NULL DEFAULT 0,
    model_name       VARCHAR(64)  NOT NULL,
    status           VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    archived_at      TIMESTAMPTZ,
    version          BIGINT       NOT NULL DEFAULT 0,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_ai_session_status CHECK (status IN ('ACTIVE','ARCHIVED')),
    CONSTRAINT chk_ai_session_archived CHECK (
        (status = 'ACTIVE' AND archived_at IS NULL)
        OR (status = 'ARCHIVED' AND archived_at IS NOT NULL)
    )
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_ai_session_id        ON ai_session(session_id);
CREATE INDEX        IF NOT EXISTS idx_ai_session_user      ON ai_session(user_id, created_at DESC);
CREATE INDEX        IF NOT EXISTS idx_ai_session_patient   ON ai_session(patient_id, created_at DESC);

-- ============================================================
-- 11. ai_session_message（长会话消息明细，可选）
-- ============================================================
CREATE TABLE IF NOT EXISTS ai_session_message (
    id          BIGSERIAL   PRIMARY KEY,
    session_id  VARCHAR(64) NOT NULL REFERENCES ai_session(session_id),
    sequence_no INT         NOT NULL,
    role        VARCHAR(16) NOT NULL,
    content     TEXT        NOT NULL,
    token_usage JSONB,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_ai_msg_seq UNIQUE (session_id, sequence_no)
);

CREATE INDEX IF NOT EXISTS idx_ai_msg_session_seq ON ai_session_message(session_id, sequence_no);

-- ============================================================
-- 12. patient_memory_note（患者记忆原始条目）
-- ============================================================
CREATE TABLE IF NOT EXISTS patient_memory_note (
    id               BIGSERIAL   PRIMARY KEY,
    note_id          VARCHAR(64) NOT NULL,
    patient_id       BIGINT      NOT NULL REFERENCES patient_profile(id),
    created_by       BIGINT      NOT NULL REFERENCES sys_user(id),
    kind             VARCHAR(20) NOT NULL,
    content          TEXT        NOT NULL,
    tags             JSONB,
    source_event_id  VARCHAR(64),
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_memory_kind CHECK (kind IN ('HABIT','PLACE','PREFERENCE','SAFETY_CUE'))
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_memory_note_id      ON patient_memory_note(note_id);
CREATE INDEX        IF NOT EXISTS idx_memory_patient      ON patient_memory_note(patient_id, created_at DESC);
CREATE INDEX        IF NOT EXISTS idx_memory_patient_kind ON patient_memory_note(patient_id, kind, created_at DESC);

-- ============================================================
-- 13. vector_store（向量记忆库 - 用TEXT替代vector类型，需pgvector时替换）
-- ============================================================
CREATE TABLE IF NOT EXISTS vector_store (
    id              BIGSERIAL    PRIMARY KEY,
    patient_id      BIGINT       NOT NULL REFERENCES patient_profile(id),
    source_type     VARCHAR(32)  NOT NULL,
    source_id       VARCHAR(64)  NOT NULL,
    source_version  BIGINT       NOT NULL,
    content         TEXT         NOT NULL,
    valid           BOOLEAN      NOT NULL DEFAULT TRUE,
    superseded_at   TIMESTAMPTZ,
    deleted_at      TIMESTAMPTZ,
    expired_at      TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_vector_patient ON vector_store(patient_id, valid, created_at DESC);

-- ============================================================
-- 14. notification_inbox（站内通知中心）
-- ============================================================
CREATE TABLE IF NOT EXISTS notification_inbox (
    notification_id   BIGSERIAL    PRIMARY KEY,
    user_id           BIGINT       NOT NULL REFERENCES sys_user(id),
    type              VARCHAR(32)  NOT NULL,
    title             VARCHAR(128) NOT NULL,
    content           TEXT         NOT NULL,
    level             VARCHAR(16)  NOT NULL,
    related_task_id   BIGINT       REFERENCES rescue_task(id),
    related_patient_id BIGINT      REFERENCES patient_profile(id),
    read_status       VARCHAR(16)  NOT NULL DEFAULT 'UNREAD',
    read_at           TIMESTAMPTZ,
    trace_id          VARCHAR(64)  NOT NULL,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_noti_type        CHECK (type IN ('TASK_PROGRESS','FENCE_ALERT','TASK_CLOSED','SYSTEM')),
    CONSTRAINT chk_noti_level       CHECK (level IN ('INFO','WARN','CRITICAL')),
    CONSTRAINT chk_noti_read_status CHECK (read_status IN ('UNREAD','READ')),
    CONSTRAINT chk_noti_read_at     CHECK (
        (read_status = 'UNREAD' AND read_at IS NULL)
        OR (read_status = 'READ' AND read_at IS NOT NULL)
    )
);

CREATE INDEX IF NOT EXISTS idx_noti_user_created  ON notification_inbox(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_noti_user_read     ON notification_inbox(user_id, read_status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_noti_user_type     ON notification_inbox(user_id, type, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_noti_related_task  ON notification_inbox(related_task_id);
CREATE INDEX IF NOT EXISTS idx_noti_related_pat   ON notification_inbox(related_patient_id);

-- ============================================================
-- 15. sys_log（治理审计日志）
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_log (
    id                 BIGSERIAL    PRIMARY KEY,
    module             VARCHAR(64)  NOT NULL,
    action             VARCHAR(64)  NOT NULL,
    action_id          VARCHAR(64),
    result_code        VARCHAR(64),
    executed_at        TIMESTAMPTZ,
    operator_user_id   BIGINT       REFERENCES sys_user(id),
    operator_username  VARCHAR(64)  NOT NULL,
    object_id          VARCHAR(64),
    result             VARCHAR(20)  NOT NULL,
    risk_level         VARCHAR(20),
    detail             JSONB,
    action_source      VARCHAR(20)  NOT NULL DEFAULT 'USER',
    agent_profile      VARCHAR(64),
    execution_mode     VARCHAR(20),
    confirm_level      VARCHAR(20),
    blocked_reason     VARCHAR(128),
    request_id         VARCHAR(64),
    trace_id           VARCHAR(64)  NOT NULL,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_log_result        CHECK (result IN ('SUCCESS','FAIL')),
    CONSTRAINT chk_log_action_source CHECK (action_source IN ('USER','AI_AGENT'))
);

CREATE INDEX IF NOT EXISTS idx_log_module_action ON sys_log(module, action, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_log_operator_time ON sys_log(operator_user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_log_trace         ON sys_log(trace_id);
CREATE INDEX IF NOT EXISTS idx_log_created_at    ON sys_log(created_at DESC);

-- ============================================================
-- 16. sys_config（治理配置）
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_config (
    config_key      VARCHAR(128) PRIMARY KEY,
    config_value    TEXT         NOT NULL,
    scope           VARCHAR(32)  NOT NULL DEFAULT 'public',
    updated_by      BIGINT       NOT NULL REFERENCES sys_user(id),
    updated_reason  VARCHAR(256) NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_config_scope CHECK (scope IN ('public','ops','security','ai_policy'))
);

CREATE INDEX IF NOT EXISTS idx_sys_config_scope ON sys_config(scope);

-- ============================================================
-- 17. sys_outbox_log（Outbox调度）
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_outbox_log (
    event_id              VARCHAR(64)  NOT NULL,
    topic                 VARCHAR(128) NOT NULL,
    aggregate_id          VARCHAR(64)  NOT NULL,
    partition_key         VARCHAR(64)  NOT NULL,
    payload               JSONB        NOT NULL,
    request_id            VARCHAR(64)  NOT NULL,
    trace_id              VARCHAR(64)  NOT NULL,
    phase                 VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    retry_count           INT          NOT NULL DEFAULT 0,
    next_retry_at         TIMESTAMPTZ,
    lease_owner           VARCHAR(64),
    lease_until           TIMESTAMPTZ,
    sent_at               TIMESTAMPTZ,
    last_error            VARCHAR(512),
    last_intervention_by  BIGINT       REFERENCES sys_user(id),
    last_intervention_at  TIMESTAMPTZ,
    replay_reason         VARCHAR(256),
    replay_token          VARCHAR(64),
    replayed_at           TIMESTAMPTZ,
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_outbox       PRIMARY KEY (event_id, created_at),
    CONSTRAINT chk_outbox_phase CHECK (phase IN ('PENDING','DISPATCHING','SENT','RETRY','DEAD'))
);

CREATE INDEX IF NOT EXISTS idx_outbox_phase_retry     ON sys_outbox_log(phase, next_retry_at, created_at);
CREATE INDEX IF NOT EXISTS idx_outbox_partition_phase ON sys_outbox_log(partition_key, phase, created_at);
CREATE INDEX IF NOT EXISTS idx_outbox_dead            ON sys_outbox_log(phase, updated_at DESC) WHERE phase = 'DEAD';

-- ============================================================
-- 18. consumed_event_log（消费幂等日志）
-- ============================================================
CREATE TABLE IF NOT EXISTS consumed_event_log (
    id             BIGSERIAL    NOT NULL,
    consumer_name  VARCHAR(64)  NOT NULL,
    topic          VARCHAR(128) NOT NULL,
    event_id       VARCHAR(64)  NOT NULL,
    partition      INT          NOT NULL,
    offset         BIGINT       NOT NULL,
    processed_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    trace_id       VARCHAR(64)  NOT NULL,
    CONSTRAINT pk_consumed_event PRIMARY KEY (id, processed_at)
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_consumer_event    ON consumed_event_log(consumer_name, topic, event_id, processed_at);
CREATE INDEX        IF NOT EXISTS idx_consumed_proc_at  ON consumed_event_log(processed_at);
