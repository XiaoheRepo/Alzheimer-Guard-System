<!-- src/views/material/TagInventoryView.vue / P-07a -->
<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  getInventorySummary,
  batchGenerate,
  type TagInventorySummary,
} from '@/api/tag'
import PageHeader from '@/components/common/PageHeader.vue'
import PermissionButton from '@/components/domain/PermissionButton.vue'
import type { ApiError } from '@/utils/request'

const { t } = useI18n()
const router = useRouter()

const summary = ref<TagInventorySummary | null>(null)
const loading = ref(false)
const genDlg = reactive({ open: false, count: 100, remark: '', submitting: false })

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
  if (!genDlg.count || genDlg.count < 1 || genDlg.count > 10000) {
    message.warning(t('page.tag.gen.countRange'))
    return
  }
  genDlg.submitting = true
  try {
    const res = await batchGenerate({ count: genDlg.count, remark: genDlg.remark || undefined })
    message.success(t('common.success'))
    genDlg.open = false
    router.push(`/tags/batch-jobs/${res.job_id}`)
  } catch (e) {
    message.error((e as ApiError)?.message || t('error.UNKNOWN'))
  } finally {
    genDlg.submitting = false
  }
}

const kpis = [
  { key: 'unbound', label: t('field.tagState.UNBOUND'), color: '#999' },
  { key: 'allocated', label: t('field.tagState.ALLOCATED'), color: '#0EA5E9' },
  { key: 'bound', label: t('field.tagState.BOUND'), color: '#52c41a' },
  { key: 'suspected_lost', label: t('field.tagState.SUSPECTED_LOST'), color: '#faad14' },
  { key: 'lost', label: t('field.tagState.LOST'), color: '#ff4d4f' },
  { key: 'voided', label: t('field.tagState.VOIDED'), color: '#666' },
]
</script>

<template>
  <div class="page-container">
    <PageHeader :title="t('menu.tagInventory')">
      <template #extra>
        <a-button :loading="loading" @click="load">{{ t('common.refresh') }}</a-button>
        <PermissionButton
          type="primary"
          :roles="['SUPER_ADMIN']"
          @click="genDlg.open = true"
        >
          {{ t('page.tag.gen.btn') }}
        </PermissionButton>
      </template>
    </PageHeader>

    <a-row :gutter="16">
      <a-col v-for="k in kpis" :key="k.key" :xs="12" :md="4">
        <a-card>
          <div class="text-muted">{{ k.label }}</div>
          <div class="kpi-value" :style="{ color: k.color }">
            {{ (summary && (summary[k.key] as number)) ?? 0 }}
          </div>
        </a-card>
      </a-col>
    </a-row>

    <a-modal
      v-model:open="genDlg.open"
      :title="t('page.tag.gen.title')"
      :confirm-loading="genDlg.submitting"
      :ok-text="t('common.submit')"
      :cancel-text="t('common.cancel')"
      @ok="onGenSubmit"
    >
      <a-form layout="vertical">
        <a-form-item :label="t('page.tag.gen.count')" required>
          <a-input-number
            v-model:value="genDlg.count"
            :min="1"
            :max="10000"
            style="width: 100%"
          />
        </a-form-item>
        <a-form-item :label="t('page.tag.gen.remark')">
          <a-input v-model:value="genDlg.remark" :maxlength="200" show-count />
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
