/**
 * 认证状态管理
 * 依据 web_admin_handbook.md §6.1-6.4：
 *   - token/role/userId 存 sessionStorage，登出或失效时一次性清空
 *   - 管理端仅允许 ADMIN / SUPERADMIN 角色登录
 */

import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { message } from 'ant-design-vue'
import { login as loginApi, logout as logoutApi } from '@/api/auth'
import { getToken, setToken, getRole, setRole, setUserId, clearSession } from '@/utils/auth'

export const useAuthStore = defineStore('auth', () => {
  // 从 sessionStorage 恢复会话（页面刷新场景）
  const token = ref<string | null>(getToken())
  const role = ref<string | null>(getRole())
  const userId = ref<string | null>(null)

  // 计算属性
  const isLoggedIn = computed(() => !!token.value)
  const isSuperAdmin = computed(() => role.value === 'SUPERADMIN')
  const isAdmin = computed(() => role.value === 'ADMIN' || role.value === 'SUPERADMIN')

  /**
   * 登录
   * 管理端仅允许 ADMIN / SUPERADMIN。后端返回其他角色时拒绝并清会话。
   */
  const login = async (params: { username: string; password: string }) => {
    const data = await loginApi(params)

    const userRole = data.user.role
    if (userRole !== 'ADMIN' && userRole !== 'SUPERADMIN') {
      message.error('无访问权限，该账号不具备管理员角色')
      throw new Error('E_GOV_4030')
    }

    token.value = data.token
    role.value = userRole
    userId.value = data.user.user_id

    setToken(data.token)
    setRole(userRole)
    setUserId(data.user.user_id)

    message.success('登录成功')
    return data
  }

  /**
   * 登出
   * 调用注销接口后无论成功与否都清除本地会话（handbook §6.4）
   */
  const logout = async () => {
    try {
      await logoutApi()
    } catch {
      // 忽略接口错误，确保本地会话一定清除
    } finally {
      token.value = null
      role.value = null
      userId.value = null
      clearSession()
      message.success('已退出登录')
    }
  }

  /**
   * 检查是否具有指定角色（含向上兼容：SUPERADMIN 拥有 ADMIN 权限）
   */
  const hasRole = (requiredRole: 'ADMIN' | 'SUPERADMIN'): boolean => {
    if (requiredRole === 'ADMIN') return isAdmin.value
    return isSuperAdmin.value
  }

  return {
    token,
    role,
    userId,
    isLoggedIn,
    isSuperAdmin,
    isAdmin,
    login,
    logout,
    hasRole,
  }
})
