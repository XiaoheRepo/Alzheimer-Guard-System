<!-- src/views/material/TagBatchJobView.vue / P-07b -->
<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { getBatchJob, type BatchJob } from '@/api/tag'
import PageHeader from '@/components/common/PageHeader.vue'
import { fmtDateTime } from '@/utils/format'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()

const jobId = computed(() => route.params.jobId as string)
const job = ref<BatchJob | null>(null)
let timer: ReturnType<typeof setInterval> | null = null

async function load() {
  try {
    job.value = await getBatchJob(jobId.value)
    if (job.value.status === 'DONE' || job.value.status === 'FAILED') {
      stop()
    }
  } catch {
    stop()
  }
}

function stop() {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
}

onMounted(() => {
  void load()
  timer = setInterval(load, 3000)
})
onBeforeUnmount(stop)

const percent = computed(() => {
  if (!job.value?.total) return 0
  return Math.round(((job.value.generated || 0) / job.value.total) * 100)
})
</script>

<template>
  <div class="page-container">
    <PageHeader :title="t('page.tag.job.title')">
      <template #extra>
        <a-button @click="router.push('/tags/inventory')">{{ t('common.back') }}</a-button>
      </template>
    </PageHeader>

    <a-card v-if="job">
      <a-descriptions bordered :column="2" size="small">
        <a-descriptions-item :label="t('page.tag.job.id')">{{ job.job_id }}</a-descriptions-item>
        <a-descriptions-item :label="t('page.tag.job.status')">
          <a-tag
            :color="job.status === 'DONE' ? 'green' : job.status === 'FAILED' ? 'red' : 'blue'"
          >
            {{ job.status }}
          </a-tag>
        </a-descriptions-item>
        <a-descriptions-item :label="t('page.tag.job.total')">{{ job.total }}</a-descriptions-item>
        <a-descriptions-item :label="t('page.tag.job.generated')">{{ job.generated }}</a-descriptions-item>
        <a-descriptions-item :label="t('page.tag.job.createdAt')">
          {{ fmtDateTime(job.created_at) }}
        </a-descriptions-item>
        <a-descriptions-item :label="t('page.tag.job.updatedAt')">
          {{ fmtDateTime(job.updated_at) || '-' }}
        </a-descriptions-item>
        <a-descriptions-item :label="t('page.tag.job.remark')" :span="2" v-if="job.remark">
          {{ job.remark }}
        </a-descriptions-item>
        <a-descriptions-item :label="t('page.tag.job.error')" :span="2" v-if="job.error_message">
          <a-typography-text type="danger">{{ job.error_message }}</a-typography-text>
        </a-descriptions-item>
      </a-descriptions>

      <a-progress :percent="percent" style="margin-top: 16px" />

      <div v-if="job.status === 'DONE' && job.download_url" style="margin-top: 16px">
        <a :href="job.download_url" target="_blank">
          <a-button type="primary">{{ t('page.tag.job.download') }}</a-button>
        </a>
      </div>
    </a-card>
  </div>
</template>
