/**
 * 用户治理 API
 * 依据 doc/API_from_SRS_SADD_LLD.md §3.8.1-3.8.3
 */

import request from './request'
import type { PageOffset } from '@/types'

export type UserStatus = 'NORMAL' | 'BANNED' | 'UNKNOWN'

export interface AdminUserItem {
  user_id: string
  username: string
  role: string
  status: UserStatus
  last_login_at: string | null
  created_at: string
}

export interface UserListParams {
  page_no?: number
  page_size?: number
  role?: string
  status?: UserStatus
  keyword?: string
}

export interface UpdateStatusBody {
  status: 'NORMAL' | 'BANNED'
  reason: string
}

export interface ResetPasswordBody {
  reason: string
}

/** GET /api/v1/admin/users */
export const getUserList = (params?: UserListParams): Promise<PageOffset<AdminUserItem>> =>
  request.get('/admin/users', { params }) as unknown as Promise<PageOffset<AdminUserItem>>

/** PUT /api/v1/admin/users/{user_id}/status */
export const updateUserStatus = (userId: string, body: UpdateStatusBody): Promise<unknown> =>
  request.put(`/admin/users/${userId}/status`, body)

/** PUT /api/v1/admin/users/{user_id}/password:reset （仅 SUPERADMIN） */
export const resetUserPassword = (userId: string, body: ResetPasswordBody): Promise<unknown> =>
  request.put(`/admin/users/${userId}/password:reset`, body)
