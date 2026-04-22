<!-- src/views/notification/InboxView.vue / P-08 -->
<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { getInbox, markRead } from '@/api/notification'
import type { NotificationItem } from '@/stores/notification'
import type { NotificationCategory } from '@/types/enums'
import PageHeader from '@/components/common/PageHeader.vue'
import StatusBadge from '@/components/domain/StatusBadge.vue'
import { fmtDateTime, fmtFromNow } from '@/utils/format'
import { sanitizeHtml } from '@/utils/sanitize'
import { useNotificationStore } from '@/stores/notification'

const { t } = useI18n()
const store = useNotificationStore()

const tab = ref<NotificationCategory | 'ALL'>('ALL')
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const data = ref<NotificationItem[]>([])
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    const params: Record<string, unknown> = {
      page: pagination.current,
      size: pagination.pageSize,
    }
    if (tab.value !== 'ALL') params.category = tab.value
    const res = await getInbox(params)
    const items = (res as { items?: NotificationItem[] }).items || []
    data.value = items
    pagination.total = (res as { total?: number }).total || items.length
    store.setItems(items)
  } finally {
    loading.value = false
  }
}

onMounted(load)

async function onRead(item: NotificationItem) {
  if (item.read_at) return
  try {
    await markRead(item.notification_id)
    item.read_at = new Date().toISOString()
    store.markRead(item.notification_id)
  } catch {
    /* ignore */
  }
}

function onTabChange() {
  pagination.current = 1
  load()
}

const categoryTabs = computed(() => [
  { key: 'ALL', label: t('common.all') },
  { key: 'ALERT', label: t('field.notificationCategory.ALERT') },
  { key: 'BIZ', label: t('field.notificationCategory.BIZ') },
  { key: 'SYS', label: t('field.notificationCategory.SYS') },
])
</script>

<template>
  <div class="page-container">
    <PageHeader :title="t('menu.notification')" />
    <a-card>
      <a-tabs v-model:active-key="tab" @change="onTabChange">
        <a-tab-pane v-for="tp in categoryTabs" :key="tp.key" :tab="tp.label" />
      </a-tabs>

      <a-list
        :loading="loading"
        :data-source="data"
        :pagination="{
          ...pagination,
          showSizeChanger: true,
          onChange: (p: number, s: number) => {
            pagination.current = p
            pagination.pageSize = s
            load()
          },
        }"
      >
        <template #renderItem="{ item }">
          <a-list-item :class="{ 'notif-unread': !item.read_at }" @click="onRead(item)">
            <a-list-item-meta>
              <template #title>
                <StatusBadge kind="notificationCategory" :value="item.category" />
                <span style="margin-left: 8px; font-weight: 500">{{ item.title }}</span>
                <a-badge v-if="!item.read_at" color="red" style="margin-left: 8px" />
              </template>
              <template #description>
                <div v-if="item.body" v-html="sanitizeHtml(item.body)" class="notif-body" />
                <div class="text-muted" style="margin-top: 4px">
                  {{ fmtFromNow(item.created_at) }} · {{ fmtDateTime(item.created_at) }}
                </div>
              </template>
            </a-list-item-meta>
          </a-list-item>
        </template>
      </a-list>
    </a-card>
  </div>
</template>

<style scoped>
.notif-unread {
  background: rgba(249, 115, 22, 0.05);
}
.notif-body {
  color: var(--text-color);
}
</style>
