// src/types/common.ts
// 通用外壳与分页类型，对齐 API_V2.0.md §1.4 / §1.5

export type ApiEnvelope<T> = {
  code: string
  message: string
  trace_id: string
  data: T
}

export type OffsetPage<T> = {
  items: T[]
  page_no: number
  page_size: number
  total: number
  has_next: boolean
}

export type CursorPage<T> = {
  items: T[]
  next_cursor?: string | null
  has_next: boolean
}

export type Role = 'FAMILY' | 'ADMIN' | 'SUPER_ADMIN'
export type UserStatus = 'ACTIVE' | 'DISABLED' | 'DEACTIVATED'
