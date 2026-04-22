// src/api/dashboard.ts
// 对齐 API_V2.0.md §3.7.1
import { http } from '@/utils/request'

export interface DashboardSummary {
  active_task_count?: number
  pending_clue_count?: number
  pending_order_count?: number
  recent_alert_count?: number
  unread_notification_count?: number
  [key: string]: unknown
}

export interface DashboardAlert {
  timestamp: string
  level: 'INFO' | 'WARN' | 'ALERT' | 'CRITICAL'
  category?: string
  message: string
  trace_id?: string
}

export interface DashboardData {
  summary: DashboardSummary
  series?: {
    task_daily?: { date: string; value: number }[]
    clue_daily?: { date: string; value: number }[]
  }
  recent_alerts?: DashboardAlert[]
}

/** GET /api/v1/dashboard */
export function getDashboard(
  params?: { range?: '7d' | '14d' | '30d' } & Record<string, unknown>,
): Promise<DashboardData> {
  return http.get<DashboardData>('/api/v1/dashboard', { params })
}
