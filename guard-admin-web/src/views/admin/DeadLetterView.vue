<!-- src/views/admin/DeadLetterView.vue / P-11 -->
<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import {
  listDeadEvents,
  replayDeadEvent,
  type DeadEventItem,
} from '@/api/admin'
import PageHeader from '@/components/common/PageHeader.vue'
import ProTable from '@/components/common/ProTable.vue'
import PermissionButton from '@/components/domain/PermissionButton.vue'
import CopyableText from '@/components/common/CopyableText.vue'
import { fmtDateTime } from '@/utils/format'
import type { ApiError } from '@/utils/request'

const { t } = useI18n()

const filter = reactive({ topic: '' })
const data = ref<DeadEventItem[]>([])
const loading = ref(false)
const nextCursor = ref('')
const cursorHist = ref<string[]>([''])
const cursorIdx = ref(0)

const dlg = reactive({
  open: false,
  item: null as DeadEventItem | null,
  reason: '',
  confirmTail: '',
  submitting: false,
})

const columns = [
  { title: t('page.dead.col.eventId'), key: 'event_id', width: 200 },
  { title: t('page.dead.col.topic'), dataIndex: 'topic', width: 200 },
  { title: t('page.dead.col.failCount'), dataIndex: 'fail_count', width: 100 },
  { title: t('page.dead.col.firstFail'), key: 'first_fail_at', width: 180 },
  { title: t('page.dead.col.lastError'), dataIndex: 'last_error' },
  { title: t('common.action'), key: 'action', width: 100, fixed: 'right' },
]

async function load() {
  loading.value = true
  try {
    const res = await listDeadEvents({
      topic: filter.topic || undefined,
      cursor: cursorHist.value[cursorIdx.value] || undefined,
      size: 30,
    })
    data.value = res.items || []
    nextCursor.value = res.next_cursor || ''
  } finally {
    loading.value = false
  }
}

onMounted(load)

function openReplay(item: DeadEventItem) {
  dlg.item = item
  dlg.reason = ''
  dlg.confirmTail = ''
  dlg.open = true
}

async function onReplay() {
  if (!dlg.item) return
  const tail = dlg.item.event_id.slice(-4)
  if (dlg.confirmTail !== tail) {
    message.warning(t('page.dead.confirmTailMismatch', { tail }))
    return
  }
  if (dlg.reason.trim().length < 10) {
    message.warning(t('page.dead.reasonMin'))
    return
  }
  dlg.submitting = true
  try {
    await replayDeadEvent(dlg.item.event_id, { reason: dlg.reason })
    message.success(t('common.success'))
    dlg.open = false
    await load()
  } catch (e) {
    message.error((e as ApiError)?.message || t('error.UNKNOWN'))
  } finally {
    dlg.submitting = false
  }
}

function doSearch() {
  cursorHist.value = ['']
  cursorIdx.value = 0
  load()
}
function nextPage() {
  if (!nextCursor.value) return
  cursorHist.value.push(nextCursor.value)
  cursorIdx.value += 1
  load()
}
function prevPage() {
  if (cursorIdx.value === 0) return
  cursorIdx.value -= 1
  load()
}
</script>

<template>
  <div class="page-container">
    <PageHeader :title="t('menu.adminDead')" />
    <ProTable
      :columns="columns"
      :data-source="data"
      :loading="loading"
      :row-key="(r: DeadEventItem) => r.event_id"
      :pagination="false"
      :scroll="{ x: 1200 }"
    >
      <template #filter>
        <a-form layout="inline">
          <a-form-item :label="t('page.dead.col.topic')">
            <a-input v-model:value="filter.topic" allow-clear />
          </a-form-item>
          <a-form-item>
            <a-button type="primary" @click="doSearch">{{ t('common.search') }}</a-button>
          </a-form-item>
        </a-form>
      </template>

      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'event_id'">
          <CopyableText :text="record.event_id" short />
        </template>
        <template v-else-if="column.key === 'first_fail_at'">
          {{ fmtDateTime(record.first_fail_at) }}
        </template>
        <template v-else-if="column.key === 'action'">
          <PermissionButton
            :roles="['SUPER_ADMIN']"
            type="link"
            size="small"
            @click="openReplay(record)"
          >
            {{ t('page.dead.replay') }}
          </PermissionButton>
        </template>
      </template>
    </ProTable>

    <div style="margin-top: 12px; text-align: right">
      <a-space>
        <a-button :disabled="cursorIdx === 0" @click="prevPage">{{ t('common.prev') }}</a-button>
        <a-button :disabled="!nextCursor" type="primary" @click="nextPage">{{ t('common.next') }}</a-button>
      </a-space>
    </div>

    <a-modal
      v-model:open="dlg.open"
      :title="t('page.dead.replay.title')"
      :confirm-loading="dlg.submitting"
      :ok-text="t('common.submit')"
      :cancel-text="t('common.cancel')"
      @ok="onReplay"
    >
      <a-alert
        type="warning"
        :message="t('page.dead.replay.warn')"
        show-icon
        style="margin-bottom: 12px"
      />
      <a-form v-if="dlg.item" layout="vertical">
        <a-form-item :label="t('page.dead.col.eventId')">
          <a-typography-text code>{{ dlg.item.event_id }}</a-typography-text>
        </a-form-item>
        <a-form-item :label="t('page.dead.reason')" required>
          <a-textarea v-model:value="dlg.reason" :rows="3" :maxlength="500" show-count />
        </a-form-item>
        <a-form-item
          :label="t('page.dead.confirmTail', { tail: dlg.item.event_id.slice(-4) })"
          required
        >
          <a-input
            v-model:value="dlg.confirmTail"
            :maxlength="4"
            :placeholder="dlg.item.event_id.slice(-4)"
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>
