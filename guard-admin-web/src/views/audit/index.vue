<template>
  <div class="page-container">
    <!-- 安全指标卡片 -->
    <a-row :gutter="16" style="margin-bottom: 16px">
      <a-col :span="6">
        <a-card :loading="metricsLoading" :bordered="false">
          <a-statistic
            title="登录失败次数（近 24h）"
            :value="secMetrics?.failed_login_count ?? '—'"
            value-style="color: #cf1322"
          />
        </a-card>
      </a-col>
      <a-col :span="6">
        <a-card :loading="metricsLoading" :bordered="false">
          <a-statistic
            title="高风险操作次数（近 24h）"
            :value="secMetrics?.risk_operation_count ?? '—'"
            value-style="color: #faad14"
          />
        </a-card>
      </a-col>
      <a-col :span="6">
        <a-card :loading="metricsLoading" :bordered="false">
          <a-statistic title="封禁用户数（近 24h）" :value="secMetrics?.banned_user_count ?? '—'" />
        </a-card>
      </a-col>
      <a-col :span="6">
        <a-card :loading="metricsLoading" :bordered="false">
          <a-statistic
            title="验证码触发次数（近 24h）"
            :value="secMetrics?.captcha_trigger_count ?? '—'"
          />
        </a-card>
      </a-col>
    </a-row>

    <a-card :bordered="false">
      <!-- 筛选栏 -->
      <a-space wrap style="margin-bottom: 12px">
        <a-input
          v-model:value="params.trace_id"
          placeholder="Trace ID 精确搜索"
          allow-clear
          style="width: 220px"
          @press-enter="firstLoad"
        />
        <a-input
          v-model:value="params.user_id"
          placeholder="操作人 ID"
          allow-clear
          style="width: 160px"
          @press-enter="firstLoad"
        />
        <a-select
          v-model:value="params.module"
          placeholder="模块"
          allow-clear
          style="width: 160px"
          @change="firstLoad"
        >
          <a-select-option value="AUTH">鉴权</a-select-option>
          <a-select-option value="TASK">救援任务</a-select-option>
          <a-select-option value="CLUE">线索</a-select-option>
          <a-select-option value="MATERIAL_ORDER">物资工单</a-select-option>
          <a-select-option value="TAG">标签</a-select-option>
          <a-select-option value="USER">用户</a-select-option>
          <a-select-option value="CONFIG">配置</a-select-option>
          <a-select-option value="DEAD_LETTER">死信队列</a-select-option>
        </a-select>
        <a-button type="primary" @click="firstLoad">查询</a-button>
        <a-button @click="resetFilters">重置</a-button>
      </a-space>

      <a-table
        :columns="columns"
        :data-source="items"
        :loading="loading"
        :pagination="false"
        row-key="log_id"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'result'">
            <a-tag
              :color="
                record.result === 'SUCCESS'
                  ? 'success'
                  : record.result === 'FAIL'
                    ? 'error'
                    : 'default'
              "
            >
              {{ record.result }}
            </a-tag>
          </template>
        </template>
      </a-table>

      <!-- 加载更多 (cursor pagination) -->
      <div style="text-align: center; margin-top: 16px">
        <a-button v-if="hasNext" :loading="loading" @click="loadMore">加载更多</a-button>
        <span v-else-if="items.length > 0" style="color: #999">已加载全部</span>
      </div>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { getAuditLogs, type AuditLogItem, type AuditLogParams } from '@/api/audit'
import { getSecurityMetrics, type SecurityMetrics } from '@/api/dashboard'

const secMetrics = ref<SecurityMetrics | null>(null)
const metricsLoading = ref(false)

async function loadSecurityMetrics() {
  metricsLoading.value = true
  try {
    secMetrics.value = await getSecurityMetrics()
  } catch {
    // partial degradation — metrics card shows '—'
  } finally {
    metricsLoading.value = false
  }
}

const params = reactive<AuditLogParams>({
  page_size: 30,
})
const items = ref<AuditLogItem[]>([])
const cursor = ref<string | null>(null)
const hasNext = ref(false)
const loading = ref(false)

const columns = [
  { title: '日志 ID', dataIndex: 'log_id', key: 'log_id', width: 180 },
  { title: '模块', dataIndex: 'module', key: 'module', width: 120 },
  { title: '操作', dataIndex: 'action', key: 'action', width: 200 },
  { title: '操作人', dataIndex: 'operator_user_id', key: 'operator_user_id', width: 120 },
  { title: '结果', dataIndex: 'result', key: 'result', width: 80 },
  { title: 'Trace ID', dataIndex: 'trace_id', key: 'trace_id', width: 200, ellipsis: true },
  { title: '时间', dataIndex: 'created_at', key: 'created_at', width: 180 },
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
    const p: AuditLogParams = { ...params }
    if (cursor.value) p.cursor = cursor.value
    const res = await getAuditLogs(p)
    items.value = [...items.value, ...res.items]
    cursor.value = res.next_cursor ?? null
    hasNext.value = res.has_next
  } catch {
    message.error('加载审计日志失败')
  } finally {
    loading.value = false
  }
}

function resetFilters() {
  params.trace_id = undefined
  params.user_id = undefined
  params.module = undefined
  params.action = undefined
  firstLoad()
}

onMounted(() => {
  loadSecurityMetrics()
  firstLoad()
})
</script>

<style scoped>
.page-container {
  padding: 16px;
}
</style>
