/**
 * 会话存储管理
 * 依据 web_admin_handbook.md §6.1-6.4：token/role/userId 均存 sessionStorage（会话级），
 * 登出或鉴权失效时必须一次性清空，禁止写入明文密码或敏感信息。
 */

import { TOKEN_KEY, ROLE_KEY, USER_ID_KEY } from '@/constants'

// ============ Token（sessionStorage，会话级）============
export const getToken = (): string | null => {
  return sessionStorage.getItem(TOKEN_KEY)
}

export const setToken = (token: string): void => {
  sessionStorage.setItem(TOKEN_KEY, token)
}

// ============ Role ============
export const getRole = (): string | null => {
  return sessionStorage.getItem(ROLE_KEY)
}

export const setRole = (role: string): void => {
  sessionStorage.setItem(ROLE_KEY, role)
}

// ============ User ID ============
export const getUserId = (): string | null => {
  return sessionStorage.getItem(USER_ID_KEY)
}

export const setUserId = (userId: string): void => {
  sessionStorage.setItem(USER_ID_KEY, userId)
}

// ============ 清除会话（登出或 E_GOV_4011 时调用）============
export const clearSession = (): void => {
  sessionStorage.removeItem(TOKEN_KEY)
  sessionStorage.removeItem(ROLE_KEY)
  sessionStorage.removeItem(USER_ID_KEY)
}

/** @deprecated 请使用 clearSession() */
export const removeToken = clearSession
