/**
 * 认证相关 API
 */

import request from './request'
import type { LoginParams, LoginResult, ApiResponse } from '@/types'

/**
 * 用户登录
 */
export const login = (data: LoginParams) => {
  return request.post<ApiResponse<LoginResult>>('/auth/login', data).then((res) => res.data.data)
}

/**
 * 用户登出
 */
export const logout = () => {
  return request.post('/auth/logout')
}

/**
 * 获取当前用户信息
 */
export const getUserInfo = () => {
  return request.get<ApiResponse<LoginResult>>('/auth/user-info').then((res) => res.data.data)
}

/**
 * 刷新 Token
 */
export const refreshToken = (refreshToken: string) => {
  return request
    .post<ApiResponse<{ token: string }>>('/auth/refresh-token', { refreshToken })
    .then((res) => res.data.data)
}
