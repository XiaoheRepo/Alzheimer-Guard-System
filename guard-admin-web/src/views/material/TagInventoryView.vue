<!-- src/views/material/TagInventoryView.vue / P-07a -->
<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  getInventorySummary,
  batchGenerate,
  type TagInventorySummary,
  type TagInventoryRow,
} from '@/api/tag'
import PageHeader from '@/components/common/PageHeader.vue'
import PermissionButton from '@/components/domain/PermissionButton.vue'
import type { ApiError } from '@/utils/request'

const { t } = useI18n()
const router = useRouter()

const summary = ref<TagInventorySummary | null>(null)
const loading = ref(false)
const genDlg = reactive({
  open: false,
  tag_type: 'QR_CODE' as 'QR_CODE' | 'NFC',
  quantity: 100,
  submitting: false,
})

// 按 tag_type 分组求和；KPI 卡片显示总览（QR_CODE + NFC 合计）
const totals = computed(() => {
  const acc = { unbound: 0, allocated: 0, bound: 0, suspected_lost: 0, lost: 0, voided: 0 }
  const rows = summary.value?.summary ?? []
  for (const r of rows) {
    acc.unbound += r.unbound ?? 0
    acc.allocated += r.allocated ?? 0
    acc.bound += r.bound ?? 0
    acc.suspected_lost += r.suspected_lost ?? 0
    acc.lost += r.lost ?? 0
    acc.voided += r.voided ?? 0
  }
  return acc
})

const rows = computed<TagInventoryRow[]>(() => summary.value?.summary ?? [])

async function load() {
  loading.value = true
  try {
    summary.value = await getInventorySummary()
  } finally {
    loading.value = false
  }
}

onMounted(load)

async function onGenSubmit() {
  if (!genDlg.quantity || genDlg.quantity < 1 || genDlg.quantity > 10000) {
    message.warning(t('page.tag.gen.countRange'))
    return
  }
  genDlg.submitting = true
  try {
    const res = await batchGenerate({
      tag_type: genDlg.tag_type,
      quantity: genDlg.quantity,
      request_time: new Date().toISOString(),
    })
    message.success(t('common.success'))
    genDlg.open = false
    if (res.job_id) router.push(`/tags/batch-jobs/${res.job_id}`)
    else load()
  } catch (e) {
    message.error((e as ApiError)?.message || t('error.UNKNOWN'))
  } finally {
    genDlg.submitting = false
  }
}

// 必须用 computed，否则切换语言后 label 不会刷新
const kpis = computed(() => [
  { key: 'unbound' as const, label: t('field.tagState.UNBOUND'), color: '#999' },
  { key: 'allocated' as const, label: t('field.tagState.ALLOCATED'), color: '#0EA5E9' },
  { key: 'bound' as const, label: t('field.tagState.BOUND'), color: '#52c41a' },
  { key: 'suspected_lost' as const, label: t('field.tagState.SUSPECTED_LOST'), color: '#faad14' },
  { key: 'lost' as const, label: t('field.tagState.LOST'), color: '#ff4d4f' },
  { key: 'voided' as const, label: t('field.tagState.VOIDED'), color: '#666' },
])
</script>

<template>
  <div class="page-container">
    <PageHeader :title="t('menu.tagInventory')">
      <template #extra>
        <a-button :loading="loading" @click="load">{{ t('common.refresh') }}</a-button>
        <PermissionButton type="primary" :roles="['SUPER_ADMIN']" @click="genDlg.open = true">
          {{ t('page.tag.gen.btn') }}
        </PermissionButton>
      </template>
    </PageHeader>

    <a-row :gutter="16">
      <a-col v-for="k in kpis" :key="k.key" :xs="12" :md="4">
        <a-card>
          <div class="text-muted">{{ k.label }}</div>
          <div class="kpi-value" :style="{ color: k.color }">
            {{ totals[k.key] ?? 0 }}
          </div>
        </a-card>
      </a-col>
    </a-row>

    <a-card style="margin-top: 16px" :title="t('page.tag.byType')">
      <a-table
        :data-source="rows"
        :pagination="false"
        :loading="loading"
        row-key="tag_type"
        size="middle"
        :columns="[
          { title: t('page.tag.gen.type'), dataIndex: 'tag_type', width: 120 },
          { title: t('field.tagState.total'), dataIndex: 'total', width: 100 },
          { title: t('field.tagState.UNBOUND'), dataIndex: 'unbound', width: 100 },
          { title: t('field.tagState.ALLOCATED'), dataIndex: 'allocated', width: 100 },
          { title: t('field.tagState.BOUND'), dataIndex: 'bound', width: 100 },
          { title: t('field.tagState.SUSPECTED_LOST'), dataIndex: 'suspected_lost', width: 100 },
          { title: t('field.tagState.LOST'), dataIndex: 'lost', width: 100 },
          { title: t('field.tagState.VOIDED'), dataIndex: 'voided', width: 100 },
        ]"
      />
    </a-card>

    <a-modal
      v-model:open="genDlg.open"
      :title="t('page.tag.gen.title')"
      :confirm-loading="genDlg.submitting"
      :ok-text="t('common.submit')"
      :cancel-text="t('common.cancel')"
      @ok="onGenSubmit"
    >
      <a-form layout="vertical">
        <a-form-item :label="t('page.tag.gen.type')" required>
          <a-radio-group v-model:value="genDlg.tag_type">
            <a-radio value="QR_CODE">QR_CODE</a-radio>
            <a-radio value="NFC">NFC</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item :label="t('page.tag.gen.count')" required>
          <a-input-number
            v-model:value="genDlg.quantity"
            :min="1"
            :max="10000"
            style="width: 100%"
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<style scoped>
.kpi-value {
  font-size: 28px;
  font-weight: 700;
  margin-top: 4px;
}
</style>
