<template>
  <div class="page-container">
    <a-page-header title="任务详情" @back="$router.back()" />

    <a-spin :spinning="loading">
      <template v-if="detail">
        <a-card title="基本信息" :bordered="false">
          <a-descriptions :column="2" bordered>
            <a-descriptions-item label="任务 ID">{{ detail.task_id }}</a-descriptions-item>
            <a-descriptions-item label="患者 ID">{{ detail.patient_id }}</a-descriptions-item>
            <a-descriptions-item label="状态">
              <a-tag :color="statusColors[detail.status]">
                {{ statusLabels[detail.status] ?? detail.status }}
              </a-tag>
            </a-descriptions-item>
            <a-descriptions-item label="来源">{{ detail.source }}</a-descriptions-item>
            <a-descriptions-item label="上报人">{{ detail.reported_by }}</a-descriptions-item>
            <a-descriptions-item label="事件时间">{{ detail.event_time }}</a-descriptions-item>
          </a-descriptions>
        </a-card>

        <!-- 操作区（仅 ACTIVE 状态可操作） -->
        <a-card
          title="操作"
          :bordered="false"
          style="margin-top: 12px"
          v-if="detail.status === 'ACTIVE'"
        >
          <a-space>
            <!-- 重推通知 -->
            <a-popconfirm
              title="请填写重推原因"
              :visible="retryVisible"
              :ok-button-props="{ disabled: retryReason.length < 5 }"
              @confirm="doRetryNotify"
              @cancel="retryVisible = false"
            >
              <template #description>
                <a-textarea
                  v-model:value="retryReason"
                  :rows="3"
                  placeholder="原因（5-256 字）"
                  :maxlength="256"
                  style="width: 280px; margin-top: 8px"
                />
              </template>
              <a-button @click="retryVisible = true" :loading="retrying">重推通知</a-button>
            </a-popconfirm>

            <!-- 强制关闭（仅 SUPERADMIN） -->
            <template v-if="isSuperAdmin">
              <a-popconfirm
                title="强制关闭确认"
                :visible="forceVisible"
                :ok-button-props="{ disabled: forceReason.length < 5, danger: true }"
                @confirm="doForceClose"
                @cancel="forceVisible = false"
              >
                <template #description>
                  <a-textarea
                    v-model:value="forceReason"
                    :rows="3"
                    placeholder="关闭原因（5-256 字）"
                    :maxlength="256"
                    style="width: 280px; margin-top: 8px"
                  />
                </template>
                <a-button danger @click="forceVisible = true" :loading="forcing">强制关闭</a-button>
              </a-popconfirm>
            </template>
          </a-space>
        </a-card>
      </template>
    </a-spin>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  getTaskDetail,
  retryNotify,
  forceCloseTask,
  type TaskStatus,
  type TaskItem,
} from '@/api/tasks'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const isSuperAdmin = authStore.isSuperAdmin

const taskId = route.params.taskId as string
const detail = ref<TaskItem | null>(null)
const loading = ref(false)

const retryVisible = ref(false)
const retryReason = ref('')
const retrying = ref(false)

const forceVisible = ref(false)
const forceReason = ref('')
const forcing = ref(false)

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

async function load() {
  loading.value = true
  try {
    detail.value = await getTaskDetail(taskId)
  } catch {
    message.error('加载任务详情失败')
    router.back()
  } finally {
    loading.value = false
  }
}

async function doRetryNotify() {
  retrying.value = true
  try {
    await retryNotify(taskId, { reason: retryReason.value })
    message.success('已重推通知')
    retryVisible.value = false
    retryReason.value = ''
    await load()
  } catch {
    // error handled by interceptor
  } finally {
    retrying.value = false
  }
}

async function doForceClose() {
  forcing.value = true
  try {
    await forceCloseTask(taskId, { close_type: 'ADMIN_CLOSE', reason: forceReason.value })
    message.success('任务已强制关闭')
    forceVisible.value = false
    forceReason.value = ''
    await load()
  } catch {
    // error handled by interceptor
  } finally {
    forcing.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.page-container {
  padding: 16px;
}
</style>
