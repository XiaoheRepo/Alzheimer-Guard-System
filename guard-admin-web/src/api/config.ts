import request from './request'

// ── 类型 ─────────────────────────────────────────────────────────
export type ConfigScope = 'public' | 'ops' | 'security' | 'ai_policy'

export interface ConfigItem {
  config_key: string
  config_value: string
  updated_at: string
}

export interface ConfigSnapshot {
  scope: ConfigScope
  items: ConfigItem[]
}

export interface UpdateConfigBody {
  config_key: string
  config_value: string
  reason: string
}

export interface UpdatedConfigItem {
  config_key: string
  config_value: string
  scope: ConfigScope
  updated_reason: string
  updated_at: string
}

// ── API 函数 ──────────────────────────────────────────────────────

/** 3.8.11 读取全局配置快照（ADMIN/SUPERADMIN，security/ai_policy 仅 SUPERADMIN） */
export const getConfig = (scope?: ConfigScope): Promise<ConfigSnapshot> =>
  request.get('/admin/config', {
    params: scope ? { scope } : undefined,
  }) as unknown as Promise<ConfigSnapshot>

/** 3.8.8 超级管理员修改配置（仅 SUPERADMIN） */
export const updateConfig = (body: UpdateConfigBody): Promise<UpdatedConfigItem> =>
  request.put('/admin/super/config', body) as unknown as Promise<UpdatedConfigItem>
