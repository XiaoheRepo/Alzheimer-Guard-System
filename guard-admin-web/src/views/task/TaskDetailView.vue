<!-- src/views/task/TaskDetailView.vue / P-03b -->
<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { message, Modal } from 'ant-design-vue'
import {
  getTaskSnapshot,
  getLatestTrajectory,
  markSustained,
  closeTask,
  type TaskSnapshot,
  type TrajectoryLatest,
} from '@/api/task'
import { listClues, type ClueListItem } from '@/api/clue'
import PageHeader from '@/components/common/PageHeader.vue'
import StatusBadge from '@/components/domain/StatusBadge.vue'
import CopyableText from '@/components/common/CopyableText.vue'
import PermissionButton from '@/components/domain/PermissionButton.vue'
import { fmtDateTime } from '@/utils/format'
import { useAuthStore } from '@/stores/auth'
import type { ApiError } from '@/utils/request'

const route = useRoute()
const router = useRouter()
const { t } = useI18n()
const auth = useAuthStore()

const taskId = computed(() => route.params.taskId as string)
const snap = ref<TaskSnapshot | null>(null)
const traj = ref<TrajectoryLatest | null>(null)
const clues = ref<ClueListItem[]>([])
const loading = ref(false)
const activeTab = ref('overview')

async function load() {
  loading.value = true
  try {
    const [s, tr, c] = await Promise.all([
      getTaskSnapshot(taskId.value),
      getLatestTrajectory(taskId.value).catch(() => null),
      listClues({ task_id: taskId.value, limit: 50 }).catch(
        () => ({ items: [] as ClueListItem[] }),
      ),
    ])
    snap.value = s
    traj.value = tr
    clues.value = c.items || []
  } finally {
    loading.value = false
  }
}

onMounted(load)

async function onSustain() {
  Modal.confirm({
    title: t('page.task.sustain.title'),
    content: t('page.task.sustain.content'),
    okText: t('common.confirm'),
    cancelText: t('common.cancel'),
    onOk: async () => {
      try {
        await markSustained(taskId.value, { request_time: new Date().toISOString() })
        message.success(t('common.success'))
        await load()
      } catch (e) {
        message.error((e as ApiError)?.message || t('error.UNKNOWN'))
      }
    },
  })
}

const closeForm = ref({ close_type: 'FOUND' as 'FOUND' | 'FALSE_ALARM', close_reason: '' })
const showClose = ref(false)

async function onCloseSubmit() {
  if (closeForm.value.close_reason.trim().length < 5) {
    message.warning(t('page.task.close.reasonMin'))
    return
  }
  try {
    await closeTask(taskId.value, {
      close_type: closeForm.value.close_type,
      close_reason: closeForm.value.close_reason,
      request_time: new Date().toISOString(),
    })
    message.success(t('common.success'))
    showClose.value = false
    await load()
  } catch (e) {
    message.error((e as ApiError)?.message || t('error.UNKNOWN'))
  }
}

const canOperate = computed(() => {
  const s = snap.value?.status
  return (
    auth.isBackofficeAllowed() &&
    s &&
    ['CREATED', 'ACTIVE', 'SUSTAINED'].includes(s as string)
  )
})
</script>

<template>
  <div class="page-container" v-loading="loading">
    <PageHeader :title="t('page.task.detail.title')">
      <template #extra>
        <a-button @click="router.back()">{{ t('common.back') }}</a-button>
        <PermissionButton
          v-if="canOperate && snap?.status !== 'SUSTAINED'"
          type="default"
          :roles="['ADMIN', 'SUPER_ADMIN']"
          @click="onSustain"
        >
          {{ t('page.task.sustain.btn') }}
        </PermissionButton>
        <PermissionButton
          v-if="canOperate"
          type="primary"
          danger
          :roles="['ADMIN', 'SUPER_ADMIN']"
          @click="showClose = true"
        >
          {{ t('page.task.close.btn') }}
        </PermissionButton>
      </template>
    </PageHeader>

    <a-card v-if="snap">
      <a-descriptions bordered :column="2" size="small">
        <a-descriptions-item :label="t('page.task.col.taskNo')">
          <CopyableText :text="snap.task_no || snap.task_id" />
        </a-descriptions-item>
        <a-descriptions-item :label="t('page.task.col.status')">
          <StatusBadge kind="taskStatus" :value="snap.status" />
        </a-descriptions-item>
        <a-descriptions-item :label="t('page.task.col.patient')">
          {{ snap.patient_snapshot?.patient_name || '-' }}
          <span class="text-muted" v-if="snap.patient_snapshot?.short_code">
            ({{ snap.patient_snapshot.short_code }})
          </span>
        </a-descriptions-item>
        <a-descriptions-item :label="t('page.task.col.source')">{{ snap.source || '-' }}</a-descriptions-item>
        <a-descriptions-item :label="t('page.task.col.createdAt')">{{ fmtDateTime(snap.created_at) }}</a-descriptions-item>
        <a-descriptions-item :label="t('page.task.col.closedAt')">{{ fmtDateTime(snap.closed_at) || '-' }}</a-descriptions-item>
        <a-descriptions-item :label="t('page.task.closeType')" v-if="snap.close_type">
          {{ snap.close_type }}
        </a-descriptions-item>
        <a-descriptions-item :label="t('page.task.closeReason')" v-if="snap.close_reason">
          {{ snap.close_reason }}
        </a-descriptions-item>
      </a-descriptions>
    </a-card>

    <a-tabs v-model:active-key="activeTab" style="margin-top: 16px">
      <a-tab-pane key="overview" :tab="t('page.task.tab.overview')">
        <a-row :gutter="16">
          <a-col :md="8">
            <a-statistic :title="t('page.task.clueTotal')" :value="snap?.clue_summary?.total_clue_count ?? 0" />
          </a-col>
          <a-col :md="8">
            <a-statistic :title="t('page.task.clueValid')" :value="snap?.clue_summary?.valid_clue_count ?? 0" />
          </a-col>
          <a-col :md="8">
            <a-statistic :title="t('page.task.trajPoint')" :value="snap?.trajectory_summary?.point_count ?? 0" />
          </a-col>
        </a-row>
      </a-tab-pane>

      <a-tab-pane key="trajectory" :tab="t('page.task.tab.trajectory')">
        <a-empty v-if="!traj?.points?.length" />
        <a-list v-else :data-source="traj.points" size="small">
          <template #renderItem="{ item }">
            <a-list-item>
              <div>
                <strong>{{ fmtDateTime(item.ts) }}</strong>
                <span class="text-muted" style="margin-left: 8px">
                  {{ item.lat?.toFixed(5) }}, {{ item.lng?.toFixed(5) }}
                </span>
                <a-tag v-if="item.source" style="margin-left: 8px">{{ item.source }}</a-tag>
              </div>
            </a-list-item>
          </template>
        </a-list>
      </a-tab-pane>

      <a-tab-pane key="clues" :tab="t('page.task.tab.clues')">
        <a-empty v-if="!clues.length" />
        <a-list v-else :data-source="clues" size="small">
          <template #renderItem="{ item }">
            <a-list-item>
              <a-list-item-meta>
                <template #title>
                  <StatusBadge kind="clueReviewState" :value="item.review_state" />
                  <span style="margin-left: 8px">{{ item.location_name || '-' }}</span>
                </template>
                <template #description>
                  {{ fmtDateTime(item.reported_at) }}
                  <span v-if="item.ai_summary" class="text-muted" style="margin-left: 8px">
                    · {{ item.ai_summary }}
                  </span>
                </template>
              </a-list-item-meta>
            </a-list-item>
          </template>
        </a-list>
      </a-tab-pane>
    </a-tabs>

    <a-modal
      v-model:open="showClose"
      :title="t('page.task.close.title')"
      :ok-text="t('common.submit')"
      :cancel-text="t('common.cancel')"
      @ok="onCloseSubmit"
    >
      <a-form layout="vertical">
        <a-form-item :label="t('page.task.closeType')" required>
          <a-radio-group v-model:value="closeForm.close_type">
            <a-radio value="FOUND">{{ t('page.task.close.found') }}</a-radio>
            <a-radio value="FALSE_ALARM">{{ t('page.task.close.falseAlarm') }}</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item :label="t('page.task.closeReason')" required>
          <a-textarea
            v-model:value="closeForm.close_reason"
            :rows="4"
            :maxlength="500"
            show-count
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>
