// src/api/auth.ts
// 对齐 API_V2.0.md §3.6
import { http } from '@/utils/request'
import type { Role } from '@/types/common'

export interface LoginResponse {
  access_token: string
  refresh_token: string
  token_type: 'Bearer'
  expires_in: number
  user?: {
    user_id: string
    username: string
    nickname: string
    role: Role
    avatar_url?: string
  }
}

/** POST /api/v1/auth/login */
export function login(body: { username: string; password: string }): Promise<LoginResponse> {
  return http.post<LoginResponse>('/api/v1/auth/login', body)
}

/** POST /api/v1/auth/token/refresh */
export function refreshToken(body: { refresh_token: string }): Promise<LoginResponse> {
  return http.post<LoginResponse>('/api/v1/auth/token/refresh', body)
}

/** POST /api/v1/auth/password-reset/request */
export function passwordResetRequest(body: { email: string }): Promise<null> {
  return http.post<null>('/api/v1/auth/password-reset/request', body)
}

/** POST /api/v1/auth/password-reset/confirm */
export function passwordResetConfirm(body: {
  token: string
  new_password: string
}): Promise<null> {
  return http.post<null>('/api/v1/auth/password-reset/confirm', body)
}
