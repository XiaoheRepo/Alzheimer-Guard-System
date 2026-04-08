/**
 * 认证相关 API
 * 接口定义依据 doc/API_from_SRS_SADD_LLD.md §3.7
 * 响应由 request.ts 拦截器解包，调用方直接获得 data 对象
 */

import request from './request'
import type { LoginApiResponse } from '@/types'

/** POST /api/v1/auth/login — 账号密码登录，返回 JWT 令牌 */
export const login = (data: { username: string; password: string }): Promise<LoginApiResponse> => {
  return request.post('/auth/login', data) as unknown as Promise<LoginApiResponse>
}

/** POST /api/v1/auth/logout — 注销登录，服务端将 jti 写入黑名单 */
export const logout = (): Promise<unknown> => {
  return request.post('/auth/logout')
}
