<!-- src/components/common/CopyableText.vue -->
<script setup lang="ts">
import { computed } from 'vue'
import { message } from 'ant-design-vue'
import { CopyOutlined } from '@ant-design/icons-vue'
import { useI18n } from 'vue-i18n'

const props = defineProps<{
  text?: string | null
  short?: boolean
  maxChars?: number
}>()

const { t } = useI18n()

const display = computed(() => {
  const v = props.text ?? ''
  if (!v) return '-'
  if (props.short) {
    const n = props.maxChars ?? 8
    return v.length > n ? v.slice(0, n) + '…' : v
  }
  return v
})

async function onCopy() {
  if (!props.text) return
  try {
    await navigator.clipboard.writeText(props.text)
    message.success(t('common.copied'))
  } catch {
    message.error(t('common.failed'))
  }
}
</script>

<template>
  <span class="copyable">
    <span class="text-mono ellipsis" :title="text || ''">{{ display }}</span>
    <a-button
      v-if="text"
      type="link"
      size="small"
      :aria-label="t('common.copy')"
      @click="onCopy"
    >
      <template #icon><CopyOutlined /></template>
    </a-button>
  </span>
</template>

<style scoped>
.copyable {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}
</style>
