<template>
  <div class="page-container">
    <a-card title="线索复核队列（按风险分降序）" :bordered="false">
      <template #extra>
        <a-button @click="load" :loading="loading">刷新</a-button>
      </template>
      <a-table
        :columns="columns"
        :data-source="items"
        :loading="loading"
        :pagination="{
          current: params.page_no,
          pageSize: params.page_size,
          total: total,
          showSizeChanger: true,
          showTotal: (t: number) => `共 ${t} 条`,
        }"
        row-key="clue_id"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-tag :color="statusColors[record.status as ClueStatus]">
              {{ statusLabels[record.status as ClueStatus] ?? record.status }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'risk_score'">
            <a-progress
              :percent="record.risk_score"
              :stroke-color="
                record.risk_score >= 70
                  ? '#ff4d4f'
                  : record.risk_score >= 40
                    ? '#faad14'
                    : '#52c41a'
              "
              size="small"
            />
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a-button
                type="link"
                size="small"
                :disabled="record.status !== 'PENDING'"
                @click="openAction(record, 'override')"
                >确认</a-button
              >
              <a-button
                type="link"
                size="small"
                danger
                :disabled="record.status !== 'PENDING'"
                @click="openAction(record, 'reject')"
                >驳回</a-button
              >
              <a-tooltip title="当前版本仅保留治理审计入口，证据请求功能未开放">
                <a-button type="link" size="small" disabled>请求证据</a-button>
              </a-tooltip>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 操作弹窗 -->
    <a-modal
      v-model:open="modalVisible"
      :title="actionType === 'override' ? '确认线索（人工核实）' : '驳回线索'"
      :ok-button-props="{ loading: submitting, disabled: actionReason.length < 5 }"
      :ok-type="actionType === 'reject' ? 'danger' : 'primary'"
      @ok="submitAction"
    >
      <p>线索 ID：{{ currentClue?.clue_id }}</p>
      <p>风险分：{{ currentClue?.risk_score }}</p>
      <a-form layout="vertical">
        <a-form-item label="原因（必填，5-256 字）" required>
          <a-textarea
            v-model:value="actionReason"
            :rows="4"
            :maxlength="256"
            show-count
            placeholder="请填写操作原因"
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
  getClueReviewQueue,
  overrideClue,
  rejectClue,
  type ClueStatus,
  type ClueItem,
  type ClueReviewParams,
} from '@/api/clues'

const params = reactive<ClueReviewParams & { page_no: number; page_size: number }>({
  page_no: 1,
  page_size: 20,
})
const items = ref<ClueItem[]>([])
const total = ref(0)
const loading = ref(false)

const modalVisible = ref(false)
const actionType = ref<'override' | 'reject'>('override')
const currentClue = ref<ClueItem | null>(null)
const actionReason = ref('')
const submitting = ref(false)

const statusLabels: Record<ClueStatus, string> = {
  PENDING: '待复核',
  OVERRIDDEN: '已确认',
  REJECTED: '已驳回',
  UNKNOWN: '未知',
}
const statusColors: Record<ClueStatus, string> = {
  PENDING: 'warning',
  OVERRIDDEN: 'success',
  REJECTED: 'default',
  UNKNOWN: 'default',
}

const columns = [
  { title: '线索 ID', dataIndex: 'clue_id', key: 'clue_id', width: 180 },
  { title: '任务 ID', dataIndex: 'task_id', key: 'task_id', width: 180 },
  { title: '风险分', dataIndex: 'risk_score', key: 'risk_score', width: 140 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '上报人', dataIndex: 'reporter_user_id', key: 'reporter_user_id', width: 120 },
  { title: '创建时间', dataIndex: 'created_at', key: 'created_at', width: 180 },
  { title: '操作', key: 'action', width: 120, fixed: 'right' as const },
]

async function load() {
  loading.value = true
  try {
    const res = await getClueReviewQueue(params)
    items.value = res.items
    total.value = res.total
  } catch {
    message.error('加载线索队列失败')
  } finally {
    loading.value = false
  }
}

function handleTableChange(pagination: { current: number; pageSize: number }) {
  params.page_no = pagination.current
  params.page_size = pagination.pageSize
  load()
}

function openAction(clue: ClueItem, type: 'override' | 'reject') {
  currentClue.value = clue
  actionType.value = type
  actionReason.value = ''
  modalVisible.value = true
}

async function submitAction() {
  if (!currentClue.value) return
  submitting.value = true
  try {
    if (actionType.value === 'override') {
      await overrideClue(currentClue.value.clue_id, { reason: actionReason.value })
      message.success('线索已确认')
    } else {
      await rejectClue(currentClue.value.clue_id, { reason: actionReason.value })
      message.success('线索已驳回')
    }
    modalVisible.value = false
    await load()
  } catch {
    // error handled by interceptor
  } finally {
    submitting.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.page-container {
  padding: 16px;
}
</style>
