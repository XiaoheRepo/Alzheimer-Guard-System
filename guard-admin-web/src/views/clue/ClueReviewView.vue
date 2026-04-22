<!-- src/views/clue/ClueReviewView.vue / P-04a -->
<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { listClues, type ClueListItem } from '@/api/clue'
import type { RiskLevel } from '@/types/enums'
import PageHeader from '@/components/common/PageHeader.vue'
import ProTable from '@/components/common/ProTable.vue'
import StatusBadge from '@/components/domain/StatusBadge.vue'
import ClueDetailDrawer from '@/components/domain/ClueDetailDrawer.vue'
import CopyableText from '@/components/common/CopyableText.vue'
import { fmtDateTime } from '@/utils/format'

const { t } = useI18n()

const filter = reactive({
  risk_level: undefined as RiskLevel | undefined,
  keyword: '',
})
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const data = ref<ClueListItem[]>([])
const loading = ref(false)

const drawerOpen = ref(false)
const currentClue = ref<ClueListItem | null>(null)

const columns = [
  { title: t('page.clue.col.clueId'), key: 'clue_id', width: 160 },
  { title: t('page.clue.col.taskId'), key: 'task_id', width: 160 },
  { title: t('page.clue.col.risk'), key: 'risk_level', width: 100 },
  { title: t('page.clue.col.confidence'), dataIndex: 'confidence', key: 'confidence', width: 100 },
  { title: t('page.clue.col.location'), dataIndex: 'location_name', key: 'location_name' },
  { title: t('page.clue.col.reportedAt'), key: 'reported_at', width: 180 },
  { title: t('common.action'), key: 'action', width: 100, fixed: 'right' },
]

async function load() {
  loading.value = true
  try {
    const res = await listClues({
      review_state: 'PENDING',
      risk_level: filter.risk_level,
      keyword: filter.keyword || undefined,
      page: pagination.current,
      size: pagination.pageSize,
    })
    data.value = res.items || []
    pagination.total = res.total || 0
  } finally {
    loading.value = false
  }
}

function openDrawer(r: ClueListItem) {
  currentClue.value = r
  drawerOpen.value = true
}

function onChange(p: { current?: number; pageSize?: number }) {
  pagination.current = p.current || 1
  pagination.pageSize = p.pageSize || 20
  load()
}

onMounted(load)
</script>

<template>
  <div class="page-container">
    <PageHeader :title="t('menu.clueReview')" :sub-title="t('page.clue.review.sub')" />
    <ProTable
      :columns="columns"
      :data-source="data"
      :loading="loading"
      :row-key="(r: ClueListItem) => r.clue_id"
      :pagination="{ ...pagination, showSizeChanger: true }"
      :scroll="{ x: 1100 }"
      @change="onChange"
    >
      <template #filter>
        <a-form layout="inline">
          <a-form-item :label="t('page.clue.col.risk')">
            <a-select v-model:value="filter.risk_level" allow-clear style="width: 160px">
              <a-select-option value="LOW">{{ t('field.riskLevel.LOW') }}</a-select-option>
              <a-select-option value="MEDIUM">{{ t('field.riskLevel.MEDIUM') }}</a-select-option>
              <a-select-option value="HIGH">{{ t('field.riskLevel.HIGH') }}</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item :label="t('common.keyword')">
            <a-input v-model:value="filter.keyword" allow-clear />
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
          </a-form-item>
        </a-form>
      </template>

      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'clue_id'">
          <CopyableText :text="record.clue_id" short />
        </template>
        <template v-else-if="column.key === 'task_id'">
          <a @click="$router.push(`/tasks/${record.task_id}`)">
            {{ record.task_id.slice(0, 8) }}…
          </a>
        </template>
        <template v-else-if="column.key === 'risk_level'">
          <StatusBadge kind="riskLevel" :value="record.risk_level" />
        </template>
        <template v-else-if="column.key === 'reported_at'">
          {{ fmtDateTime(record.reported_at) }}
        </template>
        <template v-else-if="column.key === 'action'">
          <a-button type="link" size="small" @click="openDrawer(record)">
            {{ t('common.review') }}
          </a-button>
        </template>
      </template>
    </ProTable>

    <ClueDetailDrawer v-model:open="drawerOpen" :clue="currentClue" @success="load" />
  </div>
</template>
