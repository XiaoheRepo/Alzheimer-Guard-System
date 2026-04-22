<!-- src/views/task/TaskListView.vue / P-03a -->
<script setup lang="ts">
import { reactive, ref, onMounted, h } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { listTasks, type TaskListItem } from '@/api/task'
import type { TaskStatus } from '@/types/enums'
import PageHeader from '@/components/common/PageHeader.vue'
import ProTable from '@/components/common/ProTable.vue'
import StatusBadge from '@/components/domain/StatusBadge.vue'
import CopyableText from '@/components/common/CopyableText.vue'
import { fmtDateTime } from '@/utils/format'

const { t } = useI18n()
const router = useRouter()

const filter = reactive({
  status: undefined as TaskStatus | undefined,
  keyword: '',
  dateRange: undefined as [string, string] | undefined,
})
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const data = ref<TaskListItem[]>([])
const loading = ref(false)

const columns = [
  { title: t('page.task.col.taskNo'), dataIndex: 'task_no', key: 'task_no', width: 200 },
  { title: t('page.task.col.patient'), dataIndex: 'patient_name', key: 'patient_name', width: 140 },
  { title: t('page.task.col.status'), key: 'status', width: 130 },
  { title: t('page.task.col.source'), dataIndex: 'source', key: 'source', width: 120 },
  { title: t('page.task.col.createdAt'), key: 'created_at', width: 180 },
  { title: t('page.task.col.closedAt'), key: 'closed_at', width: 180 },
  { title: t('common.action'), key: 'action', width: 100, fixed: 'right' },
]

async function load() {
  loading.value = true
  try {
    const res = await listTasks({
      status: filter.status,
      keyword: filter.keyword || undefined,
      start_at: filter.dateRange?.[0],
      end_at: filter.dateRange?.[1],
      page: pagination.current,
      size: pagination.pageSize,
    })
    data.value = res.items || []
    pagination.total = res.total || 0
  } finally {
    loading.value = false
  }
}

function reset() {
  filter.status = undefined
  filter.keyword = ''
  filter.dateRange = undefined
  pagination.current = 1
  load()
}

function onChange(p: { current?: number; pageSize?: number }) {
  pagination.current = p.current || 1
  pagination.pageSize = p.pageSize || 20
  load()
}

function goDetail(r: TaskListItem) {
  router.push(`/tasks/${r.task_id}`)
}

onMounted(load)
</script>

<template>
  <div class="page-container">
    <PageHeader :title="t('menu.task')" />
    <ProTable
      :columns="columns"
      :data-source="data"
      :loading="loading"
      :row-key="(r: TaskListItem) => r.task_id"
      :pagination="{ ...pagination, showSizeChanger: true, showTotal: (tot: number) => `${tot}` }"
      :scroll="{ x: 1000 }"
      @change="onChange"
    >
      <template #filter>
        <a-form layout="inline">
          <a-form-item :label="t('page.task.col.status')">
            <a-select v-model:value="filter.status" allow-clear style="width: 180px">
              <a-select-option value="CREATED">{{ t('field.taskStatus.CREATED') }}</a-select-option>
              <a-select-option value="ACTIVE">{{ t('field.taskStatus.ACTIVE') }}</a-select-option>
              <a-select-option value="SUSTAINED">{{
                t('field.taskStatus.SUSTAINED')
              }}</a-select-option>
              <a-select-option value="CLOSED_FOUND">{{
                t('field.taskStatus.CLOSED_FOUND')
              }}</a-select-option>
              <a-select-option value="CLOSED_FALSE_ALARM">{{
                t('field.taskStatus.CLOSED_FALSE_ALARM')
              }}</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item :label="t('common.keyword')">
            <a-input
              v-model:value="filter.keyword"
              :placeholder="t('page.task.keywordPH')"
              allow-clear
            />
          </a-form-item>
          <a-form-item>
            <a-button
              type="primary"
              @click="
                () => {
                  pagination.current = 1
                  load()
                }
              "
            >
              {{ t('common.search') }}
            </a-button>
            <a-button style="margin-left: 8px" @click="reset">{{ t('common.reset') }}</a-button>
          </a-form-item>
        </a-form>
      </template>

      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'task_no'">
          <CopyableText :text="record.task_no || record.task_id" short />
        </template>
        <template v-else-if="column.key === 'status'">
          <StatusBadge kind="taskStatus" :value="record.status" />
        </template>
        <template v-else-if="column.key === 'created_at'">
          {{ fmtDateTime(record.created_at) }}
        </template>
        <template v-else-if="column.key === 'closed_at'">
          {{ fmtDateTime(record.closed_at) || '-' }}
        </template>
        <template v-else-if="column.key === 'action'">
          <a-button type="link" size="small" @click="goDetail(record)">
            {{ t('common.detail') }}
          </a-button>
        </template>
      </template>
    </ProTable>
  </div>
</template>
