// src/types/enums.ts
// 业务枚举（对齐 LLD/DBD/API）

export type TaskStatus = 'CREATED' | 'ACTIVE' | 'SUSTAINED' | 'CLOSED_FOUND' | 'CLOSED_FALSE_ALARM'

export type ClueReviewState = 'SUBMITTED' | 'PENDING' | 'PENDING_REVIEW' | 'VALID' | 'INVALID'

export type OrderState =
  | 'PENDING_AUDIT'
  | 'PENDING_SHIP'
  | 'SHIPPED'
  | 'RECEIVED'
  | 'CANCELLED'
  | 'EXCEPTION'

export type TagState = 'UNBOUND' | 'ALLOCATED' | 'BOUND' | 'SUSPECTED_LOST' | 'LOST' | 'VOIDED'

export type PatientStatus = 'NORMAL' | 'MISSING_PENDING' | 'MISSING'

export type NotificationCategory = 'ALERT' | 'SYS' | 'BIZ'

export type RiskLevel = 'LOW' | 'MEDIUM' | 'HIGH'
