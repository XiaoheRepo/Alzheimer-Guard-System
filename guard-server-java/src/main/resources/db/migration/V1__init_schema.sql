-- =====================================================================
-- V1__init_schema.sql
-- Alzheimer Guard System - Full Schema (aligned with LLD V2.0 baseline)
-- PostgreSQL 16 (compatible with 15.3)
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
COMMENT ON TABLE sys_user IS '系统用户表，存储家庭成员、管理员、超级管理员等账户信息';
COMMENT ON COLUMN sys_user.id IS '用户唯一ID，自增主键';
COMMENT ON COLUMN sys_user.username IS '登录用户名，全局唯一';
COMMENT ON COLUMN sys_user.email IS '邮箱地址，全局唯一';
COMMENT ON COLUMN sys_user.email_verified IS '邮箱是否已验证';
COMMENT ON COLUMN sys_user.password_hash IS '密码哈希值';
COMMENT ON COLUMN sys_user.nickname IS '用户昵称';
COMMENT ON COLUMN sys_user.avatar_url IS '头像URL';
COMMENT ON COLUMN sys_user.phone IS '手机号码';
COMMENT ON COLUMN sys_user.role IS '角色：FAMILY(家属)、ADMIN(管理员)、SUPER_ADMIN(超级管理员)';
COMMENT ON COLUMN sys_user.status IS '账户状态：ACTIVE(正常)、DISABLED(禁用)、DEACTIVATED(注销)';
COMMENT ON COLUMN sys_user.last_login_at IS '最近一次登录时间';
COMMENT ON COLUMN sys_user.last_login_ip IS '最近一次登录IP';
COMMENT ON COLUMN sys_user.version IS '乐观锁版本号';
COMMENT ON COLUMN sys_user.trace_id IS '分布式追踪ID';
COMMENT ON COLUMN sys_user.created_at IS '记录创建时间';
COMMENT ON COLUMN sys_user.updated_at IS '记录更新时间';
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
COMMENT ON TABLE patient_profile IS '患者（老人）档案表';
COMMENT ON COLUMN patient_profile.id IS '患者唯一ID，自增主键';
COMMENT ON COLUMN patient_profile.profile_no IS '档案编号，可读性好的唯一标识';
COMMENT ON COLUMN patient_profile.name IS '患者姓名';
COMMENT ON COLUMN patient_profile.gender IS '性别：MALE、FEMALE、UNKNOWN';
COMMENT ON COLUMN patient_profile.birthday IS '出生日期';
COMMENT ON COLUMN patient_profile.short_code IS '6位短码，用于快速关联或扫码';
COMMENT ON COLUMN patient_profile.id_card_hash IS '身份证号哈希值，用于重复校验';
COMMENT ON COLUMN patient_profile.chronic_diseases IS '慢性病史描述';
COMMENT ON COLUMN patient_profile.medication IS '常用药物';
COMMENT ON COLUMN patient_profile.allergy IS '过敏史';
COMMENT ON COLUMN patient_profile.emergency_contact_phone IS '紧急联系电话';
COMMENT ON COLUMN patient_profile.avatar_url IS '患者头像URL';
COMMENT ON COLUMN patient_profile.long_text_profile IS '长文本档案，可包含病史、生活习惯等详细信息';
COMMENT ON COLUMN patient_profile.appearance_height_cm IS '身高(cm)';
COMMENT ON COLUMN patient_profile.appearance_weight_kg IS '体重(kg)';
COMMENT ON COLUMN patient_profile.appearance_clothing IS '常见衣着描述';
COMMENT ON COLUMN patient_profile.appearance_features IS '体貌特征描述';
COMMENT ON COLUMN patient_profile.fence_enabled IS '是否启用电子围栏';
COMMENT ON COLUMN patient_profile.fence_center_lat IS '围栏中心纬度';
COMMENT ON COLUMN patient_profile.fence_center_lng IS '围栏中心经度';
COMMENT ON COLUMN patient_profile.fence_radius_m IS '围栏半径(米)，范围100-50000';
COMMENT ON COLUMN patient_profile.fence_coord_system IS '坐标系统：WGS84、GCJ-02、BD-09';
COMMENT ON COLUMN patient_profile.lost_status IS '走失状态：NORMAL(正常)、MISSING_PENDING(疑似走失)、MISSING(确认走失)';
COMMENT ON COLUMN patient_profile.lost_status_event_time IS '最近一次走失状态变更时间';
COMMENT ON COLUMN patient_profile.profile_version IS '档案版本号，用于乐观锁';
COMMENT ON COLUMN patient_profile.deleted_at IS '软删除时间，非空表示已删除';
COMMENT ON COLUMN patient_profile.trace_id IS '分布式追踪ID';
COMMENT ON COLUMN patient_profile.created_at IS '记录创建时间';
COMMENT ON COLUMN patient_profile.updated_at IS '记录更新时间';
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
COMMENT ON TABLE guardian_relation IS '监护关系表，记录用户与患者之间的监护绑定';
COMMENT ON COLUMN guardian_relation.id IS '关系唯一ID，自增主键';
COMMENT ON COLUMN guardian_relation.user_id IS '用户ID，关联sys_user';
COMMENT ON COLUMN guardian_relation.patient_id IS '患者ID，关联patient_profile';
COMMENT ON COLUMN guardian_relation.relation_role IS '监护角色：PRIMARY_GUARDIAN(主监护人)、GUARDIAN(普通监护人)';
COMMENT ON COLUMN guardian_relation.relation_status IS '关系状态：PENDING(待确认)、ACTIVE(生效中)、REVOKED(已解除)';
COMMENT ON COLUMN guardian_relation.joined_at IS '加入时间';
COMMENT ON COLUMN guardian_relation.revoked_at IS '解除时间';
COMMENT ON COLUMN guardian_relation.version IS '乐观锁版本号';
COMMENT ON COLUMN guardian_relation.trace_id IS '分布式追踪ID';
COMMENT ON COLUMN guardian_relation.created_at IS '记录创建时间';
COMMENT ON COLUMN guardian_relation.updated_at IS '记录更新时间';
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
COMMENT ON TABLE guardian_invitation IS '监护邀请表，用于邀请其他用户成为患者监护人';
COMMENT ON COLUMN guardian_invitation.id IS '邀请记录ID，自增主键';
COMMENT ON COLUMN guardian_invitation.invite_id IS '邀请唯一ID，对外暴露';
COMMENT ON COLUMN guardian_invitation.patient_id IS '患者ID';
COMMENT ON COLUMN guardian_invitation.inviter_user_id IS '邀请人用户ID';
COMMENT ON COLUMN guardian_invitation.invitee_user_id IS '被邀请人用户ID';
COMMENT ON COLUMN guardian_invitation.relation_role IS '邀请的角色，默认为GUARDIAN';
COMMENT ON COLUMN guardian_invitation.status IS '状态：PENDING、ACCEPTED、REJECTED、EXPIRED、REVOKED';
COMMENT ON COLUMN guardian_invitation.reason IS '邀请理由';
COMMENT ON COLUMN guardian_invitation.reject_reason IS '拒绝理由';
COMMENT ON COLUMN guardian_invitation.expire_at IS '邀请过期时间';
COMMENT ON COLUMN guardian_invitation.responded_at IS '响应时间（接受或拒绝）';
COMMENT ON COLUMN guardian_invitation.revoked_at IS '邀请被撤销时间';
COMMENT ON COLUMN guardian_invitation.version IS '乐观锁版本号';
COMMENT ON COLUMN guardian_invitation.trace_id IS '分布式追踪ID';
COMMENT ON COLUMN guardian_invitation.created_at IS '记录创建时间';
COMMENT ON COLUMN guardian_invitation.updated_at IS '记录更新时间';
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
COMMENT ON TABLE guardian_transfer_request IS '主监护权转移申请表';
COMMENT ON COLUMN guardian_transfer_request.id IS '转移请求ID，自增主键';
COMMENT ON COLUMN guardian_transfer_request.request_id IS '请求唯一ID，对外暴露';
COMMENT ON COLUMN guardian_transfer_request.patient_id IS '患者ID';
COMMENT ON COLUMN guardian_transfer_request.from_user_id IS '当前主监护人用户ID';
COMMENT ON COLUMN guardian_transfer_request.to_user_id IS '目标主监护人用户ID';
COMMENT ON COLUMN guardian_transfer_request.status IS '状态：PENDING_CONFIRM、COMPLETED、REJECTED、REVOKED、EXPIRED';
COMMENT ON COLUMN guardian_transfer_request.reason IS '转移原因';
COMMENT ON COLUMN guardian_transfer_request.reject_reason IS '拒绝原因';
COMMENT ON COLUMN guardian_transfer_request.cancel_reason IS '取消原因';
COMMENT ON COLUMN guardian_transfer_request.expire_at IS '请求过期时间';
COMMENT ON COLUMN guardian_transfer_request.confirmed_at IS '确认接受时间';
COMMENT ON COLUMN guardian_transfer_request.rejected_at IS '拒绝时间';
COMMENT ON COLUMN guardian_transfer_request.revoked_at IS '撤销时间';
COMMENT ON COLUMN guardian_transfer_request.version IS '乐观锁版本号';
COMMENT ON COLUMN guardian_transfer_request.trace_id IS '分布式追踪ID';
COMMENT ON COLUMN guardian_transfer_request.created_at IS '记录创建时间';
COMMENT ON COLUMN guardian_transfer_request.updated_at IS '记录更新时间';
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
COMMENT ON TABLE rescue_task IS '搜救任务表';
COMMENT ON COLUMN rescue_task.id IS '任务ID，自增主键';
COMMENT ON COLUMN rescue_task.task_no IS '任务编号，全局唯一可读号';
COMMENT ON COLUMN rescue_task.patient_id IS '关联患者ID';
COMMENT ON COLUMN rescue_task.status IS '任务状态：CREATED(已创建)、ACTIVE(进行中)、SUSTAINED(持续搜救)、CLOSED_FOUND(已找到)、CLOSED_FALSE_ALARM(误报关闭)';
COMMENT ON COLUMN rescue_task.source IS '任务来源：APP、MINI_PROGRAM、ADMIN_PORTAL、AUTO_UPGRADE(自动升级)';
COMMENT ON COLUMN rescue_task.remark IS '任务备注';
COMMENT ON COLUMN rescue_task.daily_appearance IS '日常外貌描述快照';
COMMENT ON COLUMN rescue_task.daily_photo_url IS '日常照片URL快照';
COMMENT ON COLUMN rescue_task.ai_analysis_summary IS 'AI分析摘要';
COMMENT ON COLUMN rescue_task.poster_url IS '生成的寻人海报URL';
COMMENT ON COLUMN rescue_task.close_type IS '关闭类型：FOUND、FALSE_ALARM';
COMMENT ON COLUMN rescue_task.close_reason IS '关闭原因说明';
COMMENT ON COLUMN rescue_task.found_location_lat IS '找到时的纬度';
COMMENT ON COLUMN rescue_task.found_location_lng IS '找到时的经度';
COMMENT ON COLUMN rescue_task.event_version IS '事件版本号，用于处理乱序事件';
COMMENT ON COLUMN rescue_task.created_by IS '创建人用户ID';
COMMENT ON COLUMN rescue_task.closed_by IS '关闭人用户ID';
COMMENT ON COLUMN rescue_task.closed_at IS '关闭时间';
COMMENT ON COLUMN rescue_task.sustained_at IS '转为持续搜救状态的时间';
COMMENT ON COLUMN rescue_task.version IS '乐观锁版本号';
COMMENT ON COLUMN rescue_task.trace_id IS '分布式追踪ID';
COMMENT ON COLUMN rescue_task.created_at IS '记录创建时间';
COMMENT ON COLUMN rescue_task.updated_at IS '记录更新时间';
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
COMMENT ON TABLE clue_record IS '线索记录表，存储路人或家属上报的走失线索';
COMMENT ON COLUMN clue_record.id IS '线索ID，自增主键';
COMMENT ON COLUMN clue_record.clue_no IS '线索编号';
COMMENT ON COLUMN clue_record.patient_id IS '关联患者ID';
COMMENT ON COLUMN clue_record.task_id IS '关联任务ID';
COMMENT ON COLUMN clue_record.tag_code IS '关联的标签码';
COMMENT ON COLUMN clue_record.source_type IS '线索来源类型：SCAN(扫码)、MANUAL(手动上报)、POSTER_SCAN(海报扫码)';
COMMENT ON COLUMN clue_record.reporter_user_id IS '上报人用户ID（如为注册用户）';
COMMENT ON COLUMN clue_record.reporter_type IS '上报人类型：FAMILY、ANONYMOUS、ADMIN';
COMMENT ON COLUMN clue_record.latitude IS '纬度';
COMMENT ON COLUMN clue_record.longitude IS '经度';
COMMENT ON COLUMN clue_record.coord_system IS '坐标系统：WGS84、GCJ-02、BD-09';
COMMENT ON COLUMN clue_record.description IS '线索描述';
COMMENT ON COLUMN clue_record.photo_urls IS '线索照片URL数组';
COMMENT ON COLUMN clue_record.tag_only IS '是否仅为扫码记录（无照片/描述）';
COMMENT ON COLUMN clue_record.risk_score IS '风险评分，范围0-1';
COMMENT ON COLUMN clue_record.status IS '线索状态：VALID(有效)、OVERRIDDEN(已覆盖)、REJECTED(已驳回)、INVALID(无效)';
COMMENT ON COLUMN clue_record.suspect_flag IS '是否为可疑线索';
COMMENT ON COLUMN clue_record.suspect_reason IS '可疑原因';
COMMENT ON COLUMN clue_record.drift_flag IS '是否为漂移数据';
COMMENT ON COLUMN clue_record.review_status IS '审核状态：PENDING、OVERRIDDEN、REJECTED';
COMMENT ON COLUMN clue_record.override_reason IS '覆盖原因';
COMMENT ON COLUMN clue_record.reject_reason IS '驳回原因';
COMMENT ON COLUMN clue_record.reviewed_by IS '审核人用户ID';
COMMENT ON COLUMN clue_record.reviewed_at IS '审核时间';
COMMENT ON COLUMN clue_record.device_fingerprint IS '上报设备指纹';
COMMENT ON COLUMN clue_record.entry_token_jti IS '匿名上报凭证JTI';
COMMENT ON COLUMN clue_record.client_ip IS '客户端IP';
COMMENT ON COLUMN clue_record.version IS '乐观锁版本号';
COMMENT ON COLUMN clue_record.trace_id IS '分布式追踪ID';
COMMENT ON COLUMN clue_record.created_at IS '记录创建时间';
COMMENT ON COLUMN clue_record.updated_at IS '记录更新时间';
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
COMMENT ON TABLE patient_trajectory IS '患者轨迹表，存储时间窗口内的聚合轨迹数据';
COMMENT ON COLUMN patient_trajectory.id IS '轨迹ID，自增主键';
COMMENT ON COLUMN patient_trajectory.patient_id IS '患者ID';
COMMENT ON COLUMN patient_trajectory.task_id IS '关联任务ID';
COMMENT ON COLUMN patient_trajectory.clue_id IS '关联线索ID';
COMMENT ON COLUMN patient_trajectory.window_start IS '时间窗口起始时间';
COMMENT ON COLUMN patient_trajectory.window_end IS '时间窗口结束时间';
COMMENT ON COLUMN patient_trajectory.point_count IS '窗口内原始点数';
COMMENT ON COLUMN patient_trajectory.geometry_type IS '几何类型：LINESTRING、SPARSE_POINT、EMPTY_WINDOW、POINT';
COMMENT ON COLUMN patient_trajectory.geometry_data IS '几何数据JSONB';
COMMENT ON COLUMN patient_trajectory.latitude IS '当为单点时存储纬度';
COMMENT ON COLUMN patient_trajectory.longitude IS '当为单点时存储经度';
COMMENT ON COLUMN patient_trajectory.source_type IS '数据来源';
COMMENT ON COLUMN patient_trajectory.version IS '乐观锁版本号';
COMMENT ON COLUMN patient_trajectory.trace_id IS '分布式追踪ID';
COMMENT ON COLUMN patient_trajectory.created_at IS '记录创建时间';
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
    CONSTRAINT chk_tag_status CHECK (status IN ('UNBOUND','ALLOCATED','BOUND','SUSPECTED_LOST','LOST','VOIDED')),
    CONSTRAINT chk_tag_type   CHECK (tag_type IN ('QR_CODE','NFC'))
);
COMMENT ON TABLE tag_asset IS '标签资产表，管理二维码或NFC标签的实物资产';
COMMENT ON COLUMN tag_asset.id IS '标签ID，自增主键';
COMMENT ON COLUMN tag_asset.tag_code IS '标签唯一码';
COMMENT ON COLUMN tag_asset.tag_type IS '标签类型：QR_CODE、NFC';
COMMENT ON COLUMN tag_asset.status IS '状态：UNBOUND(未绑定)、ALLOCATED(已分配)、BOUND(已绑定)、SUSPECTED_LOST(疑似丢失)、LOST(丢失)、VOIDED(作废)';
COMMENT ON COLUMN tag_asset.patient_id IS '绑定患者ID';
COMMENT ON COLUMN tag_asset.short_code IS '患者短码';
COMMENT ON COLUMN tag_asset.order_id IS '关联申领工单ID';
COMMENT ON COLUMN tag_asset.resource_token IS '标签资源访问令牌';
COMMENT ON COLUMN tag_asset.batch_no IS '生产批次号';
COMMENT ON COLUMN tag_asset.void_reason IS '作废原因';
COMMENT ON COLUMN tag_asset.lost_reason IS '丢失原因';
COMMENT ON COLUMN tag_asset.lost_at IS '丢失时间';
COMMENT ON COLUMN tag_asset.void_at IS '作废时间';
COMMENT ON COLUMN tag_asset.suspected_lost_at IS '疑似丢失时间';
COMMENT ON COLUMN tag_asset.bound_at IS '绑定时间';
COMMENT ON COLUMN tag_asset.allocated_at IS '分配时间';
COMMENT ON COLUMN tag_asset.version IS '乐观锁版本号';
COMMENT ON COLUMN tag_asset.trace_id IS '分布式追踪ID';
COMMENT ON COLUMN tag_asset.created_at IS '记录创建时间';
COMMENT ON COLUMN tag_asset.updated_at IS '记录更新时间';
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
COMMENT ON TABLE tag_apply_record IS '标签申领工单表';
COMMENT ON COLUMN tag_apply_record.id IS '工单ID，自增主键';
COMMENT ON COLUMN tag_apply_record.order_no IS '工单编号';
COMMENT ON COLUMN tag_apply_record.patient_id IS '申领患者ID';
COMMENT ON COLUMN tag_apply_record.applicant_user_id IS '申请人用户ID';
COMMENT ON COLUMN tag_apply_record.tag_type IS '申领标签类型';
COMMENT ON COLUMN tag_apply_record.quantity IS '申领数量';
COMMENT ON COLUMN tag_apply_record.remark IS '备注';
COMMENT ON COLUMN tag_apply_record.status IS '工单状态：PENDING_AUDIT、PENDING_SHIP、SHIPPED、RECEIVED、EXCEPTION、REJECTED、CANCELLED、VOIDED';
COMMENT ON COLUMN tag_apply_record.shipping_province IS '收货省份';
COMMENT ON COLUMN tag_apply_record.shipping_city IS '收货城市';
COMMENT ON COLUMN tag_apply_record.shipping_district IS '收货区县';
COMMENT ON COLUMN tag_apply_record.shipping_detail IS '详细地址';
COMMENT ON COLUMN tag_apply_record.shipping_receiver IS '收件人姓名';
COMMENT ON COLUMN tag_apply_record.shipping_phone IS '收件人电话';
COMMENT ON COLUMN tag_apply_record.reviewer_user_id IS '审核人用户ID';
COMMENT ON COLUMN tag_apply_record.reviewed_at IS '审核时间';
COMMENT ON COLUMN tag_apply_record.reject_reason IS '驳回原因';
COMMENT ON COLUMN tag_apply_record.tag_codes IS '分配的标签码列表JSON';
COMMENT ON COLUMN tag_apply_record.logistics_no IS '物流单号';
COMMENT ON COLUMN tag_apply_record.logistics_company IS '物流公司';
COMMENT ON COLUMN tag_apply_record.shipped_at IS '发货时间';
COMMENT ON COLUMN tag_apply_record.received_at IS '收货确认时间';
COMMENT ON COLUMN tag_apply_record.cancel_reason IS '取消原因';
COMMENT ON COLUMN tag_apply_record.cancelled_at IS '取消时间';
COMMENT ON COLUMN tag_apply_record.exception_desc IS '异常描述';
COMMENT ON COLUMN tag_apply_record.version IS '乐观锁版本号';
COMMENT ON COLUMN tag_apply_record.trace_id IS '分布式追踪ID';
COMMENT ON COLUMN tag_apply_record.created_at IS '记录创建时间';
COMMENT ON COLUMN tag_apply_record.updated_at IS '记录更新时间';
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
COMMENT ON TABLE tag_batch_job IS '标签批量生成任务表';
COMMENT ON COLUMN tag_batch_job.id IS '任务ID，自增主键';
COMMENT ON COLUMN tag_batch_job.job_id IS '任务唯一标识';
COMMENT ON COLUMN tag_batch_job.tag_type IS '标签类型';
COMMENT ON COLUMN tag_batch_job.quantity IS '计划生成数量';
COMMENT ON COLUMN tag_batch_job.success_count IS '成功数量';
COMMENT ON COLUMN tag_batch_job.fail_count IS '失败数量';
COMMENT ON COLUMN tag_batch_job.status IS '状态：PENDING、RUNNING、COMPLETED、FAILED';
COMMENT ON COLUMN tag_batch_job.batch_key_id IS '批次密钥ID';
COMMENT ON COLUMN tag_batch_job.created_by IS '创建人用户ID';
COMMENT ON COLUMN tag_batch_job.completed_at IS '完成时间';
COMMENT ON COLUMN tag_batch_job.trace_id IS '分布式追踪ID';
COMMENT ON COLUMN tag_batch_job.created_at IS '记录创建时间';
COMMENT ON COLUMN tag_batch_job.updated_at IS '记录更新时间';
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
COMMENT ON TABLE ai_session IS 'AI对话会话表';
COMMENT ON COLUMN ai_session.id IS '会话ID，自增主键';
COMMENT ON COLUMN ai_session.session_id IS '会话唯一标识';
COMMENT ON COLUMN ai_session.user_id IS '用户ID';
COMMENT ON COLUMN ai_session.patient_id IS '关联患者ID';
COMMENT ON COLUMN ai_session.task_id IS '关联任务ID';
COMMENT ON COLUMN ai_session.messages IS '对话消息JSON数组';
COMMENT ON COLUMN ai_session.prompt_tokens IS '提示词Token消耗';
COMMENT ON COLUMN ai_session.completion_tokens IS '补全Token消耗';
COMMENT ON COLUMN ai_session.total_tokens IS '总Token消耗';
COMMENT ON COLUMN ai_session.token_usage IS 'Token使用详情JSON';
COMMENT ON COLUMN ai_session.model_name IS '使用的模型名称';
COMMENT ON COLUMN ai_session.status IS '会话状态：ACTIVE、ARCHIVED';
COMMENT ON COLUMN ai_session.feedback_rating IS '用户反馈评分';
COMMENT ON COLUMN ai_session.feedback_comment IS '反馈内容';
COMMENT ON COLUMN ai_session.feedback_at IS '反馈时间';
COMMENT ON COLUMN ai_session.archived_at IS '归档时间';
COMMENT ON COLUMN ai_session.version IS '乐观锁版本号';
COMMENT ON COLUMN ai_session.trace_id IS '分布式追踪ID';
COMMENT ON COLUMN ai_session.created_at IS '记录创建时间';
COMMENT ON COLUMN ai_session.updated_at IS '记录更新时间';
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
COMMENT ON TABLE ai_quota_ledger IS 'AI配额账本表，支持用户和患者两种维度的配额控制';
COMMENT ON COLUMN ai_quota_ledger.id IS '账本ID，自增主键';
COMMENT ON COLUMN ai_quota_ledger.ledger_type IS '账本类型：USER、PATIENT';
COMMENT ON COLUMN ai_quota_ledger.owner_id IS '拥有者ID（用户ID或患者ID）';
COMMENT ON COLUMN ai_quota_ledger.period IS '账期标识，如2025-04';
COMMENT ON COLUMN ai_quota_ledger.quota_limit IS '配额上限';
COMMENT ON COLUMN ai_quota_ledger.used IS '已使用配额';
COMMENT ON COLUMN ai_quota_ledger.reserved IS '已预留配额';
COMMENT ON COLUMN ai_quota_ledger.version IS '乐观锁版本号';
COMMENT ON COLUMN ai_quota_ledger.created_at IS '记录创建时间';
COMMENT ON COLUMN ai_quota_ledger.updated_at IS '记录更新时间';
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
COMMENT ON TABLE patient_memory_note IS '患者记忆笔记表，存储与患者相关的结构化或非结构化记忆信息';
COMMENT ON COLUMN patient_memory_note.id IS '笔记ID，自增主键';
COMMENT ON COLUMN patient_memory_note.note_id IS '笔记唯一标识';
COMMENT ON COLUMN patient_memory_note.patient_id IS '患者ID';
COMMENT ON COLUMN patient_memory_note.created_by IS '创建人用户ID';
COMMENT ON COLUMN patient_memory_note.kind IS '记忆类型：HABIT、PLACE、PREFERENCE、SAFETY_CUE、RESCUE_CASE';
COMMENT ON COLUMN patient_memory_note.content IS '内容文本';
COMMENT ON COLUMN patient_memory_note.tags IS '标签JSON数组';
COMMENT ON COLUMN patient_memory_note.source_event_id IS '来源事件ID';
COMMENT ON COLUMN patient_memory_note.version IS '乐观锁版本号';
COMMENT ON COLUMN patient_memory_note.trace_id IS '分布式追踪ID';
COMMENT ON COLUMN patient_memory_note.created_at IS '记录创建时间';
COMMENT ON COLUMN patient_memory_note.updated_at IS '记录更新时间';
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
COMMENT ON TABLE vector_store IS '向量存储表，为pgvector预留，目前以TEXT存储向量';
COMMENT ON COLUMN vector_store.id IS '向量记录ID，自增主键';
COMMENT ON COLUMN vector_store.patient_id IS '关联患者ID';
COMMENT ON COLUMN vector_store.source_type IS '来源类型';
COMMENT ON COLUMN vector_store.source_id IS '来源记录ID';
COMMENT ON COLUMN vector_store.source_version IS '来源记录版本';
COMMENT ON COLUMN vector_store.content IS '原始文本内容';
COMMENT ON COLUMN vector_store.embedding IS '向量数据，TEXT格式存储';
COMMENT ON COLUMN vector_store.valid IS '是否有效';
COMMENT ON COLUMN vector_store.superseded_at IS '被替代时间';
COMMENT ON COLUMN vector_store.deleted_at IS '删除时间';
COMMENT ON COLUMN vector_store.expired_at IS '过期时间';
COMMENT ON COLUMN vector_store.trace_id IS '分布式追踪ID';
COMMENT ON COLUMN vector_store.created_at IS '记录创建时间';
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
COMMENT ON TABLE ai_intent IS 'AI意图待确认表，存储需要用户确认的高风险操作意图';
COMMENT ON COLUMN ai_intent.id IS '意图ID，自增主键';
COMMENT ON COLUMN ai_intent.intent_id IS '意图唯一标识';
COMMENT ON COLUMN ai_intent.session_id IS '关联会话ID';
COMMENT ON COLUMN ai_intent.user_id IS '用户ID';
COMMENT ON COLUMN ai_intent.action IS '意图动作';
COMMENT ON COLUMN ai_intent.description IS '意图描述';
COMMENT ON COLUMN ai_intent.parameters IS '参数JSON';
COMMENT ON COLUMN ai_intent.execution_level IS '执行级别，如CONFIRM_1';
COMMENT ON COLUMN ai_intent.requires_confirm IS '是否需要用户确认';
COMMENT ON COLUMN ai_intent.status IS '状态：PENDING、APPROVED、REJECTED、EXPIRED';
COMMENT ON COLUMN ai_intent.execution_result IS '执行结果JSON';
COMMENT ON COLUMN ai_intent.expire_at IS '过期时间';
COMMENT ON COLUMN ai_intent.processed_at IS '处理时间';
COMMENT ON COLUMN ai_intent.version IS '乐观锁版本号';
COMMENT ON COLUMN ai_intent.trace_id IS '分布式追踪ID';
COMMENT ON COLUMN ai_intent.created_at IS '记录创建时间';
COMMENT ON COLUMN ai_intent.updated_at IS '记录更新时间';
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
COMMENT ON TABLE notification_inbox IS '通知收件箱表';
COMMENT ON COLUMN notification_inbox.notification_id IS '通知ID，自增主键';
COMMENT ON COLUMN notification_inbox.user_id IS '接收用户ID';
COMMENT ON COLUMN notification_inbox.type IS '通知类型：TASK_PROGRESS、FENCE_ALERT等';
COMMENT ON COLUMN notification_inbox.title IS '通知标题';
COMMENT ON COLUMN notification_inbox.content IS '通知内容';
COMMENT ON COLUMN notification_inbox.level IS '通知级别：INFO、WARN、CRITICAL';
COMMENT ON COLUMN notification_inbox.channel IS '通知渠道，默认INBOX';
COMMENT ON COLUMN notification_inbox.related_task_id IS '关联任务ID';
COMMENT ON COLUMN notification_inbox.related_patient_id IS '关联患者ID';
COMMENT ON COLUMN notification_inbox.related_object_id IS '关联对象ID';
COMMENT ON COLUMN notification_inbox.read_status IS '阅读状态：UNREAD、READ';
COMMENT ON COLUMN notification_inbox.read_at IS '阅读时间';
COMMENT ON COLUMN notification_inbox.trace_id IS '分布式追踪ID';
COMMENT ON COLUMN notification_inbox.created_at IS '记录创建时间';
COMMENT ON COLUMN notification_inbox.updated_at IS '记录更新时间';
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
COMMENT ON TABLE sys_log IS '系统日志表，记录用户和AI操作审计日志';
COMMENT ON COLUMN sys_log.id IS '日志ID，自增主键';
COMMENT ON COLUMN sys_log.module IS '模块名称';
COMMENT ON COLUMN sys_log.action IS '操作动作';
COMMENT ON COLUMN sys_log.action_source IS '操作来源：USER、AI_AGENT、SYSTEM';
COMMENT ON COLUMN sys_log.operator_user_id IS '操作用户ID';
COMMENT ON COLUMN sys_log.operator_username IS '操作用户名';
COMMENT ON COLUMN sys_log.object_id IS '操作对象ID';
COMMENT ON COLUMN sys_log.result IS '操作结果：SUCCESS、FAIL';
COMMENT ON COLUMN sys_log.result_code IS '结果码';
COMMENT ON COLUMN sys_log.risk_level IS '风险等级';
COMMENT ON COLUMN sys_log.detail IS '操作详情JSON';
COMMENT ON COLUMN sys_log.agent_profile IS '代理档案';
COMMENT ON COLUMN sys_log.execution_mode IS '执行模式';
COMMENT ON COLUMN sys_log.confirm_level IS '确认级别';
COMMENT ON COLUMN sys_log.blocked_reason IS '阻断原因';
COMMENT ON COLUMN sys_log.client_ip IS '客户端IP';
COMMENT ON COLUMN sys_log.request_id IS '请求ID';
COMMENT ON COLUMN sys_log.trace_id IS '分布式追踪ID';
COMMENT ON COLUMN sys_log.executed_at IS '执行时间';
COMMENT ON COLUMN sys_log.created_at IS '记录创建时间';
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
COMMENT ON TABLE sys_config IS '系统配置表';
COMMENT ON COLUMN sys_config.config_key IS '配置键';
COMMENT ON COLUMN sys_config.config_value IS '配置值';
COMMENT ON COLUMN sys_config.scope IS '作用域：public、ops、security、ai_policy';
COMMENT ON COLUMN sys_config.description IS '配置描述';
COMMENT ON COLUMN sys_config.updated_by IS '更新人用户ID';
COMMENT ON COLUMN sys_config.updated_reason IS '更新原因';
COMMENT ON COLUMN sys_config.version IS '乐观锁版本号';
COMMENT ON COLUMN sys_config.created_at IS '记录创建时间';
COMMENT ON COLUMN sys_config.updated_at IS '记录更新时间';
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
COMMENT ON TABLE sys_outbox_log IS '发件箱日志表，用于可靠事件投递';
COMMENT ON COLUMN sys_outbox_log.id IS '日志ID，自增主键';
COMMENT ON COLUMN sys_outbox_log.event_id IS '事件唯一ID';
COMMENT ON COLUMN sys_outbox_log.topic IS '目标Topic';
COMMENT ON COLUMN sys_outbox_log.aggregate_id IS '聚合根ID';
COMMENT ON COLUMN sys_outbox_log.partition_key IS '分区键';
COMMENT ON COLUMN sys_outbox_log.payload IS '事件负载JSON';
COMMENT ON COLUMN sys_outbox_log.request_id IS '请求ID';
COMMENT ON COLUMN sys_outbox_log.trace_id IS '分布式追踪ID';
COMMENT ON COLUMN sys_outbox_log.phase IS '状态：PENDING、DISPATCHING、SENT、RETRY、DEAD';
COMMENT ON COLUMN sys_outbox_log.retry_count IS '重试次数';
COMMENT ON COLUMN sys_outbox_log.next_retry_at IS '下次重试时间';
COMMENT ON COLUMN sys_outbox_log.lease_owner IS '租约持有者标识';
COMMENT ON COLUMN sys_outbox_log.lease_until IS '租约过期时间';
COMMENT ON COLUMN sys_outbox_log.sent_at IS '成功发送时间';
COMMENT ON COLUMN sys_outbox_log.last_error IS '最后一次错误信息';
COMMENT ON COLUMN sys_outbox_log.last_intervention_by IS '最后介入人用户ID';
COMMENT ON COLUMN sys_outbox_log.last_intervention_at IS '最后介入时间';
COMMENT ON COLUMN sys_outbox_log.replay_reason IS '重放原因';
COMMENT ON COLUMN sys_outbox_log.replay_token IS '重放令牌';
COMMENT ON COLUMN sys_outbox_log.replayed_at IS '重放时间';
COMMENT ON COLUMN sys_outbox_log.created_at IS '记录创建时间';
COMMENT ON COLUMN sys_outbox_log.updated_at IS '记录更新时间';
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
COMMENT ON TABLE consumed_event_log IS '消费事件日志表，用于幂等消费';
COMMENT ON COLUMN consumed_event_log.id IS '日志ID，自增主键';
COMMENT ON COLUMN consumed_event_log.consumer_name IS '消费者名称';
COMMENT ON COLUMN consumed_event_log.topic IS 'Topic名称';
COMMENT ON COLUMN consumed_event_log.event_id IS '事件ID';
COMMENT ON COLUMN consumed_event_log.partition_no IS '分区号';
COMMENT ON COLUMN consumed_event_log.msg_offset IS '消息偏移量';
COMMENT ON COLUMN consumed_event_log.trace_id IS '分布式追踪ID';
COMMENT ON COLUMN consumed_event_log.processed_at IS '处理时间';
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
COMMENT ON TABLE ws_ticket IS 'WebSocket一次性连接票据表';
COMMENT ON COLUMN ws_ticket.id IS '票据ID，自增主键';
COMMENT ON COLUMN ws_ticket.ticket IS '票据字符串';
COMMENT ON COLUMN ws_ticket.user_id IS '关联用户ID';
COMMENT ON COLUMN ws_ticket.expire_at IS '过期时间';
COMMENT ON COLUMN ws_ticket.used_at IS '使用时间';
COMMENT ON COLUMN ws_ticket.created_at IS '记录创建时间';
CREATE UNIQUE INDEX IF NOT EXISTS ux_ws_ticket ON ws_ticket(ticket);