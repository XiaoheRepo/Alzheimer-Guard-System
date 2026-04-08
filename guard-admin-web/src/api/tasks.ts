/**
 * 任务治理 API
 * 依据 doc/API_from_SRS_SADD_LLD.md §3.1.11-3.1.15
 */

import request from './request'
import type { PageOffset } from '@/types'

export type TaskStatus = 'ACTIVE' | 'RESOLVED' | 'FALSE_ALARM' | 'UNKNOWN'
export type TaskSource = 'APP' | 'MINI_PROGRAM' | 'ADMIN_PORTAL' | 'UNKNOWN'

export interface TaskItem {
  task_id: string
  patient_id: string
  status: TaskStatus
  source: TaskSource
  reported_by: string
  event_time: string
}

export interface TaskListParams {
  page_no?: number
  page_size?: number
  status?: TaskStatus
  source?: TaskSource
}

export interface NotifyRetryBody {
  reason: string
}

export interface ForceCloseBody {
  close_type: 'FALSE_ALARM' | 'ADMIN_CLOSE'
  reason: string
}

/** GET /api/v1/admin/rescue/tasks */
export const getTaskList = (params?: TaskListParams): Promise<PageOffset<TaskItem>> =>
  request.get('/admin/rescue/tasks', { params }) as unknown as Promise<PageOffset<TaskItem>>

/** GET /api/v1/admin/rescue/tasks/{task_id} */
export const getTaskDetail = (taskId: string): Promise<TaskItem> =>
  request.get(`/admin/rescue/tasks/${taskId}`) as unknown as Promise<TaskItem>

/** POST /api/v1/admin/rescue/tasks/{task_id}/notify/retry */
export const retryNotify = (taskId: string, body: NotifyRetryBody): Promise<unknown> =>
  request.post(`/admin/rescue/tasks/${taskId}/notify/retry`, body)

/** POST /api/v1/admin/super/rescue/tasks/{task_id}/force-close （仅 SUPERADMIN） */
export const forceCloseTask = (taskId: string, body: ForceCloseBody): Promise<unknown> =>
  request.post(`/admin/super/rescue/tasks/${taskId}/force-close`, body)
