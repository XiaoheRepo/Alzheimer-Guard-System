<template>
  <div class="page-container">
    <!-- 筛选栏 -->
    <a-card class="filter-card" :bordered="false">
      <a-space wrap>
        <a-select
          v-model:value="params.status"
          placeholder="任务状态"
          allow-clear
          style="width: 140px"
          @change="() => { params.page_no = 1; load() }"
        >
          <a-select-option value="ACTIVE">处理中</a-select-option>
          <a-select-option value="RESOLVED">已处置</a-select-option>
          <a-select-option value="FALSE_ALARM">误报</a-select-option>
        </a-select>
        <a-select
          v-model:value="params.source"
          placeholder="上报来源"
          allow-clear
          style="width: 140px"
          @change="() => { params.page_no = 1; load() }"
        >
          <a-select-option value="APP">App</a-select-option>
          <a-select-option value="MINI_PROGRAM">小程序</a-select-option>
          <a-select-option value="ADMIN_PORTAL">管理端</a-select-option>
        </a-select>
        <a-button type="primary" @click="() => { params.page_no = 1; load() }">查询</a-button>
        <a-button @click="resetFilters">重置</a-button>
      </a-space>
    </a-card>

    <!-- 数据表格 -->
    <a-card :bordered="false" style="margin-top: 12px">
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
        row-key="task_id"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-tag :color="statusColors[record.status as TaskStatus]">
              {{ statusLabels[record.status as TaskStatus] ?? record.status }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-button type="link" size="small" @click="viewDetail(record.task_id)">
              详情
            </a-button>
          </template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { getTaskList, type TaskStatus, type TaskListParams, type TaskItem } from '@/api/tasks'

const router = useRouter()

const params = reactive<TaskListParams & { page_no: number; page_size: number }>({
  page_no: 1,
  page_size: 20,
})
const items = ref<TaskItem[]>([])
const total = ref(0)
const loading = ref(false)

const statusLabels: Record<TaskStatus, string> = {
  ACTIVE: '处理中',
  RESOLVED: '已处置',
  FALSE_ALARM: '误报',
  UNKNOWN: '未知',
}
const statusColors: Record<TaskStatus, string> = {
  ACTIVE: 'processing',
  RESOLVED: 'success',
  FALSE_ALARM: 'default',
  UNKNOWN: 'default',
}

const columns = [
  { title: '任务 ID', dataIndex: 'task_id', key: 'task_id', width: 180 },
  { title: '患者 ID', dataIndex: 'patient_id', key: 'patient_id', width: 120 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '来源', dataIndex: 'source', key: 'source', width: 120 },
  { title: '上报人', dataIndex: 'reported_by', key: 'reported_by', width: 120 },
  { title: '事件时间', dataIndex: 'event_time', key: 'event_time', width: 180 },
  { title: '操作', key: 'action', width: 80, fixed: 'right' as const },
]

async function load() {
  loading.value = true
  try {
    const res = await getTaskList(params)
    items.value = res.items
    total.value = res.total
  } catch {
    message.error('加载任务列表失败')
  } finally {
    loading.value = false
  }
}

function handleTableChange(pagination: { current: number; pageSize: number }) {
  params.page_no = pagination.current
  params.page_size = pagination.pageSize
  load()
}

function resetFilters() {
  params.status = undefined
  params.source = undefined
  params.page_no = 1
  load()
}

function viewDetail(taskId: string) {
  router.push(`/admin/tasks/${taskId}`)
}

onMounted(load)
</script>

<style scoped>
.page-container {
  padding: 16px;
}
.filter-card {
  background: #fff;
}
</style>
