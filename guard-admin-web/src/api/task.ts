// src/api/task.ts
// 对齐 API_V2.0.md §3.1
import { http } from '@/utils/request'
import type { OffsetPage } from '@/types/common'
import type { TaskStatus } from '@/types/enums'

export interface TaskListItem {
  task_id: string
  task_no?: string
  patient_id: string
  patient_name?: string
  status: TaskStatus
  source?: 'APP' | 'ADMIN_PORTAL' | 'AUTO_UPGRADE'
  reported_by?: string
  created_at: string
  closed_at?: string | null
}

export interface TaskSnapshot {
  task_id: string
  task_no?: string
  patient_id: string
  status: TaskStatus
  source?: string
  reported_by?: string
  remark?: string
  created_at: string
  closed_at?: string | null
  close_type?: 'FOUND' | 'FALSE_ALARM' | null
  close_reason?: string | null
  sustained_at?: string | null
  patient_snapshot?: {
    patient_name?: string
    gender?: string
    age?: number
    avatar_url?: string
    short_code?: string
    appearance?: Record<string, unknown>
  }
  clue_summary?: {
    total_clue_count?: number
    valid_clue_count?: number
    suspect_clue_count?: number
    latest_clue_time?: string
  }
  trajectory_summary?: {
    point_count?: number
    latest_point_time?: string
    bounding_box?: Record<string, number>
  }
  version?: number
}

export interface TrajectoryLatest {
  task_id: string
  points: Array<{
    lat: number
    lng: number
    ts: string
    source?: string
    clue_id?: string
  }>
}

/** GET /api/v1/rescue/tasks */
export function listTasks(
  params: Record<string, unknown>,
): Promise<OffsetPage<TaskListItem>> {
  return http.get<OffsetPage<TaskListItem>>('/api/v1/rescue/tasks', { params })
}

/** GET /api/v1/rescue/tasks/{task_id}/snapshot */
export function getTaskSnapshot(taskId: string): Promise<TaskSnapshot> {
  return http.get<TaskSnapshot>(
    `/api/v1/rescue/tasks/${encodeURIComponent(taskId)}/snapshot`,
  )
}

/** GET /api/v1/rescue/tasks/{task_id}/full */
export function getTaskFull(taskId: string): Promise<Record<string, unknown>> {
  return http.get<Record<string, unknown>>(
    `/api/v1/rescue/tasks/${encodeURIComponent(taskId)}/full`,
  )
}

/** GET /api/v1/rescue/tasks/{task_id}/trajectory/latest */
export function getLatestTrajectory(taskId: string): Promise<TrajectoryLatest> {
  return http.get<TrajectoryLatest>(
    `/api/v1/rescue/tasks/${encodeURIComponent(taskId)}/trajectory/latest`,
  )
}

/** POST /api/v1/rescue/tasks/{task_id}/sustained */
export function markSustained(
  taskId: string,
  body: { reason?: string; request_time: string },
): Promise<TaskSnapshot> {
  return http.post<TaskSnapshot>(
    `/api/v1/rescue/tasks/${encodeURIComponent(taskId)}/sustained`,
    body,
  )
}

/** POST /api/v1/rescue/tasks/{task_id}/close */
export function closeTask(
  taskId: string,
  body: {
    close_type: 'FOUND' | 'FALSE_ALARM'
    close_reason: string
    request_time: string
  },
): Promise<TaskSnapshot> {
  return http.post<TaskSnapshot>(
    `/api/v1/rescue/tasks/${encodeURIComponent(taskId)}/close`,
    body,
  )
}
