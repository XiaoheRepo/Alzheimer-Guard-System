/**
 * Token 和本地存储管理
 */

import { TOKEN_KEY, REFRESH_TOKEN_KEY, USER_INFO_KEY } from '@/constants'
import type { User } from '@/types'

// ============ Token 管理 ============
export const getToken = (): string | null => {
  return localStorage.getItem(TOKEN_KEY)
}

export const setToken = (token: string): void => {
  localStorage.setItem(TOKEN_KEY, token)
}

export const removeToken = (): void => {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(REFRESH_TOKEN_KEY)
  localStorage.removeItem(USER_INFO_KEY)
}

// ============ Refresh Token 管理 ============
export const getRefreshToken = (): string | null => {
  return localStorage.getItem(REFRESH_TOKEN_KEY)
}

export const setRefreshToken = (token: string): void => {
  localStorage.setItem(REFRESH_TOKEN_KEY, token)
}

// ============ 用户信息管理 ============
export const getUserInfoFromStorage = (): User | null => {
  const userInfo = localStorage.getItem(USER_INFO_KEY)
  return userInfo ? JSON.parse(userInfo) : null
}

export const setUserInfoToStorage = (userInfo: User): void => {
  localStorage.setItem(USER_INFO_KEY, JSON.stringify(userInfo))
}
