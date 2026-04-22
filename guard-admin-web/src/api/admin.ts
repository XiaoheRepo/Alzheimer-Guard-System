// src/api/admin.ts
// 系统配置 / 审计日志 / 用户管理 / DEAD / WS
// 对齐 API_V2.0.md §3.6.8+ / §3.7 / §3.8
import { http } from '@/utils/request'
import type { CursorPage, OffsetPage, Role, UserStatus } from '@/types/common'

/** -------- 系统配置 -------- */
export interface SysConfigItem {
  config_key: string
  config_value: string
  value_type: 'int' | 'float' | 'boolean' | 'string' | 'json'
  group?: string
  description?: string
  min?: number
  max?: number
  updated_at?: string
  updated_by?: string
}

/** GET /api/v1/admin/configs */
export function listConfigs(
  params?: Record<string, unknown>,
): Promise<OffsetPage<SysConfigItem> | { items: SysConfigItem[] }> {
  return http.get<OffsetPage<SysConfigItem>>('/api/v1/admin/configs', { params })
}

/** PUT /api/v1/admin/configs/{config_key} */
export function updateConfig(
  configKey: string,
  body: { value: string; reason?: string },
): Promise<SysConfigItem> {
  return http.put<SysConfigItem>(
    `/api/v1/admin/configs/${encodeURIComponent(configKey)}`,
    body,
  )
}

/** -------- 审计日志 -------- */
export interface AuditLogItem {
  log_id: string
  ts: string
  operator_id?: string
  operator_name?: string
  role?: Role
  action: string
  resource_type?: string
  resource_id?: string
  trace_id?: string
  status?: 'SUCCESS' | 'FAILED'
  request_summary?: string
  request_body?: unknown
  response_body?: unknown
  duration_ms?: number
  pod_id?: string
}

/** GET /api/v1/admin/logs */
export function listAuditLogs(
  params: Record<string, unknown>,
): Promise<CursorPage<AuditLogItem>> {
  return http.get<CursorPage<AuditLogItem>>('/api/v1/admin/logs', { params })
}

/** GET /api/v1/admin/logs/export （返回文件流，使用 responseType blob） */
export function exportAuditLogs(params: Record<string, unknown>) {
  // baseURL 内已配置；此处需要 blob；走底层 request
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const req = (http as any)
  return req.get<Blob>('/api/v1/admin/logs/export', {
    params,
    responseType: 'blob',
    transformResponse: [(d: unknown) => d],
  })
}

/** -------- 用户管理 -------- */
export interface AdminUserListItem {
  user_id: string
  username: string
  nickname: string
  email?: string
  phone?: string
  role: Role
  status: UserStatus
  last_login_at?: string
  created_at?: string
}

/** GET /api/v1/admin/users */
export function listAdminUsers(
  params: Record<string, unknown>,
): Promise<CursorPage<AdminUserListItem> | OffsetPage<AdminUserListItem>> {
  return http.get<CursorPage<AdminUserListItem>>('/api/v1/admin/users', { params })
}

/** GET /api/v1/admin/users/{user_id} */
export function getAdminUser(userId: string): Promise<AdminUserListItem> {
  return http.get<AdminUserListItem>(
    `/api/v1/admin/users/${encodeURIComponent(userId)}`,
  )
}

/** PUT /api/v1/admin/users/{user_id} */
export function updateAdminUser(
  userId: string,
  body: { nickname?: string; email?: string; phone?: string; role?: Role },
): Promise<AdminUserListItem> {
  return http.put<AdminUserListItem>(
    `/api/v1/admin/users/${encodeURIComponent(userId)}`,
    body,
  )
}

/** POST /api/v1/admin/users/{user_id}/disable */
export function disableAdminUser(
  userId: string,
  body: { reason: string },
): Promise<AdminUserListItem> {
  return http.post<AdminUserListItem>(
    `/api/v1/admin/users/${encodeURIComponent(userId)}/disable`,
    body,
    { headers: { 'X-Confirm-Level': 'CONFIRM_2' } },
  )
}

/** POST /api/v1/admin/users/{user_id}/enable */
export function enableAdminUser(
  userId: string,
  body?: { reason?: string },
): Promise<AdminUserListItem> {
  return http.post<AdminUserListItem>(
    `/api/v1/admin/users/${encodeURIComponent(userId)}/enable`,
    body || {},
  )
}

/** DELETE /api/v1/admin/users/{user_id} */
export function deleteAdminUser(
  userId: string,
  body: { reason: string },
): Promise<null> {
  return http.delete<null>(
    `/api/v1/admin/users/${encodeURIComponent(userId)}`,
    {
      data: body,
      headers: { 'X-Confirm-Level': 'CONFIRM_3' },
    },
  )
}

/** -------- DEAD 事件 -------- */
export interface DeadEventItem {
  event_id: string
  topic: string
  partition_key?: string
  fail_count: number
  first_fail_at: string
  last_error?: string
  payload?: unknown
  headers?: Record<string, unknown>
  retry_records?: Array<{ ts: string; error: string }>
  trace_id?: string
  replaying?: boolean
}

/** GET /api/v1/admin/super/outbox/dead */
export function listDeadEvents(
  params: Record<string, unknown>,
): Promise<CursorPage<DeadEventItem>> {
  return http.get<CursorPage<DeadEventItem>>(
    '/api/v1/admin/super/outbox/dead',
    { params },
  )
}

/** POST /api/v1/admin/super/outbox/dead/{event_id}/replay */
export function replayDeadEvent(
  eventId: string,
  body: { reason: string },
): Promise<{ replay_id: string }> {
  return http.post<{ replay_id: string }>(
    `/api/v1/admin/super/outbox/dead/${encodeURIComponent(eventId)}/replay`,
    body,
    { headers: { 'X-Confirm-Level': 'CONFIRM_2' } },
  )
}

/** -------- WebSocket ticket -------- */
/** POST /api/v1/ws/ticket */
export function getWsTicket(): Promise<{ ticket: string; expires_in: number }> {
  return http.post<{ ticket: string; expires_in: number }>('/api/v1/ws/ticket')
}
