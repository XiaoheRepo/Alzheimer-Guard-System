<template>
  <div class="page-container">
    <a-card :bordered="false">
      <!-- 筛选栏 -->
      <a-space wrap style="margin-bottom: 12px">
        <a-input
          v-model:value="params.topic"
          placeholder="Topic 前缀过滤"
          allow-clear
          style="width: 220px"
          @press-enter="firstLoad"
        />
        <a-input
          v-model:value="params.partition_key"
          placeholder="Partition Key 精确匹配"
          allow-clear
          style="width: 220px"
          @press-enter="firstLoad"
        />
        <a-button type="primary" @click="firstLoad">查询</a-button>
        <a-button @click="resetFilters">重置</a-button>
      </a-space>

      <a-table
        :columns="columns"
        :data-source="items"
        :loading="loading"
        :pagination="false"
        row-key="event_id"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'last_intervention_at'">
            {{ record.last_intervention_at ?? '—' }}
          </template>
          <template v-else-if="column.key === 'action'">
            <a-button
              type="link"
              size="small"
              @click="openReplay(record)"
            >重放</a-button>
          </template>
        </template>
      </a-table>

      <!-- 加载更多 (cursor pagination) -->
      <div style="text-align: center; margin-top: 16px">
        <a-button v-if="hasNext" :loading="loading" @click="loadMore">加载更多</a-button>
        <span v-else-if="items.length > 0" style="color: #999">已加载全部</span>
      </div>
    </a-card>

    <!-- 重放弹窗 -->
    <a-modal
      v-model:open="replayModalVisible"
      title="重放 DEAD 事件"
      :ok-button-props="{ loading: replaySubmitting, disabled: replayReason.length < 5, danger: true }"
      ok-text="确认重放"
      @ok="submitReplay"
    >
      <a-alert
        message="重放将尝试重新投递该事件到 Broker，请确认链路已恢复后操作。"
        type="warning"
        show-icon
        style="margin-bottom: 12px"
      />
      <a-descriptions :column="1" size="small" bordered>
        <a-descriptions-item label="事件 ID">{{ currentEvent?.event_id }}</a-descriptions-item>
        <a-descriptions-item label="Topic">{{ currentEvent?.topic }}</a-descriptions-item>
        <a-descriptions-item label="Partition Key">{{ currentEvent?.partition_key }}</a-descriptions-item>
        <a-descriptions-item label="重试次数">{{ currentEvent?.retry_count }}</a-descriptions-item>
        <a-descriptions-item label="最近错误">{{ currentEvent?.last_error }}</a-descriptions-item>
      </a-descriptions>
      <a-form layout="vertical" style="margin-top: 16px">
        <a-form-item label="重放模式">
          <a-radio-group v-model:value="replayMode">
            <a-radio value="RETRY_NOW">立即重试</a-radio>
            <a-radio value="RETRY_AT">定时重试</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item v-if="replayMode === 'RETRY_AT'" label="重试时间" required>
          <a-input v-model:value="nextRetryAt" placeholder="ISO-8601 格式，如 2026-04-06T12:00:00Z" />
        </a-form-item>
        <a-form-item label="重放原因（5-256 字）" required>
          <a-textarea
            v-model:value="replayReason"
            :rows="3"
            :maxlength="256"
            show-count
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import {
  getDeadEvents,
  replayDeadEvent,
  type DeadEventItem,
  type DeadEventListParams,
} from '@/api/dead-letter'

const params = reactive<DeadEventListParams>({
  page_size: 20,
})
const items = ref<DeadEventItem[]>([])
const cursor = ref<string | null>(null)
const hasNext = ref(false)
const loading = ref(false)

const columns = [
  { title: '事件 ID', dataIndex: 'event_id', key: 'event_id', width: 200, ellipsis: true },
  { title: 'Topic', dataIndex: 'topic', key: 'topic', width: 200, ellipsis: true },
  { title: 'Partition Key', dataIndex: 'partition_key', key: 'partition_key', width: 180 },
  { title: '重试次数', dataIndex: 'retry_count', key: 'retry_count', width: 80 },
  { title: '最近错误', dataIndex: 'last_error', key: 'last_error', ellipsis: true },
  { title: '创建时间', dataIndex: 'created_at', key: 'created_at', width: 180 },
  { title: '最近干预', key: 'last_intervention_at', dataIndex: 'last_intervention_at', width: 180 },
  { title: '操作', key: 'action', width: 80, fixed: 'right' as const },
]

async function firstLoad() {
  cursor.value = null
  items.value = []
  await fetchPage()
}

async function loadMore() {
  if (!hasNext.value) return
  await fetchPage()
}

async function fetchPage() {
  loading.value = true
  try {
    const p: DeadEventListParams = { ...params }
    if (cursor.value) p.cursor = cursor.value
    const res = await getDeadEvents(p)
    items.value = [...items.value, ...res.items]
    cursor.value = res.next_cursor ?? null
    hasNext.value = res.has_next
  } catch {
    message.error('加载 DEAD 队列失败')
  } finally {
    loading.value = false
  }
}

function resetFilters() {
  params.topic = undefined
  params.partition_key = undefined
  firstLoad()
}

// ── 重放 ──────────────────────────────────────────────────────────
const replayModalVisible = ref(false)
const currentEvent = ref<DeadEventItem | null>(null)
const replayReason = ref('')
const replayMode = ref<'RETRY_NOW' | 'RETRY_AT'>('RETRY_NOW')
const nextRetryAt = ref('')
const replaySubmitting = ref(false)

function openReplay(event: DeadEventItem) {
  currentEvent.value = event
  replayReason.value = ''
  replayMode.value = 'RETRY_NOW'
  nextRetryAt.value = ''
  replayModalVisible.value = true
}

async function submitReplay() {
  if (!currentEvent.value) return
  replaySubmitting.value = true
  try {
    await replayDeadEvent(currentEvent.value.event_id, {
      created_at: currentEvent.value.created_at,
      replay_reason: replayReason.value,
      replay_mode: replayMode.value,
      ...(replayMode.value === 'RETRY_AT' ? { next_retry_at: nextRetryAt.value } : {}),
    })
    message.success('已提交重放，事件将重新尝试投递')
    replayModalVisible.value = false
    await firstLoad()
  } catch {
    // error handled by interceptor
  } finally {
    replaySubmitting.value = false
  }
}

onMounted(firstLoad)
</script>

<style scoped>
.page-container {
  padding: 16px;
}
</style>
