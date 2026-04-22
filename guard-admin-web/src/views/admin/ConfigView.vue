<!-- src/views/admin/ConfigView.vue / P-09 / P-16 (Dict 复用) -->
<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import { listConfigs, updateConfig, type SysConfigItem } from '@/api/admin'
import PageHeader from '@/components/common/PageHeader.vue'
import ProTable from '@/components/common/ProTable.vue'
import PermissionButton from '@/components/domain/PermissionButton.vue'
import { fmtDateTime } from '@/utils/format'
import type { ApiError } from '@/utils/request'

const props = defineProps<{ group?: string; title?: string }>()

const { t } = useI18n()
const filter = reactive({ keyword: '' })
const data = ref<SysConfigItem[]>([])
const loading = ref(false)
const editDlg = reactive({
  open: false,
  item: null as SysConfigItem | null,
  value: '',
  reason: '',
  submitting: false,
})

const columns = [
  { title: t('page.config.columns.key'), dataIndex: 'config_key', key: 'config_key', width: 220 },
  { title: t('page.config.columns.value'), key: 'config_value' },
  { title: t('page.config.columns.type'), dataIndex: 'value_type', key: 'value_type', width: 100 },
  { title: t('page.config.columns.group'), dataIndex: 'group', key: 'group', width: 120 },
  { title: t('page.config.columns.updatedAt'), key: 'updated_at', width: 180 },
  { title: t('common.operation'), key: 'action', width: 100, fixed: 'right' },
]

async function load() {
  loading.value = true
  try {
    const params: Record<string, unknown> = { size: 200 }
    if (props.group) params.group = props.group
    if (filter.keyword) params.keyword = filter.keyword
    const res = await listConfigs(params)
    data.value = (res as { items?: SysConfigItem[] }).items || []
  } finally {
    loading.value = false
  }
}

onMounted(load)

function openEdit(item: SysConfigItem) {
  editDlg.item = item
  editDlg.value = item.config_value
  editDlg.reason = ''
  editDlg.open = true
}

async function onSave() {
  if (!editDlg.item) return
  if (editDlg.reason.trim().length < 5) {
    message.warning(t('page.config.reason') + ' ≥ 5')
    return
  }
  editDlg.submitting = true
  try {
    await updateConfig(editDlg.item.config_key, {
      value: editDlg.value,
      reason: editDlg.reason,
    })
    message.success(t('common.success'))
    editDlg.open = false
    await load()
  } catch (e) {
    message.error((e as ApiError)?.message || t('error.UNKNOWN'))
  } finally {
    editDlg.submitting = false
  }
}

const headerTitle = computed(() => props.title || t('menu.adminConfig'))
</script>

<template>
  <div class="page-container">
    <PageHeader :title="headerTitle" />
    <ProTable
      :columns="columns"
      :data-source="data"
      :loading="loading"
      :row-key="(r: SysConfigItem) => r.config_key"
      :pagination="false"
      :scroll="{ x: 900 }"
    >
      <template #filter>
        <a-form layout="inline">
          <a-form-item :label="t('common.keyword')">
            <a-input v-model:value="filter.keyword" allow-clear @pressEnter="load" />
          </a-form-item>
          <a-form-item>
            <a-button type="primary" @click="load">{{ t('common.search') }}</a-button>
          </a-form-item>
        </a-form>
      </template>
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'config_value'">
          <code>{{ record.config_value }}</code>
          <div v-if="record.description" class="text-muted" style="font-size: 12px">
            {{ record.description }}
          </div>
        </template>
        <template v-else-if="column.key === 'updated_at'">
          {{ fmtDateTime(record.updated_at) }}
        </template>
        <template v-else-if="column.key === 'action'">
          <PermissionButton
            type="link"
            size="small"
            :roles="['SUPER_ADMIN']"
            @click="openEdit(record)"
          >
            {{ t('common.edit') }}
          </PermissionButton>
        </template>
      </template>
    </ProTable>

    <a-modal
      v-model:open="editDlg.open"
      :title="t('page.config.editTitle')"
      :confirm-loading="editDlg.submitting"
      :ok-text="t('common.submit')"
      :cancel-text="t('common.cancel')"
      @ok="onSave"
    >
      <a-form v-if="editDlg.item" layout="vertical">
        <a-form-item :label="t('page.config.columns.key')">
          <a-typography-text code>{{ editDlg.item.config_key }}</a-typography-text>
        </a-form-item>
        <a-form-item :label="t('page.config.columns.value')">
          <a-typography-text code>{{ editDlg.item.config_value }}</a-typography-text>
        </a-form-item>
        <a-form-item :label="t('page.config.columns.value') + ' (new)'" required>
          <a-textarea v-model:value="editDlg.value" :rows="3" />
        </a-form-item>
        <a-form-item :label="t('page.config.reason')" required>
          <a-textarea v-model:value="editDlg.reason" :rows="3" :maxlength="500" show-count />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>
