<!-- src/views/patient/PatientListView.vue / P-05 / P-15 -->
<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { listAdminPatients, type AdminPatientListItem } from '@/api/patient'
import type { PatientStatus } from '@/types/enums'
import PageHeader from '@/components/common/PageHeader.vue'
import ProTable from '@/components/common/ProTable.vue'
import StatusBadge from '@/components/domain/StatusBadge.vue'
import CopyableText from '@/components/common/CopyableText.vue'
import { fmtDateTime } from '@/utils/format'

const { t } = useI18n()
const router = useRouter()

const filter = reactive({
  status: undefined as PatientStatus | undefined,
  keyword: '',
})
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const data = ref<AdminPatientListItem[]>([])
const loading = ref(false)

const columns = [
  { title: t('page.patient.col.profileNo'), key: 'profile_no', width: 160 },
  { title: t('page.patient.col.name'), dataIndex: 'patient_name', width: 120 },
  { title: t('page.patient.col.status'), key: 'status', width: 120 },
  { title: t('page.patient.col.primaryGuardian'), key: 'primary_guardian', width: 180 },
  { title: t('page.patient.col.guardianCount'), dataIndex: 'guardian_count', width: 100 },
  { title: t('page.patient.col.tagCount'), dataIndex: 'bound_tag_count', width: 100 },
  { title: t('page.patient.col.createdAt'), key: 'created_at', width: 180 },
  { title: t('common.action'), key: 'action', width: 100, fixed: 'right' },
]

async function load() {
  loading.value = true
  try {
    const res = await listAdminPatients({
      status: filter.status,
      keyword: filter.keyword || undefined,
      page: pagination.current,
      size: pagination.pageSize,
    })
    const items = (res as { items?: AdminPatientListItem[] }).items || []
    data.value = items
    pagination.total = (res as { total?: number }).total || items.length
  } finally {
    loading.value = false
  }
}

function onChange(p: { current?: number; pageSize?: number }) {
  pagination.current = p.current || 1
  pagination.pageSize = p.pageSize || 20
  load()
}

function goDetail(r: AdminPatientListItem) {
  router.push(`/patients/${r.patient_id}`)
}

onMounted(load)
</script>

<template>
  <div class="page-container">
    <PageHeader :title="t('menu.patient')" />
    <ProTable
      :columns="columns"
      :data-source="data"
      :loading="loading"
      :row-key="(r: AdminPatientListItem) => r.patient_id"
      :pagination="{ ...pagination, showSizeChanger: true }"
      :scroll="{ x: 1100 }"
      @change="onChange"
    >
      <template #filter>
        <a-form layout="inline">
          <a-form-item :label="t('page.patient.col.status')">
            <a-select v-model:value="filter.status" allow-clear style="width: 160px">
              <a-select-option value="NORMAL">{{
                t('field.patientStatus.NORMAL')
              }}</a-select-option>
              <a-select-option value="MISSING_PENDING">{{
                t('field.patientStatus.MISSING_PENDING')
              }}</a-select-option>
              <a-select-option value="MISSING">{{
                t('field.patientStatus.MISSING')
              }}</a-select-option>
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
        <template v-if="column.key === 'profile_no'">
          <CopyableText :text="record.profile_no || record.patient_id" short />
        </template>
        <template v-else-if="column.key === 'status'">
          <StatusBadge kind="patientStatus" :value="record.status" />
        </template>
        <template v-else-if="column.key === 'primary_guardian'">
          {{ record.primary_guardian?.nickname || '-' }}
          <span v-if="record.primary_guardian?.phone" class="text-muted">
            ({{ record.primary_guardian.phone }})
          </span>
        </template>
        <template v-else-if="column.key === 'created_at'">
          {{ fmtDateTime(record.created_at) }}
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
