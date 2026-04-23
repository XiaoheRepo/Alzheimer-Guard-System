<!-- src/views/admin/AuditLogView.vue / P-10 -->
<script setup lang="ts">
import { reactive, ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import { listAuditLogs, exportAuditLogs, type AuditLogItem } from '@/api/admin'
import PageHeader from '@/components/common/PageHeader.vue'
import ProTable from '@/components/common/ProTable.vue'
import PermissionButton from '@/components/domain/PermissionButton.vue'
import CopyableText from '@/components/common/CopyableText.vue'
import { fmtDateTime } from '@/utils/format'
import { downloadBlob } from '@/utils/download'
import type { ApiError } from '@/utils/request'

const { t } = useI18n()

const filter = reactive({
  module: '',
  action: '',
  action_source: '',
  operator_user_id: '',
  risk_level: '',
})
const cursors = ref<string[]>([''])
const cursorIdx = ref(0)
const data = ref<AuditLogItem[]>([])
const loading = ref(false)
const exporting = ref(false)
const drawerOpen = ref(false)
const current = ref<AuditLogItem | null>(null)
const nextCursor = ref<string>('')

const columns = computed(() => [
  { title: t('page.log.col.time'), key: 'created_at', width: 180 },
  { title: t('page.log.col.module'), dataIndex: 'module', width: 120 },
  { title: t('page.log.col.operator'), key: 'operator', width: 160 },
  { title: t('page.log.col.action'), dataIndex: 'action', width: 200 },
  { title: t('page.log.col.result'), key: 'result', width: 110 },
  { title: t('page.log.col.riskLevel'), key: 'risk_level', width: 110 },
  { title: t('page.log.col.traceId'), key: 'trace_id', width: 160 },
  { title: t('common.operation'), key: 'detail', width: 100, fixed: 'right' },
])

async function load() {
  loading.value = true
  try {
    const res = await listAuditLogs({
      module: filter.module || undefined,
      action: filter.action || undefined,
      action_source: filter.action_source || undefined,
      operator_user_id: filter.operator_user_id || undefined,
      risk_level: filter.risk_level || undefined,
      cursor: cursors.value[cursorIdx.value] || undefined,
      page_size: 50,
    })
    data.value = res.items || []
    nextCursor.value = res.next_cursor || ''
  } finally {
    loading.value = false
  }
}

function doSearch() {
  cursors.value = ['']
  cursorIdx.value = 0
  load()
}

function nextPage() {
  if (!nextCursor.value) return
  cursors.value.push(nextCursor.value)
  cursorIdx.value += 1
  load()
}
function prevPage() {
  if (cursorIdx.value === 0) return
  cursorIdx.value -= 1
  load()
}

function openDetail(r: AuditLogItem) {
  current.value = r
  drawerOpen.value = true
}

async function onExport() {
  exporting.value = true
  try {
    // 后端 /api/v1/admin/logs/export 必填 start_at / end_at（API §3.6.21）。
    // 默认导出最近 30 天，其他可选过滤来自当前面板。
    const endAt = new Date()
    const startAt = new Date(endAt.getTime() - 30 * 24 * 3600 * 1000)
    const operatorId = filter.operator_user_id ? Number(filter.operator_user_id) : undefined
    const blob = await exportAuditLogs({
      start_at: startAt.toISOString(),
      end_at: endAt.toISOString(),
      operator_id: Number.isFinite(operatorId) ? operatorId : undefined,
      action: filter.action || undefined,
      resource_type: filter.module || undefined,
      format: 'csv',
    })
    downloadBlob(blob, `audit-log-${endAt.toISOString().slice(0, 10)}.csv`)
  } catch (e) {
    message.error((e as ApiError)?.message || t('error.UNKNOWN'))
  } finally {
    exporting.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="page-container">
    <PageHeader :title="t('menu.adminLog')">
      <template #extra>
        <PermissionButton
          :roles="['SUPER_ADMIN']"
          type="primary"
          :loading="exporting"
          :disabled="exporting"
          @click="onExport"
        >
          {{ t('page.log.export') }}
        </PermissionButton>
      </template>
    </PageHeader>

    <ProTable
      :columns="columns"
      :data-source="data"
      :loading="loading"
      :row-key="(r: AuditLogItem) => String(r.id)"
      :pagination="false"
      :scroll="{ x: 1200 }"
    >
      <template #filter>
        <a-form layout="inline">
          <a-form-item :label="t('page.log.col.module')">
            <a-select v-model:value="filter.module" allow-clear style="width: 140px">
              <a-select-option value="TASK">TASK</a-select-option>
              <a-select-option value="CLUE">CLUE</a-select-option>
              <a-select-option value="PROFILE">PROFILE</a-select-option>
              <a-select-option value="MAT">MAT</a-select-option>
              <a-select-option value="AI">AI</a-select-option>
              <a-select-option value="GOV">GOV</a-select-option>
              <a-select-option value="NOTIFICATION">NOTIFICATION</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item :label="t('page.log.col.action')">
            <a-input v-model:value="filter.action" allow-clear />
          </a-form-item>
          <a-form-item :label="t('page.log.col.operator')">
            <a-input v-model:value="filter.operator_user_id" allow-clear />
          </a-form-item>
          <a-form-item :label="t('page.log.col.riskLevel')">
            <a-select v-model:value="filter.risk_level" allow-clear style="width: 110px">
              <a-select-option value="LOW">LOW</a-select-option>
              <a-select-option value="MEDIUM">MEDIUM</a-select-option>
              <a-select-option value="HIGH">HIGH</a-select-option>
              <a-select-option value="CRITICAL">CRITICAL</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item>
            <a-button type="primary" @click="doSearch">{{ t('common.search') }}</a-button>
          </a-form-item>
        </a-form>
      </template>

      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'created_at'">
          {{ fmtDateTime(record.created_at) }}
        </template>
        <template v-else-if="column.key === 'operator'">
          {{ record.operator_username || record.operator_user_id || '-' }}
        </template>
        <template v-else-if="column.key === 'result'">
          <a-tag :color="record.result === 'SUCCESS' ? 'success' : 'error'">
            {{ record.result }}
          </a-tag>
        </template>
        <template v-else-if="column.key === 'risk_level'">
          <a-tag
            :color="
              { LOW: 'default', MEDIUM: 'warning', HIGH: 'orange', CRITICAL: 'error' }[
                record.risk_level
              ] ?? 'default'
            "
          >
            {{ record.risk_level }}
          </a-tag>
        </template>
        <template v-else-if="column.key === 'trace_id'">
          <CopyableText :text="record.trace_id" short />
        </template>
        <template v-else-if="column.key === 'detail'">
          <a-button type="link" size="small" @click="openDetail(record)">
            {{ t('common.detail') }}
          </a-button>
        </template>
      </template>
    </ProTable>

    <div style="margin-top: 12px; text-align: right">
      <a-space>
        <a-button :disabled="cursorIdx === 0" @click="prevPage">
          {{ t('common.prev') }}
        </a-button>
        <a-button :disabled="!nextCursor" type="primary" @click="nextPage">
          {{ t('common.next') }}
        </a-button>
      </a-space>
    </div>

    <a-drawer v-model:open="drawerOpen" width="720" :title="t('page.log.detail.title')">
      <template v-if="current">
        <a-descriptions bordered :column="1" size="small">
          <a-descriptions-item :label="t('page.log.col.time')">{{
            fmtDateTime(current.created_at)
          }}</a-descriptions-item>
          <a-descriptions-item :label="t('page.log.col.module')">{{
            current.module
          }}</a-descriptions-item>
          <a-descriptions-item :label="t('page.log.col.operator')">
            {{ current.operator_username || current.operator_user_id || '-' }}
          </a-descriptions-item>
          <a-descriptions-item :label="t('page.log.col.action')">{{
            current.action
          }}</a-descriptions-item>
          <a-descriptions-item :label="t('page.log.col.result')">{{
            current.result
          }}</a-descriptions-item>
          <a-descriptions-item :label="t('page.log.col.riskLevel')">{{
            current.risk_level
          }}</a-descriptions-item>
          <a-descriptions-item :label="t('page.log.col.traceId')">
            <CopyableText :text="current.trace_id" />
          </a-descriptions-item>
          <a-descriptions-item v-if="current.ip" :label="t('page.log.col.ip')">{{
            current.ip
          }}</a-descriptions-item>
          <a-descriptions-item v-if="current.detail" :label="t('page.log.col.detail')">
            <pre class="code-block">{{ current.detail }}</pre>
          </a-descriptions-item>
        </a-descriptions>
      </template>
    </a-drawer>
  </div>
</template>

<style scoped>
.code-block {
  background: var(--bg-layout);
  padding: 12px;
  border-radius: 6px;
  max-height: 280px;
  overflow: auto;
  font-size: 12px;
}
</style>
