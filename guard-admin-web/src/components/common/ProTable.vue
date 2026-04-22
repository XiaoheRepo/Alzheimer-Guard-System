<!-- src/components/common/ProTable.vue -->
<!-- 通用表格容器：filter 插槽、toolbar 插槽、加载/空态/错误态 -->
<script setup lang="ts" generic="T">
import { computed } from 'vue'
import type { TablePaginationConfig } from 'ant-design-vue'

const props = defineProps<{
  columns: Array<Record<string, unknown>>
  dataSource: T[]
  loading?: boolean
  rowKey: string | ((r: T) => string)
  pagination?: TablePaginationConfig | false
  scroll?: { x?: number | string; y?: number | string }
  size?: 'small' | 'middle' | 'large'
  error?: string | null
}>()

const emit = defineEmits<{
  (e: 'change', pagination: TablePaginationConfig, filters: Record<string, unknown>, sorter: unknown): void
  (e: 'retry'): void
}>()

const rowKeyFn = computed(() =>
  typeof props.rowKey === 'string'
    ? (props.rowKey as string)
    : (props.rowKey as (r: T) => string),
)
</script>

<template>
  <div class="pro-table">
    <div v-if="$slots.filter" class="filter-bar">
      <slot name="filter" />
    </div>
    <div v-if="$slots.toolbar" class="toolbar">
      <slot name="toolbar" />
    </div>

    <a-alert
      v-if="error"
      type="error"
      :message="error"
      show-icon
      style="margin-bottom: 12px"
    >
      <template #action>
        <a-button size="small" @click="emit('retry')">{{ $t('common.retry') }}</a-button>
      </template>
    </a-alert>

    <a-table
      :columns="columns"
      :data-source="dataSource"
      :loading="loading"
      :row-key="rowKeyFn"
      :pagination="pagination"
      :scroll="scroll"
      :size="size || 'middle'"
      @change="(p, f, s) => emit('change', p, f, s)"
    >
      <template v-for="name in Object.keys($slots)" #[name]="slotProps" :key="name">
        <slot :name="name" v-bind="slotProps || {}" />
      </template>
    </a-table>
  </div>
</template>

<style scoped>
.pro-table {
  background: var(--bg-elevated);
  padding: 16px;
  border-radius: 8px;
}
</style>
