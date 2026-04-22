// src/stores/notification.ts
import { defineStore } from 'pinia'

export interface NotificationItem {
  notification_id: string
  title: string
  body?: string
  category: 'ALERT' | 'SYS' | 'BIZ'
  level?: string
  created_at: string
  read_at?: string | null
  trace_id?: string
  deeplink?: { type: string; target_id: string }
}

export const useNotificationStore = defineStore('notification', {
  state: () => ({
    items: [] as NotificationItem[],
    unreadCount: 0,
  }),
  actions: {
    setItems(items: NotificationItem[]) {
      this.items = items
      this.unreadCount = items.filter((i) => !i.read_at).length
    },
    prepend(item: NotificationItem) {
      this.items.unshift(item)
      if (!item.read_at) this.unreadCount += 1
    },
    markRead(id: string) {
      const it = this.items.find((i) => i.notification_id === id)
      if (it && !it.read_at) {
        it.read_at = new Date().toISOString()
        this.unreadCount = Math.max(0, this.unreadCount - 1)
      }
    },
    setUnreadCount(n: number) {
      this.unreadCount = Math.max(0, n)
    },
  },
})
