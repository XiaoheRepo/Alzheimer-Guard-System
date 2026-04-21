-- =====================================================================
-- V1__init_schema.sql
-- Alzheimer Guard System - Full Schema (aligned with LLD V2.0 baseline)
-- PostgreSQL 16
-- NOTE: PostGIS / pgvector are optional at this stage. We store
--       geo fields as DOUBLE PRECISION (lat/lng) and embeddings as TEXT
--       so the schema compiles on a vanilla PostgreSQL instance.
--       Deploy-ready variants with PostGIS(GEOMETRY) / pgvector(vector)
--       can be introduced via later migrations.
-- =====================================================================

-- ============================================================
-- 1. sys_user
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_user (
    id                BIGSERIAL    PRIMARY KEY,
    username          VARCHAR(64)  NOT NULL,
    email             VARCHAR(128) NOT NULL,
    email_verified    BOOLEAN      NOT NULL DEFAULT FALSE,
    password_hash     VARCHAR(128) NOT NULL,
    nickname          VARCHAR(64),
    avatar_url        VARCHAR(1024),
    phone             VARCHAR(32),
    role              VARCHAR(32)  NOT NULL DEFAULT 'FAMILY',
    status            VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    last_login_at     TIMESTAMPTZ,
    last_login_ip     VARCHAR(64),
    version           BIGINT       NOT NULL DEFAULT 0,
    trace_id          VARCHAR(64),
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_sys_user_role   CHECK (role   IN ('FAMILY','ADMIN','SUPER_ADMIN')),
    CONSTRAINT chk_sys_user_status CHECK (status IN ('ACTIVE','DISABLED','DEACTIVATED'))
);
CREATE UNIQUE INDEX IF NOT EXISTS ux_sys_user_username ON sys_user(username);
CREATE UNIQUE INDEX IF NOT EXISTS ux_sys_user_email    ON sys_user(email);
CREATE INDEX        IF NOT EXISTS idx_sys_user_role    ON sys_user(role, status);

-- ============================================================
-- 2. patient_profile
-- ============================================================
CREATE SEQUENCE IF NOT EXISTS patient_short_code_seq START 1 INCREMENT 1;

CREATE TABLE IF NOT EXISTS patient_profile (
    id                      BIGSERIAL    PRIMARY KEY,
    profile_no              VARCHAR(32)  UNIQUE,
    name                    VARCHAR(64)  NOT NULL,
    gender                  VARCHAR(16)  NOT NULL,
    birthday                DATE         NOT NULL,
    short_code              CHAR(6)      NOT NULL,
    id_card_hash            VARCHAR(128),
    chronic_diseases        VARCHAR(500),
    medication              VARCHAR(500),
    allergy                 VARCHAR(500),
    emergency_contact_phone VARCHAR(32),
    avatar_url              VARCHAR(1024) NOT NULL,
    long_text_profile       TEXT,
    appearance_height_cm    INT,
    appearance_weight_kg    INT,
    appearance_clothing     VARCHAR(500),
    appearance_features     VARCHAR(500),
    fence_enabled           BOOLEAN      NOT NULL DEFAULT FALSE,
    fence_center_lat        DOUBLE PRECISION,
    fence_center_lng        DOUBLE PRECISION,
    fence_radius_m          INT,
    fence_coord_system      VARCHAR(10)  DEFAULT 'WGS84',
    lost_status             VARCHAR(20)  NOT NULL DEFAULT 'NORMAL',
    lost_status_event_time  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    profile_version         BIGINT       NOT NULL DEFAULT 0,
    deleted_at              TIMESTAMPTZ,
    trace_id                VARCHAR(64),
    created_at              TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_patient_lost_status CHECK (lost_status IN ('NORMAL','MISSING_PENDING','MISSING')),
    CONSTRAINT chk_patient_gender      CHECK (gender IN ('MALE','FEMALE','UNKNOWN')),
    CONSTRAINT chk_fence_logic CHECK (
        (fence_enabled = FALSE)
        OR
        (fence_enabled = TRUE AND fence_center_lat IS NOT NULL AND fence_center_lng IS NOT NULL
            AND fence_radius_m BETWEEN 100 AND 50000)
    )
);
CREATE UNIQUE INDEX IF NOT EXISTS ux_patient_profile_no ON patient_profile(profile_no) WHERE profile_no IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS ux_patient_short_code ON patient_profile(short_code);
CREATE INDEX        IF NOT EXISTS idx_patient_lost      ON patient_profile(lost_status);
CREATE INDEX        IF NOT EXISTS idx_patient_deleted   ON patient_profile(deleted_at);

-- ============================================================
-- 3. guardian_relation (监护关系)
-- ============================================================
CREATE TABLE IF NOT EXISTS guardian_relation (
    id               BIGSERIAL    PRIMARY KEY,
    user_id          BIGINT       NOT NULL REFERENCES sys_user(id),
    patient_id       BIGINT       NOT NULL REFERENCES patient_profile(id),
    relation_role    VARCHAR(32)  NOT NULL,
    relation_status  VARCHAR(20)  NOT NULL,
    joined_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    revoked_at       TIMESTAMPTZ,
    version          BIGINT       NOT NULL DEFAULT 0,
    trace_id         VARCHAR(64),
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_gr_role   CHECK (relation_role   IN ('PRIMARY_GUARDIAN','GUARDIAN')),
    CONSTRAINT chk_gr_status CHECK (relation_status IN ('PENDING','ACTIVE','REVOKED'))
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_gr_active_user_patient ON guardian_relation(user_id, patient_id) WHERE relation_status = 'ACTIVE';
CREATE UNIQUE INDEX IF NOT EXISTS uq_gr_primary_per_patient ON guardian_relation(patient_id) WHERE relation_role = 'PRIMARY_GUARDIAN' AND relation_status = 'ACTIVE';
CREATE INDEX        IF NOT EXISTS idx_gr_user              ON guardian_relation(user_id, relation_status);
CREATE INDEX        IF NOT EXISTS idx_gr_patient           ON guardian_relation(patient_id, relation_status);

-- ============================================================
-- 4. guardian_invitation (监护邀请)
-- ============================================================
CREATE TABLE IF NOT EXISTS guardian_invitation (
    id               BIGSERIAL    PRIMARY KEY,
    invite_id        VARCHAR(64)  NOT NULL,
    patient_id       BIGINT       NOT NULL REFERENCES patient_profile(id),
    inviter_user_id  BIGINT       NOT NULL REFERENCES sys_user(id),
    invitee_user_id  BIGINT       NOT NULL REFERENCES sys_user(id),
    relation_role    VARCHAR(32)  NOT NULL DEFAULT 'GUARDIAN',
    status           VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    reason           VARCHAR(500),
    reject_reason    VARCHAR(256),
    expire_at        TIMESTAMPTZ  NOT NULL,
    responded_at     TIMESTAMPTZ,
    revoked_at       TIMESTAMPTZ,
    version          BIGINT       NOT NULL DEFAULT 0,
    trace_id         VARCHAR(64),
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_inv_status CHECK (status IN ('PENDING','ACCEPTED','REJECTED','EXPIRED','REVOKED'))
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_inv_invite_id       ON guardian_invitation(invite_id);
CREATE UNIQUE INDEX IF NOT EXISTS uq_inv_pending_per_pair ON guardian_invitation(patient_id, invitee_user_id) WHERE status = 'PENDING';
CREATE INDEX        IF NOT EXISTS idx_inv_invitee        ON guardian_invitation(invitee_user_id, status);
CREATE INDEX        IF NOT EXISTS idx_inv_patient        ON guardian_invitation(patient_id, status);

-- ============================================================
-- 5. guardian_transfer_request (主监护权转移)
-- ============================================================
CREATE TABLE IF NOT EXISTS guardian_transfer_request (
    id                  BIGSERIAL    PRIMARY KEY,
    request_id          VARCHAR(64)  NOT NULL,
    patient_id          BIGINT       NOT NULL REFERENCES patient_profile(id),
    from_user_id        BIGINT       NOT NULL REFERENCES sys_user(id),
    to_user_id          BIGINT       NOT NULL REFERENCES sys_user(id),
    status              VARCHAR(32)  NOT NULL DEFAULT 'PENDING_CONFIRM',
    reason              VARCHAR(500),
    reject_reason       VARCHAR(256),
    cancel_reason       VARCHAR(256),
    expire_at           TIMESTAMPTZ  NOT NULL,
    confirmed_at        TIMESTAMPTZ,
    rejected_at         TIMESTAMPTZ,
    revoked_at          TIMESTAMPTZ,
    version             BIGINT       NOT NULL DEFAULT 0,
    trace_id            VARCHAR(64),
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_gt_status CHECK (status IN ('PENDING_CONFIRM','COMPLETED','REJECTED','REVOKED','EXPIRED'))
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_gt_request_id      ON guardian_transfer_request(request_id);
CREATE UNIQUE INDEX IF NOT EXISTS uq_gt_pending_patient ON guardian_transfer_request(patient_id) WHERE status = 'PENDING_CONFIRM';
CREATE INDEX        IF NOT EXISTS idx_gt_from           ON guardian_transfer_request(from_user_id, status);
CREATE INDEX        IF NOT EXISTS idx_gt_to             ON guardian_transfer_request(to_user_id, status);

-- ============================================================
-- 6. rescue_task
-- ============================================================
CREATE TABLE IF NOT EXISTS rescue_task (
    id                    BIGSERIAL    PRIMARY KEY,
    task_no               VARCHAR(32)  UNIQUE,
    patient_id            BIGINT       NOT NULL REFERENCES patient_profile(id),
    status                VARCHAR(32)  NOT NULL,
    source                VARCHAR(32)  NOT NULL,
    remark                VARCHAR(500),
    daily_appearance      TEXT,
    daily_photo_url       VARCHAR(1024),
    ai_analysis_summary   TEXT,
    poster_url            VARCHAR(1024),
    close_type            VARCHAR(20),
    close_reason          VARCHAR(256),
    found_location_lat    DOUBLE PRECISION,
    found_location_lng    DOUBLE PRECISION,
    event_version         BIGINT       NOT NULL DEFAULT 0,
    created_by            BIGINT       NOT NULL REFERENCES sys_user(id),
    closed_by             BIGINT       REFERENCES sys_user(id),
    closed_at             TIMESTAMPTZ,
    sustained_at          TIMESTAMPTZ,
    version               BIGINT       NOT NULL DEFAULT 0,
    trace_id              VARCHAR(64),
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_task_status     CHECK (status IN ('CREATED','ACTIVE','SUSTAINED','CLOSED_FOUND','CLOSED_FALSE_ALARM')),
    CONSTRAINT chk_task_source     CHECK (source IN ('APP','MINI_PROGRAM','ADMIN_PORTAL','AUTO_UPGRADE')),
    CONSTRAINT chk_task_close_type CHECK (close_type IS NULL OR close_type IN ('FOUND','FALSE_ALARM'))
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_task_active_per_patient
    ON rescue_task(patient_id)
    WHERE status NOT IN ('CLOSED_FOUND','CLOSED_FALSE_ALARM');
CREATE INDEX IF NOT EXISTS idx_task_patient ON rescue_task(patient_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_task_status  ON rescue_task(status, created_at DESC);

-- ============================================================
-- 7. clue_record
-- ============================================================
CREATE TABLE IF NOT EXISTS clue_record (
    id                  BIGSERIAL     PRIMARY KEY,
    clue_no             VARCHAR(32)   UNIQUE,
    patient_id          BIGINT        NOT NULL REFERENCES patient_profile(id),
    task_id             BIGINT        REFERENCES rescue_task(id),
    tag_code            VARCHAR(100),
    source_type         VARCHAR(20)   NOT NULL,
    reporter_user_id    BIGINT        REFERENCES sys_user(id),
    reporter_type       VARCHAR(20)   NOT NULL,
    latitude            DOUBLE PRECISION NOT NULL,
    longitude           DOUBLE PRECISION NOT NULL,
    coord_system        VARCHAR(10)   NOT NULL DEFAULT 'WGS84',
    description         TEXT,
    photo_urls          JSONB         NOT NULL DEFAULT '[]',
    tag_only            BOOLEAN       NOT NULL DEFAULT FALSE,
    risk_score          NUMERIC(5,4),
    status              VARCHAR(20)   NOT NULL DEFAULT 'VALID',
    suspect_flag        BOOLEAN       NOT NULL DEFAULT FALSE,
    suspect_reason      VARCHAR(256),
    drift_flag          BOOLEAN       NOT NULL DEFAULT FALSE,
    review_status       VARCHAR(20),
    override_reason     VARCHAR(256),
    reject_reason       VARCHAR(256),
    reviewed_by         BIGINT        REFERENCES sys_user(id),
    reviewed_at         TIMESTAMPTZ,
    device_fingerprint  VARCHAR(128),
    entry_token_jti     VARCHAR(64),
    client_ip           VARCHAR(64),
    version             BIGINT        NOT NULL DEFAULT 0,
    trace_id            VARCHAR(64),
    created_at          TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_clue_source_type   CHECK (source_type IN ('SCAN','MANUAL','POSTER_SCAN')),
    CONSTRAINT chk_clue_reporter_type CHECK (reporter_type IN ('FAMILY','ANONYMOUS','ADMIN')),
    CONSTRAINT chk_clue_status        CHECK (status IN ('VALID','OVERRIDDEN','REJECTED','INVALID')),
    CONSTRAINT chk_clue_coord         CHECK (coord_system IN ('WGS84','GCJ-02','BD-09')),
    CONSTRAINT chk_clue_review_status CHECK (review_status IS NULL OR review_status IN ('PENDING','OVERRIDDEN','REJECTED')),
    CONSTRAINT chk_clue_risk_score    CHECK (risk_score IS NULL OR (risk_score >= 0 AND risk_score <= 1)),
    CONSTRAINT chk_clue_suspect_review CHECK (
        (suspect_flag = FALSE AND review_status IS NULL)
        OR (suspect_flag = TRUE AND review_status IS NOT NULL)
    )
);
CREATE INDEX IF NOT EXISTS idx_clue_patient  ON clue_record(patient_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_clue_task     ON clue_record(task_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_clue_suspect  ON clue_record(suspect_flag, review_status);
CREATE INDEX IF NOT EXISTS idx_clue_review   ON clue_record(review_status, created_at DESC) WHERE review_status = 'PENDING';

-- ============================================================
-- 8. patient_trajectory
-- ============================================================
CREATE TABLE IF NOT EXISTS patient_trajectory (
    id              BIGSERIAL    PRIMARY KEY,
    patient_id      BIGINT       NOT NULL REFERENCES patient_profile(id),
    task_id         BIGINT       REFERENCES rescue_task(id),
    clue_id         BIGINT       REFERENCES clue_record(id),
    window_start    TIMESTAMPTZ  NOT NULL,
    window_end      TIMESTAMPTZ  NOT NULL,
    point_count     INT          NOT NULL,
    geometry_type   VARCHAR(32)  NOT NULL,
    geometry_data   JSONB,
    latitude        DOUBLE PRECISION,
    longitude       DOUBLE PRECISION,
    source_type     VARCHAR(32),
    version         BIGINT       NOT NULL DEFAULT 0,
    trace_id        VARCHAR(64),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_traj_geom_type CHECK (geometry_type IN ('LINESTRING','SPARSE_POINT','EMPTY_WINDOW','POINT'))
);
CREATE INDEX IF NOT EXISTS idx_traj_patient_window ON patient_trajectory(patient_id, window_start DESC);
CREATE INDEX IF NOT EXISTS idx_traj_task            ON patient_trajectory(task_id, created_at DESC);

-- ============================================================
-- 9. tag_asset
-- ============================================================
CREATE TABLE IF NOT EXISTS tag_asset (
    id              BIGSERIAL    PRIMARY KEY,
    tag_code        VARCHAR(100) NOT NULL,
    tag_type        VARCHAR(20)  NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'UNBOUND',
    patient_id      BIGINT       REFERENCES patient_profile(id),
    short_code      CHAR(6),
    order_id        BIGINT,
    resource_token  VARCHAR(512),
    batch_no        VARCHAR(64),
    void_reason     VARCHAR(256),
    lost_reason     VARCHAR(256),
    lost_at         TIMESTAMPTZ,
    void_at         TIMESTAMPTZ,
    suspected_lost_at TIMESTAMPTZ,
    bound_at        TIMESTAMPTZ,
    allocated_at    TIMESTAMPTZ,
    version         BIGINT       NOT NULL DEFAULT 0,
    trace_id        VARCHAR(64),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_tag_status CHECK (status IN ('UNBOUND','ALLOCATED','BOUND','SUSPECTED_LOST','LOST','VOIDED'))
        ,
    CONSTRAINT chk_tag_type   CHECK (tag_type IN ('QR_CODE','NFC'))
);
CREATE UNIQUE INDEX IF NOT EXISTS ux_tag_code    ON tag_asset(tag_code);
CREATE INDEX        IF NOT EXISTS idx_tag_status ON tag_asset(status);
CREATE INDEX        IF NOT EXISTS idx_tag_patient ON tag_asset(patient_id, status);
CREATE INDEX        IF NOT EXISTS idx_tag_batch  ON tag_asset(batch_no);

-- ============================================================
-- 10. tag_apply_record (物资申领工单)
-- ============================================================
CREATE TABLE IF NOT EXISTS tag_apply_record (
    id                      BIGSERIAL    PRIMARY KEY,
    order_no                VARCHAR(32)  UNIQUE,
    patient_id              BIGINT       NOT NULL REFERENCES patient_profile(id),
    applicant_user_id       BIGINT       NOT NULL REFERENCES sys_user(id),
    tag_type                VARCHAR(20)  NOT NULL,
    quantity                INT          NOT NULL,
    remark                  VARCHAR(500),
    status                  VARCHAR(32)  NOT NULL DEFAULT 'PENDING_AUDIT',
    shipping_province       VARCHAR(64),
    shipping_city           VARCHAR(64),
    shipping_district       VARCHAR(64),
    shipping_detail         VARCHAR(512),
    shipping_receiver       VARCHAR(64),
    shipping_phone          VARCHAR(32),
    reviewer_user_id        BIGINT       REFERENCES sys_user(id),
    reviewed_at             TIMESTAMPTZ,
    reject_reason           VARCHAR(256),
    tag_codes               JSONB,
    logistics_no            VARCHAR(64),
    logistics_company       VARCHAR(64),
    shipped_at              TIMESTAMPTZ,
    received_at             TIMESTAMPTZ,
    cancel_reason           VARCHAR(256),
    cancelled_at            TIMESTAMPTZ,
    exception_desc          VARCHAR(512),
    version                 BIGINT       NOT NULL DEFAULT 0,
    trace_id                VARCHAR(64),
    created_at              TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_apply_status CHECK (status IN ('PENDING_AUDIT','PENDING_SHIP','SHIPPED','RECEIVED','EXCEPTION','REJECTED','CANCELLED','VOIDED')),
    CONSTRAINT chk_apply_tag_type CHECK (tag_type IN ('QR_CODE','NFC'))
);
CREATE INDEX IF NOT EXISTS idx_apply_patient  ON tag_apply_record(patient_id, status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_apply_applicant ON tag_apply_record(applicant_user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_apply_status    ON tag_apply_record(status, created_at DESC);

ALTER TABLE tag_asset
    ADD CONSTRAINT fk_tag_apply_record
    FOREIGN KEY (order_id) REFERENCES tag_apply_record(id);

-- ============================================================
-- 11. tag_batch_job (批量发号任务)
-- ============================================================
CREATE TABLE IF NOT EXISTS tag_batch_job (
    id              BIGSERIAL    PRIMARY KEY,
    job_id          VARCHAR(64)  NOT NULL,
    tag_type        VARCHAR(20)  NOT NULL,
    quantity        INT          NOT NULL,
    success_count   INT          NOT NULL DEFAULT 0,
    fail_count      INT          NOT NULL DEFAULT 0,
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    batch_key_id    VARCHAR(64),
    created_by      BIGINT       NOT NULL REFERENCES sys_user(id),
    completed_at    TIMESTAMPTZ,
    trace_id        VARCHAR(64),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_batch_status CHECK (status IN ('PENDING','RUNNING','COMPLETED','FAILED'))
);
CREATE UNIQUE INDEX IF NOT EXISTS ux_tag_batch_job_id ON tag_batch_job(job_id);

-- ============================================================
-- 12. ai_session
-- ============================================================
CREATE TABLE IF NOT EXISTS ai_session (
    id               BIGSERIAL    PRIMARY KEY,
    session_id       VARCHAR(64)  NOT NULL,
    user_id          BIGINT       NOT NULL REFERENCES sys_user(id),
    patient_id       BIGINT       NOT NULL REFERENCES patient_profile(id),
    task_id          BIGINT       REFERENCES rescue_task(id),
    messages         JSONB        NOT NULL DEFAULT '[]',
    prompt_tokens    INT          NOT NULL DEFAULT 0,
    completion_tokens INT         NOT NULL DEFAULT 0,
    total_tokens     INT          NOT NULL DEFAULT 0,
    token_usage      JSONB        NOT NULL DEFAULT '{}',
    model_name       VARCHAR(64)  NOT NULL DEFAULT 'qwen-max-latest',
    status           VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    feedback_rating  INT,
    feedback_comment VARCHAR(1000),
    feedback_at      TIMESTAMPTZ,
    archived_at      TIMESTAMPTZ,
    version          BIGINT       NOT NULL DEFAULT 0,
    trace_id         VARCHAR(64),
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_ai_status CHECK (status IN ('ACTIVE','ARCHIVED'))
);
CREATE UNIQUE INDEX IF NOT EXISTS ux_ai_session_id      ON ai_session(session_id);
CREATE UNIQUE INDEX IF NOT EXISTS uq_ai_session_active   ON ai_session(patient_id, task_id) WHERE status = 'ACTIVE';
CREATE INDEX        IF NOT EXISTS idx_ai_session_user    ON ai_session(user_id, created_at DESC);
CREATE INDEX        IF NOT EXISTS idx_ai_session_patient ON ai_session(patient_id, created_at DESC);

-- ============================================================
-- 13. ai_quota_ledger (AI 双账本配额)
-- ============================================================
CREATE TABLE IF NOT EXISTS ai_quota_ledger (
    id           BIGSERIAL    PRIMARY KEY,
    ledger_type  VARCHAR(16)  NOT NULL,
    owner_id     BIGINT       NOT NULL,
    period       VARCHAR(16)  NOT NULL,
    quota_limit  INT          NOT NULL,
    used         INT          NOT NULL DEFAULT 0,
    reserved     INT          NOT NULL DEFAULT 0,
    version      BIGINT       NOT NULL DEFAULT 0,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_quota_type CHECK (ledger_type IN ('USER','PATIENT'))
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_quota_key ON ai_quota_ledger(ledger_type, owner_id, period);

-- ============================================================
-- 14. patient_memory_note
-- ============================================================
CREATE TABLE IF NOT EXISTS patient_memory_note (
    id               BIGSERIAL   PRIMARY KEY,
    note_id          VARCHAR(64) NOT NULL,
    patient_id       BIGINT      NOT NULL REFERENCES patient_profile(id),
    created_by       BIGINT      NOT NULL REFERENCES sys_user(id),
    kind             VARCHAR(32) NOT NULL,
    content          TEXT        NOT NULL,
    tags             JSONB,
    source_event_id  VARCHAR(64),
    version          BIGINT      NOT NULL DEFAULT 0,
    trace_id         VARCHAR(64),
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_memory_kind CHECK (kind IN ('HABIT','PLACE','PREFERENCE','SAFETY_CUE','RESCUE_CASE'))
);
CREATE UNIQUE INDEX IF NOT EXISTS ux_memory_note_id ON patient_memory_note(note_id);
CREATE INDEX        IF NOT EXISTS idx_memory_patient ON patient_memory_note(patient_id, created_at DESC);

-- ============================================================
-- 15. vector_store (pgvector-ready, TEXT placeholder)
-- ============================================================
CREATE TABLE IF NOT EXISTS vector_store (
    id              BIGSERIAL    PRIMARY KEY,
    patient_id      BIGINT       NOT NULL REFERENCES patient_profile(id),
    source_type     VARCHAR(32)  NOT NULL,
    source_id       VARCHAR(64)  NOT NULL,
    source_version  BIGINT       NOT NULL DEFAULT 1,
    content         TEXT         NOT NULL,
    embedding       TEXT,
    valid           BOOLEAN      NOT NULL DEFAULT TRUE,
    superseded_at   TIMESTAMPTZ,
    deleted_at      TIMESTAMPTZ,
    expired_at      TIMESTAMPTZ,
    trace_id        VARCHAR(64),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_vector_patient ON vector_store(patient_id, valid, created_at DESC);

-- ============================================================
-- 16. ai_intent (Agent 意图待确认)
-- ============================================================
CREATE TABLE IF NOT EXISTS ai_intent (
    id               BIGSERIAL   PRIMARY KEY,
    intent_id        VARCHAR(64) NOT NULL,
    session_id       VARCHAR(64) NOT NULL,
    user_id          BIGINT      NOT NULL REFERENCES sys_user(id),
    action           VARCHAR(64) NOT NULL,
    description      VARCHAR(500),
    parameters       JSONB,
    execution_level  VARCHAR(20) NOT NULL DEFAULT 'CONFIRM_1',
    requires_confirm BOOLEAN     NOT NULL DEFAULT TRUE,
    status           VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    execution_result JSONB,
    expire_at        TIMESTAMPTZ NOT NULL,
    processed_at     TIMESTAMPTZ,
    version          BIGINT      NOT NULL DEFAULT 0,
    trace_id         VARCHAR(64),
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_intent_status CHECK (status IN ('PENDING','APPROVED','REJECTED','EXPIRED'))
);
CREATE UNIQUE INDEX IF NOT EXISTS ux_ai_intent_id ON ai_intent(intent_id);
CREATE INDEX        IF NOT EXISTS idx_ai_intent_session ON ai_intent(session_id, status);

-- ============================================================
-- 17. notification_inbox
-- ============================================================
CREATE TABLE IF NOT EXISTS notification_inbox (
    notification_id     BIGSERIAL    PRIMARY KEY,
    user_id             BIGINT       NOT NULL REFERENCES sys_user(id),
    type                VARCHAR(32)  NOT NULL,
    title               VARCHAR(128) NOT NULL,
    content             TEXT         NOT NULL,
    level               VARCHAR(16)  NOT NULL DEFAULT 'INFO',
    channel             VARCHAR(32)  NOT NULL DEFAULT 'INBOX',
    related_task_id     BIGINT       REFERENCES rescue_task(id),
    related_patient_id  BIGINT       REFERENCES patient_profile(id),
    related_object_id   VARCHAR(64),
    read_status         VARCHAR(16)  NOT NULL DEFAULT 'UNREAD',
    read_at             TIMESTAMPTZ,
    trace_id            VARCHAR(64)  NOT NULL,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_noti_type        CHECK (type IN ('TASK_PROGRESS','FENCE_ALERT','TASK_CLOSED','MISSING_PENDING_ALERT','TAG_SUSPECTED_LOST','TRANSFER_REQUEST','INVITATION','SYSTEM')),
    CONSTRAINT chk_noti_level       CHECK (level IN ('INFO','WARN','CRITICAL')),
    CONSTRAINT chk_noti_read_status CHECK (read_status IN ('UNREAD','READ'))
);
CREATE INDEX IF NOT EXISTS idx_noti_user          ON notification_inbox(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_noti_user_read     ON notification_inbox(user_id, read_status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_noti_user_type     ON notification_inbox(user_id, type, created_at DESC);

-- ============================================================
-- 18. sys_log
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_log (
    id                 BIGSERIAL    PRIMARY KEY,
    module             VARCHAR(64)  NOT NULL,
    action             VARCHAR(64)  NOT NULL,
    action_source      VARCHAR(20)  NOT NULL DEFAULT 'USER',
    operator_user_id   BIGINT       REFERENCES sys_user(id),
    operator_username  VARCHAR(64),
    object_id          VARCHAR(64),
    result             VARCHAR(20)  NOT NULL,
    result_code        VARCHAR(64),
    risk_level         VARCHAR(20),
    detail             JSONB,
    agent_profile      VARCHAR(64),
    execution_mode     VARCHAR(20),
    confirm_level      VARCHAR(20),
    blocked_reason     VARCHAR(128),
    client_ip          VARCHAR(64),
    request_id         VARCHAR(64),
    trace_id           VARCHAR(64)  NOT NULL,
    executed_at        TIMESTAMPTZ,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_log_result        CHECK (result IN ('SUCCESS','FAIL')),
    CONSTRAINT chk_log_action_source CHECK (action_source IN ('USER','AI_AGENT','SYSTEM'))
);
CREATE INDEX IF NOT EXISTS idx_log_module_action ON sys_log(module, action, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_log_operator      ON sys_log(operator_user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_log_trace         ON sys_log(trace_id);
CREATE INDEX IF NOT EXISTS idx_log_created       ON sys_log(created_at DESC);

-- ============================================================
-- 19. sys_config
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_config (
    config_key      VARCHAR(128) PRIMARY KEY,
    config_value    TEXT         NOT NULL,
    scope           VARCHAR(32)  NOT NULL DEFAULT 'public',
    description     VARCHAR(500),
    updated_by      BIGINT       REFERENCES sys_user(id),
    updated_reason  VARCHAR(256),
    version         BIGINT       NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_cfg_scope CHECK (scope IN ('public','ops','security','ai_policy'))
);
CREATE INDEX IF NOT EXISTS idx_sys_config_scope ON sys_config(scope);

-- ============================================================
-- 20. sys_outbox_log
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_outbox_log (
    id                    BIGSERIAL    PRIMARY KEY,
    event_id              VARCHAR(64)  NOT NULL,
    topic                 VARCHAR(128) NOT NULL,
    aggregate_id          VARCHAR(64)  NOT NULL,
    partition_key         VARCHAR(64)  NOT NULL,
    payload               JSONB        NOT NULL,
    request_id            VARCHAR(64),
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
    CONSTRAINT chk_outbox_phase CHECK (phase IN ('PENDING','DISPATCHING','SENT','RETRY','DEAD'))
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_outbox_event_id       ON sys_outbox_log(event_id);
CREATE INDEX        IF NOT EXISTS idx_outbox_phase_retry   ON sys_outbox_log(phase, next_retry_at, created_at);
CREATE INDEX        IF NOT EXISTS idx_outbox_partition     ON sys_outbox_log(partition_key, phase, created_at);
CREATE INDEX        IF NOT EXISTS idx_outbox_dead          ON sys_outbox_log(phase, updated_at DESC) WHERE phase = 'DEAD';

-- ============================================================
-- 21. consumed_event_log
-- ============================================================
CREATE TABLE IF NOT EXISTS consumed_event_log (
    id             BIGSERIAL    PRIMARY KEY,
    consumer_name  VARCHAR(64)  NOT NULL,
    topic          VARCHAR(128) NOT NULL,
    event_id       VARCHAR(64)  NOT NULL,
    partition_no   INT,
    msg_offset     BIGINT,
    trace_id       VARCHAR(64)  NOT NULL,
    processed_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_consumer_event ON consumed_event_log(consumer_name, topic, event_id);
CREATE INDEX        IF NOT EXISTS idx_consumed_proc ON consumed_event_log(processed_at);

-- ============================================================
-- 22. ws_ticket (一次性 WebSocket 票据)
-- ============================================================
CREATE TABLE IF NOT EXISTS ws_ticket (
    id          BIGSERIAL    PRIMARY KEY,
    ticket      VARCHAR(128) NOT NULL,
    user_id     BIGINT       NOT NULL REFERENCES sys_user(id),
    expire_at   TIMESTAMPTZ  NOT NULL,
    used_at     TIMESTAMPTZ,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE UNIQUE INDEX IF NOT EXISTS ux_ws_ticket ON ws_ticket(ticket);
