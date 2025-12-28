/**
 * 常量定义：角色、权限、状态等
 */

import { UserRole, UserStatus, OrderStatus } from '@/types'

// ============ 角色配置 ============
export const ROLE_LABELS: Record<UserRole, string> = {
  [UserRole.SUPER_ADMIN]: '超级管理员',
  [UserRole.ADMIN]: '管理员',
  [UserRole.COMMUNITY]: '社区管理员',
  [UserRole.FACTORY]: '工厂管理员',
  [UserRole.DOCTOR]: '医生',
  [UserRole.NURSE]: '护士',
  [UserRole.FAMILY]: '家属',
  [UserRole.VOLUNTEER]: '志愿者',
}

export const ROLE_COLORS: Record<UserRole, string> = {
  [UserRole.SUPER_ADMIN]: 'red',
  [UserRole.ADMIN]: 'volcano',
  [UserRole.COMMUNITY]: 'blue',
  [UserRole.FACTORY]: 'orange',
  [UserRole.DOCTOR]: 'cyan',
  [UserRole.NURSE]: 'geekblue',
  [UserRole.FAMILY]: 'green',
  [UserRole.VOLUNTEER]: 'purple',
}

// ============ 用户状态 ============
export const USER_STATUS_LABELS: Record<UserStatus, string> = {
  [UserStatus.ACTIVE]: '正常',
  [UserStatus.INACTIVE]: '未激活',
  [UserStatus.BANNED]: '已禁用',
}

export const USER_STATUS_COLORS: Record<UserStatus, string> = {
  [UserStatus.ACTIVE]: 'success',
  [UserStatus.INACTIVE]: 'default',
  [UserStatus.BANNED]: 'error',
}

// ============ 订单状态 ============
export const ORDER_STATUS_LABELS: Record<OrderStatus, string> = {
  [OrderStatus.CREATED]: '待生产',
  [OrderStatus.PRODUCING]: '生产中',
  [OrderStatus.SHIPPING]: '运输中',
  [OrderStatus.COMPLETED]: '已完成',
  [OrderStatus.CANCELLED]: '已取消',
}

export const ORDER_STATUS_COLORS: Record<OrderStatus, string> = {
  [OrderStatus.CREATED]: 'default',
  [OrderStatus.PRODUCING]: 'processing',
  [OrderStatus.SHIPPING]: 'warning',
  [OrderStatus.COMPLETED]: 'success',
  [OrderStatus.CANCELLED]: 'error',
}

// ============ 权限码 ============
export const PERMISSIONS = {
  // 用户管理
  USER_VIEW: 'user:view',
  USER_CREATE: 'user:create',
  USER_UPDATE: 'user:update',
  USER_DELETE: 'user:delete',
  USER_AUDIT: 'user:audit',

  // 社区管理
  COMMUNITY_VIEW: 'community:view',
  COMMUNITY_CREATE: 'community:create',
  COMMUNITY_UPDATE: 'community:update',
  COMMUNITY_DELETE: 'community:delete',

  // 工厂管理
  FACTORY_VIEW: 'factory:view',
  FACTORY_CREATE: 'factory:create',
  FACTORY_UPDATE: 'factory:update',
  FACTORY_DELETE: 'factory:delete',

  // 订单管理
  ORDER_VIEW: 'order:view',
  ORDER_UPDATE: 'order:update',
  ORDER_CANCEL: 'order:cancel',
  ORDER_DELETE: 'order:delete',

  // 系统日志
  LOG_VIEW: 'log:view',
  LOG_EXPORT: 'log:export',

  // 消息通知
  NOTIFICATION_SEND: 'notification:send',
} as const

// ============ 路由白名单 ============
export const WHITE_LIST = ['/login', '/404', '/403']

// ============ Token 相关 ============
export const TOKEN_KEY = 'guard_admin_token'
export const REFRESH_TOKEN_KEY = 'guard_admin_refresh_token'
export const USER_INFO_KEY = 'guard_admin_user_info'

// ============ 分页配置 ============
export const PAGE_SIZE = 10
export const PAGE_SIZE_OPTIONS = ['10', '20', '50', '100']
