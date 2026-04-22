<!-- src/views/material/OrderListView.vue / P-06a -->
<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { listOrders, type MaterialOrder } from '@/api/material'
import type { OrderState } from '@/types/enums'
import PageHeader from '@/components/common/PageHeader.vue'
import ProTable from '@/components/common/ProTable.vue'
import StatusBadge from '@/components/domain/StatusBadge.vue'
import CopyableText from '@/components/common/CopyableText.vue'
import { fmtDateTime } from '@/utils/format'

const { t } = useI18n()
const router = useRouter()

const filter = reactive({
  state: undefined as OrderState | undefined,
  keyword: '',
})
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const data = ref<MaterialOrder[]>([])
const loading = ref(false)

const columns = [
  { title: t('page.material.col.orderId'), key: 'order_id', width: 160 },
  { title: t('page.material.col.patient'), key: 'patient', width: 140 },
  { title: t('page.material.col.state'), key: 'state', width: 130 },
  { title: t('page.material.col.items'), key: 'items' },
  { title: t('page.material.col.createdAt'), key: 'created_at', width: 180 },
  { title: t('common.action'), key: 'action', width: 100, fixed: 'right' },
]

async function load() {
  loading.value = true
  try {
    const res = await listOrders({
      state: filter.state,
      keyword: filter.keyword || undefined,
      page: pagination.current,
      size: pagination.pageSize,
    })
    const items = (res as { items?: MaterialOrder[] }).items || []
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

function goDetail(r: MaterialOrder) {
  router.push(`/material/orders/${r.order_id}`)
}

onMounted(load)
</script>

<template>
  <div class="page-container">
    <PageHeader :title="t('menu.materialOrder')" />
    <ProTable
      :columns="columns"
      :data-source="data"
      :loading="loading"
      :row-key="(r: MaterialOrder) => r.order_id"
      :pagination="{ ...pagination, showSizeChanger: true }"
      :scroll="{ x: 1100 }"
      @change="onChange"
    >
      <template #filter>
        <a-form layout="inline">
          <a-form-item :label="t('page.material.col.state')">
            <a-select v-model:value="filter.state" allow-clear style="width: 180px">
              <a-select-option value="PENDING_AUDIT">{{
                t('field.orderState.PENDING_AUDIT')
              }}</a-select-option>
              <a-select-option value="PENDING_SHIP">{{
                t('field.orderState.PENDING_SHIP')
              }}</a-select-option>
              <a-select-option value="SHIPPED">{{ t('field.orderState.SHIPPED') }}</a-select-option>
              <a-select-option value="RECEIVED">{{
                t('field.orderState.RECEIVED')
              }}</a-select-option>
              <a-select-option value="CANCELLED">{{
                t('field.orderState.CANCELLED')
              }}</a-select-option>
              <a-select-option value="EXCEPTION">{{
                t('field.orderState.EXCEPTION')
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
        <template v-if="column.key === 'order_id'">
          <CopyableText :text="record.order_id" short />
        </template>
        <template v-else-if="column.key === 'patient'">
          {{ record.patient?.patient_name || record.patient?.display_name || record.patient_id }}
        </template>
        <template v-else-if="column.key === 'state'">
          <StatusBadge kind="orderState" :value="record.state" />
        </template>
        <template v-else-if="column.key === 'items'">
          <span v-for="(it, i) in record.items" :key="i" style="margin-right: 8px">
            {{ it.material_type }} × {{ it.quantity }}
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
