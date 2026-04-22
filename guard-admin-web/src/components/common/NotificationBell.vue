<!-- src/components/common/NotificationBell.vue -->
<script setup lang="ts">
import { onMounted } from 'vue'
import { BellOutlined } from '@ant-design/icons-vue'
import { useRouter } from 'vue-router'
import { useNotificationStore } from '@/stores/notification'
import { getInbox } from '@/api/notification'

const store = useNotificationStore()
const router = useRouter()

async function refresh() {
  try {
    const res = await getInbox({ limit: 10 })
    store.setItems(res.items || [])
    const unread = (res.items || []).filter((x) => !x.read).length
    store.setUnreadCount(unread)
  } catch {
    /* ignore */
  }
}

function go() {
  void router.push('/notifications')
}

onMounted(() => {
  void refresh()
})
</script>

<template>
  <a-badge :count="store.unreadCount" :offset="[-4, 4]">
    <a-button type="text" shape="circle" :aria-label="$t('menu.notification')" @click="go">
      <template #icon><BellOutlined /></template>
    </a-button>
  </a-badge>
</template>
