<template>
  <div class="page-container">
    <a-card :bordered="false">
      <a-space style="margin-bottom: 12px">
        <a-select v-model:value="selectedScope" style="width: 160px" @change="load">
          <a-select-option value="">全部（默认）</a-select-option>
          <a-select-option value="public">公共（public）</a-select-option>
          <a-select-option value="ops">运维（ops）</a-select-option>
          <a-select-option v-if="isSuperAdmin" value="security">安全（security）</a-select-option>
          <a-select-option v-if="isSuperAdmin" value="ai_policy"
            >AI 策略（ai_policy）</a-select-option
          >
        </a-select>
        <a-button @click="load" :loading="loading">刷新</a-button>
      </a-space>

      <a-spin :spinning="loading">
        <a-table :columns="columns" :data-source="items" :pagination="false" row-key="config_key">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'action'">
              <a-button v-if="isSuperAdmin" type="link" size="small" @click="openEdit(record)"
                >修改</a-button
              >
              <span v-else style="color: #999">—</span>
            </template>
          </template>
        </a-table>
      </a-spin>
    </a-card>

    <!-- 修改配置弹窗（仅 SUPERADMIN） -->
    <a-modal
      v-model:open="editModalVisible"
      title="修改配置项"
      :ok-button-props="{ loading: editSubmitting, disabled: editReason.length < 5 }"
      @ok="submitEdit"
    >
      <a-form layout="vertical">
        <a-form-item label="配置键">
          <a-input :value="editItem?.config_key" disabled />
        </a-form-item>
        <!-- Diff 预览 -->
        <a-alert
          v-if="editValue !== editOriginalValue"
          type="info"
          show-icon
          style="margin-bottom: 12px"
        >
          <template #description>
            <a-descriptions :column="1" size="small" bordered>
              <a-descriptions-item label="Scope">{{ editScope || '全部' }}</a-descriptions-item>
              <a-descriptions-item label="旧值">
                <span style="color: #cf1322; text-decoration: line-through">{{
                  editOriginalValue
                }}</span>
              </a-descriptions-item>
              <a-descriptions-item label="新值">
                <span style="color: #389e0d">{{ editValue }}</span>
              </a-descriptions-item>
            </a-descriptions>
          </template>
        </a-alert>
        <a-form-item label="配置值" required>
          <a-input v-model:value="editValue" />
        </a-form-item>
        <a-form-item label="修改原因（5-256 字）" required>
          <a-textarea v-model:value="editReason" :rows="3" :maxlength="256" show-count />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { getConfig, updateConfig, type ConfigScope, type ConfigItem } from '@/api/config'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const isSuperAdmin = authStore.isSuperAdmin

const selectedScope = ref<ConfigScope | ''>('')
const items = ref<ConfigItem[]>([])
const loading = ref(false)

const columns = [
  { title: '配置键', dataIndex: 'config_key', key: 'config_key', width: 280, ellipsis: true },
  { title: '配置值', dataIndex: 'config_value', key: 'config_value' },
  { title: '最近更新', dataIndex: 'updated_at', key: 'updated_at', width: 180 },
  { title: '操作', key: 'action', width: 80, fixed: 'right' as const },
]

async function load() {
  loading.value = true
  try {
    const scope = selectedScope.value as ConfigScope | undefined
    const res = await getConfig(scope || undefined)
    items.value = res.items
  } catch {
    message.error('加载配置失败')
  } finally {
    loading.value = false
  }
}

// ── 修改配置 ──────────────────────────────────────────────────────
const editModalVisible = ref(false)
const editItem = ref<ConfigItem | null>(null)
const editScope = ref<ConfigScope | ''>('')
const editValue = ref('')
const editOriginalValue = ref('')
const editReason = ref('')
const editSubmitting = ref(false)

function openEdit(item: ConfigItem) {
  editItem.value = item
  editScope.value = selectedScope.value
  editValue.value = item.config_value
  editOriginalValue.value = item.config_value
  editReason.value = ''
  editModalVisible.value = true
}

async function submitEdit() {
  if (!editItem.value) return
  editSubmitting.value = true
  try {
    await updateConfig({
      config_key: editItem.value.config_key,
      config_value: editValue.value,
      reason: editReason.value,
    })
    message.success('配置已更新')
    editModalVisible.value = false
    await load()
  } catch {
    // error handled by interceptor
  } finally {
    editSubmitting.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.page-container {
  padding: 16px;
}
</style>
