/**
 * 认证状态管理
 */

import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { message } from 'ant-design-vue'
import type { User, UserRole, LoginParams } from '@/types'
import { login as loginApi, logout as logoutApi, getUserInfo as getUserInfoApi } from '@/api/auth'
import { setToken, setRefreshToken, removeToken, getToken } from '@/utils/auth'

export const useAuthStore = defineStore('auth', () => {
  // 状态
  const token = ref<string | null>(getToken())
  const userInfo = ref<User | null>(null)
  const roles = ref<UserRole[]>([])
  const permissions = ref<string[]>([])

  // 计算属性
  const isLoggedIn = computed(() => !!token.value)
  const isSuperAdmin = computed(() => roles.value.includes('super_admin' as UserRole))
  const isAdmin = computed(
    () =>
      roles.value.includes('super_admin' as UserRole) || roles.value.includes('admin' as UserRole),
  )

  /**
   * 登录
   */
  const login = async (loginParams: LoginParams) => {
    try {
      const res = await loginApi(loginParams)

      // 保存 Token
      token.value = res.token
      setToken(res.token)

      if (res.refreshToken) {
        setRefreshToken(res.refreshToken)
      }

      // 保存用户信息
      userInfo.value = res.userInfo
      roles.value = res.roles
      permissions.value = res.permissions

      message.success('登录成功')
      return res
    } catch (error) {
      message.error('登录失败，请检查用户名和密码')
      throw error
    }
  }

  /**
   * 登出
   */
  const logout = async () => {
    try {
      await logoutApi()
    } catch (error) {
      console.error('登出接口调用失败:', error)
    } finally {
      // 清除本地数据
      token.value = null
      userInfo.value = null
      roles.value = []
      permissions.value = []
      removeToken()

      message.success('已退出登录')
    }
  }

  /**
   * 获取用户信息
   */
  const getUserInfo = async () => {
    try {
      const res = await getUserInfoApi()
      userInfo.value = res.userInfo
      roles.value = res.roles
      permissions.value = res.permissions
      return res
    } catch (error) {
      // 获取用户信息失败，清除登录状态
      removeToken()
      throw error
    }
  }

  /**
   * 检查权限
   */
  const hasPermission = (permission: string): boolean => {
    // 超级管理员拥有所有权限
    if (isSuperAdmin.value) return true
    return permissions.value.includes(permission)
  }

  /**
   * 检查角色
   */
  const hasRole = (role: UserRole): boolean => {
    return roles.value.includes(role)
  }

  return {
    // 状态
    token,
    userInfo,
    roles,
    permissions,
    // 计算属性
    isLoggedIn,
    isSuperAdmin,
    isAdmin,
    // 方法
    login,
    logout,
    getUserInfo,
    hasPermission,
    hasRole,
  }
})
