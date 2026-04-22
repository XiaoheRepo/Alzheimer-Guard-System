<!-- src/views/dashboard/DashboardView.vue / P-02 -->
<script setup lang="ts">
import { onMounted, ref, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import VChart from 'vue-echarts'
import 'echarts'
import { getDashboard } from '@/api/dashboard'
import type { DashboardData } from '@/api/dashboard'
import PageHeader from '@/components/common/PageHeader.vue'
import { fmtDateTime } from '@/utils/format'

const { t } = useI18n()
const router = useRouter()
const data = ref<DashboardData | null>(null)
const loading = ref(false)
const range = ref<'7d' | '14d' | '30d'>('7d')

async function load() {
  loading.value = true
  try {
    data.value = await getDashboard({ range: range.value })
  } finally {
    loading.value = false
  }
}

onMounted(load)

const kpis = computed(() => {
  const s = data.value?.summary || {}
  return [
    {
      key: 'active_task_count',
      title: t('page.dashboard.activeTask'),
      value: s.active_task_count ?? 0,
      color: '#F97316',
      click: () => router.push('/tasks'),
    },
    {
      key: 'pending_clue_count',
      title: t('page.dashboard.pendingClue'),
      value: s.pending_clue_count ?? 0,
      color: '#0EA5E9',
      click: () => router.push('/clues/review'),
    },
    {
      key: 'pending_order_count',
      title: t('page.dashboard.pendingOrder'),
      value: s.pending_order_count ?? 0,
      color: '#9333ea',
      click: () => router.push('/material/orders'),
    },
    {
      key: 'recent_alert_count',
      title: t('page.dashboard.recentAlert'),
      value: s.recent_alert_count ?? 0,
      color: '#ef4444',
      click: () => router.push('/notifications'),
    },
  ]
})

const chartOption = computed(() => {
  const t1 = data.value?.series?.task_daily || []
  const c1 = data.value?.series?.clue_daily || []
  return {
    tooltip: { trigger: 'axis' },
    legend: { data: [t('page.dashboard.activeTask'), t('page.dashboard.pendingClue')] },
    grid: { left: 40, right: 20, top: 40, bottom: 30 },
    xAxis: { type: 'category', data: t1.map((p) => p.date) },
    yAxis: { type: 'value' },
    series: [
      {
        name: t('page.dashboard.activeTask'),
        type: 'line',
        smooth: true,
        data: t1.map((p) => p.value),
        itemStyle: { color: '#F97316' },
      },
      {
        name: t('page.dashboard.pendingClue'),
        type: 'line',
        smooth: true,
        data: c1.map((p) => p.value),
        itemStyle: { color: '#0EA5E9' },
      },
    ],
  }
})
</script>

<template>
  <div class="page-container">
    <PageHeader :title="t('menu.dashboard')">
      <template #extra>
        <a-radio-group v-model:value="range" @change="load">
          <a-radio-button value="7d">7d</a-radio-button>
          <a-radio-button value="14d">14d</a-radio-button>
          <a-radio-button value="30d">30d</a-radio-button>
        </a-radio-group>
        <a-button :loading="loading" @click="load">{{ t('common.refresh') }}</a-button>
      </template>
    </PageHeader>

    <a-row :gutter="16">
      <a-col v-for="k in kpis" :key="k.key" :xs="12" :md="6">
        <a-card hoverable class="kpi-card" @click="k.click">
          <div class="kpi-title">{{ k.title }}</div>
          <div class="kpi-value" :style="{ color: k.color }">{{ k.value }}</div>
        </a-card>
      </a-col>
    </a-row>

    <a-card :title="t('page.dashboard.trend')" style="margin-top: 16px">
      <v-chart :option="chartOption" autoresize style="height: 320px" />
    </a-card>

    <a-card :title="t('page.dashboard.alerts')" style="margin-top: 16px">
      <a-empty v-if="!data?.recent_alerts?.length" />
      <a-list v-else :data-source="data.recent_alerts" size="small">
        <template #renderItem="{ item }">
          <a-list-item>
            <a-list-item-meta>
              <template #title>
                <a-tag :color="item.level === 'CRITICAL' ? 'red' : item.level === 'ALERT' ? 'orange' : 'blue'">
                  {{ item.level }}
                </a-tag>
                {{ item.message }}
              </template>
              <template #description>
                {{ fmtDateTime(item.timestamp) }}
                <span v-if="item.trace_id" class="text-muted">· Trace {{ item.trace_id.slice(0, 12) }}</span>
              </template>
            </a-list-item-meta>
          </a-list-item>
        </template>
      </a-list>
    </a-card>
  </div>
</template>

<style scoped>
.kpi-card {
  cursor: pointer;
}
.kpi-title {
  color: var(--text-muted);
  font-size: 13px;
}
.kpi-value {
  font-size: 28px;
  font-weight: 700;
  margin-top: 8px;
}
</style>
