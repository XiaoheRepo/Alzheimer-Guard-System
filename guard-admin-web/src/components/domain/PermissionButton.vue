<!-- src/components/domain/PermissionButton.vue -->
<!-- 带权限检查的按钮（降级型：无权限置灰 + tooltip） -->
<script setup lang="ts">
import { computed } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useI18n } from 'vue-i18n'
import type { Role } from '@/types/common'

const props = defineProps<{
  roles?: Role | Role[]
  disabled?: boolean
  tooltip?: string
}>()

const auth = useAuthStore()
const { t } = useI18n()

const allowed = computed(() => {
  if (!props.roles) return true
  const role = auth.user?.role
  if (!role) return false
  if (role === 'SUPER_ADMIN') return true
  const list = Array.isArray(props.roles) ? props.roles : [props.roles]
  return list.includes(role)
})

const finalDisabled = computed(() => !allowed.value || !!props.disabled)
const finalTooltip = computed(() =>
  !allowed.value ? t('common.noPermission') : props.tooltip || '',
)
</script>

<template>
  <a-tooltip :title="finalTooltip">
    <a-button v-bind="$attrs" :disabled="finalDisabled">
      <slot />
    </a-button>
  </a-tooltip>
</template>
