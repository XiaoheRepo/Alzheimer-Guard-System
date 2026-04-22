<!-- src/views/admin/AuditLogView.vue / P-10 -->
<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
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
  operator: '',
  action: '',
  resource_type: '',
  trace_id: '',
})
const cursors = ref<string[]>([''])
const cursorIdx = ref(0)
const data = ref<AuditLogItem[]>([])
const loading = ref(false)
const exporting = ref(false)
const drawerOpen = ref(false)
const current = ref<AuditLogItem | null>(null)
const nextCursor = ref<string>('')

const columns = [
  { title: t('page.log.columns.ts'), key: 'ts', width: 180 },
  { title: t('page.log.columns.operator'), key: 'operator', width: 160 },
  { title: t('page.log.columns.action'), dataIndex: 'action', width: 200 },
  { title: t('page.log.columns.resource'), key: 'resource', width: 200 },
  { title: t('page.log.columns.status'), dataIndex: 'status', width: 100 },
  { title: t('page.log.columns.traceId'), key: 'trace_id', width: 160 },
  { title: t('common.operation'), key: 'detail', width: 100, fixed: 'right' },
]

async function load() {
  loading.value = true
  try {
    const res = await listAuditLogs({
      operator: filter.operator || undefined,
      action: filter.action || undefined,
      resource_type: filter.resource_type || undefined,
      trace_id: filter.trace_id || undefined,
      cursor: cursors.value[cursorIdx.value] || undefined,
      size: 30,
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
    const blob = await exportAuditLogs({
      operator: filter.operator || undefined,
      action: filter.action || undefined,
      resource_type: filter.resource_type || undefined,
      trace_id: filter.trace_id || undefined,
    })
    downloadBlob(blob, `audit-log-${new Date().toISOString().slice(0, 10)}.csv`)
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
      :row-key="(r: AuditLogItem) => r.log_id"
      :pagination="false"
      :scroll="{ x: 1200 }"
    >
      <template #filter>
        <a-form layout="inline">
          <a-form-item :label="t('page.log.col.operator')">
            <a-input v-model:value="filter.operator" allow-clear />
          </a-form-item>
          <a-form-item :label="t('page.log.col.action')">
            <a-input v-model:value="filter.action" allow-clear />
          </a-form-item>
          <a-form-item :label="t('page.log.col.resource')">
            <a-input v-model:value="filter.resource_type" allow-clear />
          </a-form-item>
          <a-form-item :label="t('page.log.col.traceId')">
            <a-input v-model:value="filter.trace_id" allow-clear />
          </a-form-item>
          <a-form-item>
            <a-button type="primary" @click="doSearch">{{ t('common.search') }}</a-button>
          </a-form-item>
        </a-form>
      </template>

      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'ts'">
          {{ fmtDateTime(record.ts) }}
        </template>
        <template v-else-if="column.key === 'operator'">
          {{ record.operator_name || record.operator_id || '-' }}
          <span v-if="record.role" class="text-muted">({{ record.role }})</span>
        </template>
        <template v-else-if="column.key === 'resource'">
          <span v-if="record.resource_type">{{ record.resource_type }}</span>
          <CopyableText v-if="record.resource_id" :text="record.resource_id" short />
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
            fmtDateTime(current.ts)
          }}</a-descriptions-item>
          <a-descriptions-item :label="t('page.log.col.operator')">
            {{ current.operator_name || current.operator_id || '-' }}
          </a-descriptions-item>
          <a-descriptions-item :label="t('page.log.col.action')">{{
            current.action
          }}</a-descriptions-item>
          <a-descriptions-item :label="t('page.log.col.resource')">
            {{ current.resource_type }} / {{ current.resource_id }}
          </a-descriptions-item>
          <a-descriptions-item :label="t('page.log.col.traceId')">
            <CopyableText :text="current.trace_id" />
          </a-descriptions-item>
          <a-descriptions-item :label="t('page.log.col.status')">{{
            current.status
          }}</a-descriptions-item>
          <a-descriptions-item :label="t('page.log.col.duration')"
            >{{ current.duration_ms }} ms</a-descriptions-item
          >
        </a-descriptions>

        <a-divider>{{ t('page.log.col.request') }}</a-divider>
        <pre class="code-block">{{ JSON.stringify(current.request_body, null, 2) }}</pre>

        <a-divider>{{ t('page.log.col.response') }}</a-divider>
        <pre class="code-block">{{ JSON.stringify(current.response_body, null, 2) }}</pre>
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
