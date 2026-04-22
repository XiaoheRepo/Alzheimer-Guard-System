// src/api/me.ts
// 对齐 API_V2.0.md §3.6.6 / §3.6.7
import { http } from '@/utils/request'
import type { CurrentUser } from '@/stores/auth'

/** GET /api/v1/users/me — API_V2.0.md §3.6.6 */
export function getCurrentUser(): Promise<CurrentUser> {
  return http.get<CurrentUser>('/api/v1/users/me')
}

/** PUT /api/v1/users/me/password — API_V2.0.md §3.6.7 */
export function changePassword(body: {
  old_password: string
  new_password: string
  request_time: string
}): Promise<null> {
  return http.put<null>('/api/v1/users/me/password', body)
}
