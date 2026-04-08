/**
 * 全局类型定义
 */

// ============ API 响应结构 ============
export interface ApiResponse<T = unknown> {
  code: string
  message: string
  trace_id: string
  data: T
}

// 偏移分页（任务/用户/日志等列表）
export interface PageOffset<T> {
  items: T[]
  page_no: number
  page_size: number
  total: number
  has_next: boolean
}

// 游标分页（审计日志/DEAD 队列）
export interface PageCursor<T> {
  items: T[]
  page_size: number
  next_cursor: string | null
  has_next: boolean
}

/** @deprecated 使用 PageOffset<T> 替代 */
export interface PageResult<T> {
  list: T[]
  total: number
  pageNum: number
  pageSize: number
}

// ============ 用户相关 ============
export enum UserRole {
  SUPERADMIN = 'SUPERADMIN',
  ADMIN = 'ADMIN',
  COMMUNITY = 'COMMUNITY',
  FACTORY = 'FACTORY',
  DOCTOR = 'DOCTOR',
  NURSE = 'NURSE',
  FAMILY = 'FAMILY',
  VOLUNTEER = 'VOLUNTEER',
}

export enum UserStatus {
  ACTIVE = 'active',
  INACTIVE = 'inactive',
  BANNED = 'banned',
}

export interface User {
  id: string
  username: string
  email?: string
  phone?: string
  realName?: string
  avatar?: string
  role: UserRole
  status: UserStatus
  createdAt: string
  updatedAt: string
}

export interface LoginParams {
  username: string
  password: string
}

/** 与 POST /api/v1/auth/login 响应 data 对齐 */
export interface LoginApiResponse {
  token: string
  expires_in: number
  user: {
    user_id: string
    role: string
  }
}

// ============ 社区相关 ============
export interface Community {
  id: string
  name: string
  address: string
  centerLocation: {
    lng: number
    lat: number
  }
  serviceArea?: Record<string, unknown> // PostGIS Polygon
  adminId?: string
  adminName?: string
  patientCount?: number
  createdAt: string
}

// ============ 工厂相关 ============
export interface Factory {
  id: string
  name: string
  contact: string
  phone: string
  address: string
  capacity?: number
  status: 'active' | 'inactive'
  createdAt: string
}

export enum OrderStatus {
  CREATED = 'created',
  PRODUCING = 'producing',
  SHIPPING = 'shipping',
  COMPLETED = 'completed',
  CANCELLED = 'cancelled',
}

export interface Order {
  id: string
  orderNo: string
  userId: string
  userName?: string
  factoryId: string
  factoryName?: string
  status: OrderStatus
  totalAmount: number
  address?: string
  createdAt: string
  updatedAt: string
  cancelledAt?: string
}

// ============ 老人相关 ============
export interface Patient {
  id: string
  name: string
  age: number
  gender: 'male' | 'female'
  avatar?: string
  guardianId: string
  guardianName?: string
  guardianPhone?: string
  communityId: string
  communityName?: string
  antiLostCode: string
  status: 'normal' | 'lost'
  medicalHistory?: string
  currentLocation?: {
    lng: number
    lat: number
  }
  createdAt: string
}

export interface ClueRecord {
  id: string
  patientId: string
  patientName?: string
  reporterId: string
  reporterName?: string
  location: {
    lng: number
    lat: number
  }
  address?: string
  description: string
  status: 'pending' | 'verified' | 'false_alarm'
  createdAt: string
  verifiedAt?: string
}

// ============ 系统相关 ============
export interface AuditLog {
  id: string
  userId: string
  userName: string
  userAvatar?: string
  module: string
  action: string
  ip: string
  detail?: Record<string, unknown>
  createdAt: string
}

export interface Notification {
  id: string
  title: string
  content: string
  type: 'system' | 'order' | 'lost' | 'alert'
  targetUserId?: string
  targetRole?: UserRole
  isRead: boolean
  createdAt: string
}
