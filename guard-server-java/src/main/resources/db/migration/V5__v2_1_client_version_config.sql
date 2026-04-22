-- ============================================================
-- V5: V2.1 基线增量 —— 客户端版本与公告默认配置
-- 依据：API §3.8.6.1；MetaController 读取的 sys_config 键
-- ============================================================

INSERT INTO sys_config (config_key, config_value, scope, description, created_at, updated_at, version)
VALUES
  ('client.version.android.latest',             '2.1.0', 'public', '当前最新 Android 版本', now(), now(), 0),
  ('client.version.android.min_compatible',     '2.0.0', 'public', 'Android 最低兼容版本', now(), now(), 0),
  ('client.version.android.force_upgrade',      'false', 'public', 'Android 是否强升',    now(), now(), 0),
  ('client.version.android.release_notes_url',  'https://static.example.com/release/android/2.1.0.html', 'public', '发版说明', now(), now(), 0),
  ('client.version.android.download_url',       'https://static.example.com/apk/android/mashang-2.1.0.apk', 'public', 'APK 下载', now(), now(), 0),
  ('client.version.h5.latest',                  '2.1.0', 'public', '当前最新 H5 版本',    now(), now(), 0),
  ('client.version.h5.min_compatible',          '2.0.0', 'public', 'H5 最低兼容版本',     now(), now(), 0),
  ('client.version.h5.force_upgrade',           'false', 'public', 'H5 是否强升',         now(), now(), 0),
  ('client.version.web_admin.latest',           '2.1.0', 'public', '当前最新管理端版本',  now(), now(), 0),
  ('client.version.web_admin.min_compatible',   '2.0.0', 'public', '管理端最低兼容版本',  now(), now(), 0),
  ('client.version.web_admin.force_upgrade',    'false', 'public', '管理端是否强升',       now(), now(), 0)
ON CONFLICT (config_key) DO NOTHING;
