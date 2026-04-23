// src/api/notification.ts
// 对齐 API_V2.0.md §3.5
import { http } from '@/utils/request'
import type { CursorPage, OffsetPage } from '@/types/common'
import type { NotificationItem } from '@/stores/notification'

export type InboxResponse = OffsetPage<NotificationItem> | CursorPage<NotificationItem>

/** GET /api/v1/notifications/inbox */
export function getInbox(params?: Record<string, unknown>): Promise<InboxResponse> {
  return http.get<InboxResponse>('/api/v1/notifications/inbox', { params })
}

/** 标记通知已读响应（API_V2.0.md §3.6.12） */
export interface MarkReadResponse {
  notification_id: string
  read_status: 'READ'
  read_at: string
}

/** POST /api/v1/notifications/{notification_id}/read */
export function markRead(notificationId: string): Promise<MarkReadResponse> {
  return http.post<MarkReadResponse>(
    `/api/v1/notifications/${encodeURIComponent(notificationId)}/read`,
  )
}
