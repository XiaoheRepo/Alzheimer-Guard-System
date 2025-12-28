/**
 * 全局类型定义
 */

// ============ API 响应结构 ============
export interface ApiResponse<T = unknown> {
  code: number
  message: string
  data: T
}

export interface PageResult<T> {
  list: T[]
  total: number
  pageNum: number
  pageSize: number
}

// ============ 用户相关 ============
export enum UserRole {
  SUPER_ADMIN = 'super_admin',
  ADMIN = 'admin',
  COMMUNITY = 'community',
  FACTORY = 'factory',
  DOCTOR = 'doctor',
  NURSE = 'nurse',
  FAMILY = 'family',
  VOLUNTEER = 'volunteer',
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
  remember?: boolean
}

export interface LoginResult {
  token: string
  refreshToken?: string
  userInfo: User
  roles: UserRole[]
  permissions: string[]
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
