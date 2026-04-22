<!-- src/components/domain/StatusBadge.vue -->
<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'

type Kind =
  | 'taskStatus'
  | 'clueReviewState'
  | 'orderState'
  | 'tagState'
  | 'patientStatus'
  | 'userStatus'
  | 'riskLevel'
  | 'userRole'
  | 'notificationCategory'

const props = defineProps<{ kind: Kind; value?: string | null }>()
const { t } = useI18n()

const colorMap: Record<string, string> = {
  // taskStatus
  CREATED: 'default',
  ACTIVE: 'orange',
  SUSTAINED: 'blue',
  CLOSED_FOUND: 'green',
  CLOSED_FALSE_ALARM: 'red',
  // clueReviewState
  SUBMITTED: 'default',
  PENDING: 'blue',
  PENDING_REVIEW: 'gold',
  VALID: 'green',
  INVALID: 'red',
  // orderState
  PENDING_AUDIT: 'gold',
  PENDING_SHIP: 'blue',
  SHIPPED: 'purple',
  RECEIVED: 'green',
  CANCELLED: 'default',
  EXCEPTION: 'red',
  // tagState
  UNBOUND: 'default',
  ALLOCATED: 'blue',
  BOUND: 'green',
  SUSPECTED_LOST: 'gold',
  LOST: 'red',
  VOIDED: 'default',
  // patientStatus
  NORMAL: 'green',
  MISSING_PENDING: 'gold',
  MISSING: 'red',
  // userStatus
  DISABLED: 'gold',
  DEACTIVATED: 'default',
  // riskLevel
  LOW: 'green',
  MEDIUM: 'gold',
  HIGH: 'red',
  // userRole
  FAMILY: 'blue',
  ADMIN: 'orange',
  SUPER_ADMIN: 'red',
  // notificationCategory
  ALERT: 'red',
  SYS: 'blue',
  BIZ: 'orange',
}

const color = computed(() => colorMap[props.value ?? ''] || 'default')
const label = computed(() => {
  if (!props.value) return '-'
  const key = `field.${props.kind}.${props.value}`
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const te: any = (useI18n() as any).te
  return te && te(key) ? (t(key) as string) : props.value
})
</script>

<template>
  <a-tag :color="color">{{ label }}</a-tag>
</template>
