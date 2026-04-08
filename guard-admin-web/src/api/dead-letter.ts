import request from './request'
import type { PageCursor } from '@/types'

// ── 类型 ─────────────────────────────────────────────────────────
export interface DeadEventItem {
  event_id: string
  created_at: string
  topic: string
  partition_key: string
  retry_count: number
  last_error: string
  updated_at: string
  last_intervention_at: string | null
  last_intervention_by: string | null
}

export interface DeadEventListParams {
  cursor?: string
  page_size?: number
  topic?: string
  partition_key?: string
  from_time?: string
  to_time?: string
}

export interface ReplayBody {
  created_at: string
  replay_reason: string
  replay_mode?: 'RETRY_NOW' | 'RETRY_AT'
  next_retry_at?: string
}

export interface ReplayResult {
  event_id: string
  previous_phase: string
  phase: string
  partition_key: string
  next_retry_at: string
  replay_reason: string
  replayed_by: string
  replayed_at: string
}

// ── API 函数 ──────────────────────────────────────────────────────

/** 3.8.12 分页查询 DEAD 队列（仅 SUPERADMIN） */
export const getDeadEvents = (params?: DeadEventListParams): Promise<PageCursor<DeadEventItem>> =>
  request.get('/admin/super/outbox/dead', {
    params,
  }) as unknown as Promise<PageCursor<DeadEventItem>>

/** 3.8.13 重放 DEAD 事件（仅 SUPERADMIN） */
export const replayDeadEvent = (eventId: string, body: ReplayBody): Promise<ReplayResult> =>
  request.post(
    `/admin/super/outbox/dead/${eventId}/replay`,
    body,
  ) as unknown as Promise<ReplayResult>

/** 3.8.6 导出数据与审计报表（仅 SUPERADMIN） */
export const exportData = (body: {
  export_type: 'AUDIT_REPORT' | 'OPS_METRICS'
  window_start: string
  window_end: string
  reason: string
}): Promise<{ export_ref_id: string; file_url: string | null; logged_at: string }> =>
  request.post('/admin/super/export-data', body) as unknown as Promise<{
    export_ref_id: string
    file_url: string | null
    logged_at: string
  }>

/** 3.8.7 清理过期审计日志（仅 SUPERADMIN） */
export const purgeAuditLogs = (body: {
  before_time: string
  reason: string
}): Promise<{ purged_count: number; before_time: string; purged_at: string }> =>
  request.post('/admin/super/logs/purge', body) as unknown as Promise<{
    purged_count: number
    before_time: string
    purged_at: string
  }>
