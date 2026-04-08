/**
 * 看板与安全指标 API
 * 接口依据 doc/API_from_SRS_SADD_LLD.md §3.8.5、§3.2.14、§3.8.10
 */

import request from './request'

export type DashboardWindow = '1h' | '24h' | '7d' | '30d'

export interface DashboardMetrics {
  window: string
  login_success_rate: number
  risk_operation_count: number
  tp95_ms: number
  error_rate: number
}

export interface ClueStatistics {
  time_from: string
  time_to: string
  granularity: string
  total_clues: number
  suspected_count: number
  overridden_count: number
  rejected_count: number
  avg_review_minutes: number
}

export interface SecurityMetrics {
  scope: string
  time_from: string
  time_to: string
  failed_login_count: number
  risk_operation_count: number
  banned_user_count: number
  captcha_trigger_count: number
}

/** GET /api/v1/admin/dashboard/metrics */
export const getDashboardMetrics = (window: DashboardWindow = '24h'): Promise<DashboardMetrics> =>
  request.get('/admin/dashboard/metrics', {
    params: { window },
  }) as unknown as Promise<DashboardMetrics>

/** GET /api/v1/admin/clues/statistics */
export const getClueStatistics = (params?: {
  time_from?: string
  time_to?: string
  granularity?: 'day' | 'week' | 'month'
}): Promise<ClueStatistics> =>
  request.get('/admin/clues/statistics', { params }) as unknown as Promise<ClueStatistics>

/** GET /api/v1/admin/metrics/security */
export const getSecurityMetrics = (params?: {
  time_from?: string
  time_to?: string
  scope?: 'summary' | 'detail'
}): Promise<SecurityMetrics> =>
  request.get('/admin/metrics/security', { params }) as unknown as Promise<SecurityMetrics>
