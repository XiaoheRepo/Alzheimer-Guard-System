// src/stores/auth.ts
import { defineStore } from 'pinia'
import type { Role, UserStatus } from '@/types/common'
import * as authApi from '@/api/auth'
import * as meApi from '@/api/me'

export interface CurrentUser {
  user_id: string
  username: string
  nickname: string
  email?: string
  phone?: string
  role: Role
  status?: UserStatus
  avatar_url?: string
  email_verified?: boolean
  created_at?: string
  last_login_at?: string
}

export const useAuthStore = defineStore('auth', {
  state: () => ({
    accessToken: '' as string,
    refreshToken: '' as string,
    expiresIn: 0 as number,
    user: null as CurrentUser | null,
  }),
  getters: {
    isLoggedIn: (s) => !!s.accessToken,
    isAdmin: (s) => s.user?.role === 'ADMIN' || s.user?.role === 'SUPER_ADMIN',
    isSuperAdmin: (s) => s.user?.role === 'SUPER_ADMIN',
  },
  actions: {
    async login(payload: { username: string; password: string }) {
      const data = await authApi.login(payload)
      this.accessToken = data.access_token
      this.refreshToken = data.refresh_token
      this.expiresIn = data.expires_in
      // 登录响应中的 user 仅为简要信息，完整信息仍走 /users/me
      if (data.user) {
        this.user = {
          user_id: data.user.user_id,
          username: data.user.username,
          nickname: data.user.nickname,
          role: data.user.role,
          avatar_url: data.user.avatar_url,
        }
      }
      await this.fetchCurrentUser()
      return this.user
    },
    async fetchCurrentUser() {
      this.user = await meApi.getCurrentUser()
      return this.user
    },
    hasPermission(): boolean {
      // 管理端默认按角色控制；按钮级细粒度由调用方结合 role 判断
      return this.isAdmin
    },
    /** 角色准入：仅 admin / super_admin 可访问管理端 */
    isBackofficeAllowed(): boolean {
      return this.user?.role === 'ADMIN' || this.user?.role === 'SUPER_ADMIN'
    },
    async logout() {
      this.$reset()
      // 清理持久化
      localStorage.removeItem('auth')
    },
  },
  persist: {
    key: 'auth',
    pick: ['accessToken', 'refreshToken', 'expiresIn'],
  },
})
