<!-- src/components/domain/ClueDetailDrawer.vue -->
<script setup lang="ts">
import { ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import { overrideClue, rejectClue, type ClueListItem } from '@/api/clue'
import StatusBadge from './StatusBadge.vue'
import CopyableText from '@/components/common/CopyableText.vue'
import { fmtDateTime } from '@/utils/format'
import type { ApiError } from '@/utils/request'

const props = defineProps<{ open: boolean; clue: ClueListItem | null }>()
const emit = defineEmits<{
  (e: 'update:open', v: boolean): void
  (e: 'success'): void
}>()

const { t } = useI18n()
const action = ref<'override' | 'reject' | null>(null)
const reason = ref('')
const submitting = ref(false)

watch(
  () => props.open,
  (v) => {
    if (v) {
      action.value = null
      reason.value = ''
    }
  },
)

async function submit() {
  if (!props.clue || !action.value) return
  const len = reason.value.trim().length
  if (len < 30 || len > 500) {
    message.warning(t('page.clue.reasonRange'))
    return
  }
  submitting.value = true
  try {
    if (action.value === 'override') {
      await overrideClue(props.clue.clue_id, {
        reason: reason.value,
        request_time: new Date().toISOString(),
      })
    } else {
      await rejectClue(props.clue.clue_id, {
        reason: reason.value,
        request_time: new Date().toISOString(),
      })
    }
    message.success(t('common.success'))
    emit('success')
    emit('update:open', false)
  } catch (e) {
    message.error((e as ApiError)?.message || t('error.UNKNOWN'))
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <a-drawer
    :open="open"
    width="640"
    :title="t('page.clue.drawer.title')"
    @update:open="(v: boolean) => emit('update:open', v)"
  >
    <template v-if="clue">
      <a-descriptions bordered :column="1" size="small">
        <a-descriptions-item :label="t('page.clue.col.clueId')">
          <CopyableText :text="clue.clue_id" short />
        </a-descriptions-item>
        <a-descriptions-item :label="t('page.clue.col.state')">
          <StatusBadge kind="clueReviewState" :value="clue.review_state" />
        </a-descriptions-item>
        <a-descriptions-item :label="t('page.clue.col.risk')" v-if="clue.risk_level">
          <StatusBadge kind="riskLevel" :value="clue.risk_level" />
        </a-descriptions-item>
        <a-descriptions-item :label="t('page.clue.col.reportedAt')">
          {{ fmtDateTime(clue.reported_at) }}
        </a-descriptions-item>
        <a-descriptions-item :label="t('page.clue.col.location')">
          {{ clue.location_name || '-' }}
          <span v-if="clue.lat && clue.lng" class="text-muted">
            ({{ clue.lat.toFixed(5) }}, {{ clue.lng.toFixed(5) }})
          </span>
        </a-descriptions-item>
        <a-descriptions-item :label="t('page.clue.col.aiSummary')" v-if="clue.ai_summary">
          {{ clue.ai_summary }}
        </a-descriptions-item>
        <a-descriptions-item :label="t('page.clue.col.media')" v-if="clue.media?.length">
          <a-image-preview-group>
            <a-image
              v-for="(m, i) in clue.media"
              :key="i"
              :src="m.thumb_url || m.url"
              width="80"
              style="margin: 4px"
            />
          </a-image-preview-group>
        </a-descriptions-item>
      </a-descriptions>

      <a-divider>{{ t('page.clue.history') }}</a-divider>
      <a-timeline v-if="clue.review_history?.length">
        <a-timeline-item v-for="(h, i) in clue.review_history" :key="i">
          <div>
            <strong>{{ h.action }}</strong> · {{ fmtDateTime(h.reviewed_at) }}
          </div>
          <div class="text-muted">{{ h.reason || '-' }}</div>
        </a-timeline-item>
      </a-timeline>
      <a-empty v-else />

      <a-divider>{{ t('page.clue.decision') }}</a-divider>
      <a-radio-group v-model:value="action">
        <a-radio value="override">{{ t('page.clue.override') }}</a-radio>
        <a-radio value="reject">{{ t('page.clue.reject') }}</a-radio>
      </a-radio-group>
      <a-textarea
        v-model:value="reason"
        :rows="4"
        :maxlength="500"
        show-count
        :placeholder="t('page.clue.reasonPH')"
        style="margin-top: 12px"
      />
    </template>

    <template #footer>
      <a-space>
        <a-button @click="emit('update:open', false)">{{ t('common.cancel') }}</a-button>
        <a-button type="primary" :loading="submitting" :disabled="!action" @click="submit">
          {{ t('common.submit') }}
        </a-button>
      </a-space>
    </template>
  </a-drawer>
</template>
