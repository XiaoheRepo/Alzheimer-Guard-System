// src/api/notification.ts
// 对齐 API_V2.0.md §3.5
import { http } from '@/utils/request'
import type { CursorPage, OffsetPage } from '@/types/common'
import type { NotificationItem } from '@/stores/notification'

export type InboxResponse =
  | OffsetPage<NotificationItem>
  | CursorPage<NotificationItem>

/** GET /api/v1/notifications/inbox */
export function getInbox(
  params?: Record<string, unknown>,
): Promise<InboxResponse> {
  return http.get<InboxResponse>('/api/v1/notifications/inbox', { params })
}

/** POST /api/v1/notifications/{notification_id}/read */
export function markRead(notificationId: string): Promise<null> {
  return http.post<null>(
    `/api/v1/notifications/${encodeURIComponent(notificationId)}/read`,
  )
}
