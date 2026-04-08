/**
 * 审计日志 API
 * 依据 doc/API_from_SRS_SADD_LLD.md §3.8.4
 * 使用 Cursor 分页，支持 trace_id 精确检索
 */

import request from './request'
import type { PageCursor } from '@/types'

export interface AuditLogItem {
  log_id: string
  module: string
  action: string
  operator_user_id: string
  result: 'SUCCESS' | 'FAIL' | 'UNKNOWN'
  trace_id: string
  created_at: string
}

export interface AuditLogParams {
  cursor?: string
  page_size?: number
  module?: string
  action?: string
  user_id?: string
  trace_id?: string
  start_time?: string
  end_time?: string
}

/** GET /api/v1/admin/logs */
export const getAuditLogs = (params?: AuditLogParams): Promise<PageCursor<AuditLogItem>> =>
  request.get('/admin/logs', { params }) as unknown as Promise<PageCursor<AuditLogItem>>
