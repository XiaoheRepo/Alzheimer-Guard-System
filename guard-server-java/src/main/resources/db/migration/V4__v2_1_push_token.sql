-- ============================================================
-- V4: V2.1 基线增量 —— 推送令牌表
-- 依据：DBD §2.6.7 user_push_token；API §3.8.5；backend_handbook §25.7
-- ============================================================

CREATE TABLE IF NOT EXISTS user_push_token (
    push_token_id   BIGSERIAL       PRIMARY KEY,
    user_id         BIGINT          NOT NULL,
    platform        VARCHAR(24)     NOT NULL,
    device_id       VARCHAR(128)    NOT NULL,
    push_token      VARCHAR(512)    NOT NULL,
    app_version     VARCHAR(32)     NOT NULL,
    os_version      VARCHAR(64),
    device_model    VARCHAR(64),
    locale          VARCHAR(16)     NOT NULL DEFAULT 'zh-CN',
    status          VARCHAR(16)     NOT NULL DEFAULT 'ACTIVE',
    last_active_at  TIMESTAMPTZ     NOT NULL DEFAULT now(),
    revoked_at      TIMESTAMPTZ,
    trace_id        VARCHAR(64)     NOT NULL,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),

    CONSTRAINT uk_user_push_token_device UNIQUE (user_id, device_id),
    CONSTRAINT ck_push_platform CHECK (platform IN (
        'ANDROID_FCM','ANDROID_HMS','ANDROID_MIPUSH','IOS_APNS','WEB_PUSH'
    )),
    CONSTRAINT ck_push_status   CHECK (status IN ('ACTIVE','REVOKED')),
    CONSTRAINT ck_push_locale   CHECK (locale IN ('zh-CN','en-US'))
);

CREATE INDEX IF NOT EXISTS idx_push_user_active
    ON user_push_token (user_id, status) WHERE status = 'ACTIVE';

CREATE INDEX IF NOT EXISTS idx_push_platform_active
    ON user_push_token (platform, status, last_active_at DESC) WHERE status = 'ACTIVE';

COMMENT ON TABLE  user_push_token IS '推送令牌表 — 按 (user_id, device_id) 唯一；notification-service 按平台分发 FCM/HMS/MiPush/APNs/WebPush';
COMMENT ON COLUMN user_push_token.push_token IS '推送服务商下发的设备 token，PII：@Desensitize(TOKEN)';
COMMENT ON COLUMN user_push_token.trace_id  IS '全链路追踪标识（HC-04）';
