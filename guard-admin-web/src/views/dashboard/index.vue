<script setup lang="ts">
/**
 * ADM-01 运营看板
 * 依据 web_admin_handbook.md §9.2：
 *   - 默认窗口 24h，切换触发三路并发请求
 *   - 局部降级：单卡片失败不清空其他卡片
 *   - 指标点击跳转对应治理页
 */
import { ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { getDashboardMetrics, getClueStatistics, getSecurityMetrics } from '@/api/dashboard'
import type {
  DashboardMetrics,
  ClueStatistics,
  SecurityMetrics,
  DashboardWindow,
} from '@/api/dashboard'

const router = useRouter()

const window_ = ref<DashboardWindow>('24h')
const windowOptions = [
  { label: '近1小时', value: '1h' },
  { label: '近24小时', value: '24h' },
  { label: '近7天', value: '7d' },
  { label: '近30天', value: '30d' },
]

const metrics = ref<DashboardMetrics | null>(null)
const metricsError = ref(false)
const metricsLoading = ref(false)

const clueStats = ref<ClueStatistics | null>(null)
const clueStatsError = ref(false)
const clueStatsLoading = ref(false)

const secMetrics = ref<SecurityMetrics | null>(null)
const secMetricsError = ref(false)
const secMetricsLoading = ref(false)

const loadAll = async () => {
  // 三路并发，局部降级：一路失败不影响另外两路
  metricsLoading.value = true
  clueStatsLoading.value = true
  secMetricsLoading.value = true

  const [r1, r2, r3] = await Promise.allSettled([
    getDashboardMetrics(window_.value),
    getClueStatistics(),
    getSecurityMetrics(),
  ])

  metricsLoading.value = false
  clueStatsLoading.value = false
  secMetricsLoading.value = false

  if (r1.status === 'fulfilled') {
    metrics.value = r1.value
    metricsError.value = false
  } else metricsError.value = true

  if (r2.status === 'fulfilled') {
    clueStats.value = r2.value
    clueStatsError.value = false
  } else clueStatsError.value = true

  if (r3.status === 'fulfilled') {
    secMetrics.value = r3.value
    secMetricsError.value = false
  } else secMetricsError.value = true
}

watch(window_, loadAll, { immediate: true })

const pct = (v?: number) => (v != null ? `${(v * 100).toFixed(1)}%` : '-')
const num = (v?: number) => (v != null ? v.toLocaleString() : '-')
</script>

<template>
  <div class="dashboard-page">
    <!-- 窗口切换 -->
    <div class="dashboard-toolbar">
      <span class="toolbar-label">统计窗口：</span>
      <a-radio-group v-model:value="window_" button-style="solid" size="small">
        <a-radio-button v-for="o in windowOptions" :key="o.value" :value="o.value">
          {{ o.label }}
        </a-radio-button>
      </a-radio-group>
    </div>

    <!-- 运营指标卡片 -->
    <a-row :gutter="[16, 16]" class="kpi-row">
      <a-col :xs="24" :sm="12" :lg="6">
        <a-card
          :loading="metricsLoading"
          hoverable
          class="kpi-card"
          @click="router.push('/admin/audit')"
        >
          <template v-if="metricsError">
            <a-result
              status="error"
              title="加载失败"
              sub-title="点击刷新"
              style="padding: 12px"
              @click.stop="loadAll"
            />
          </template>
          <template v-else>
            <a-statistic
              title="登录成功率"
              :value="metrics ? pct(metrics.login_success_rate) : '-'"
              :value-style="{ color: '#2e7d32', fontSize: '28px' }"
            />
          </template>
        </a-card>
      </a-col>
      <a-col :xs="24" :sm="12" :lg="6">
        <a-card
          :loading="metricsLoading"
          hoverable
          class="kpi-card"
          @click="router.push('/admin/audit')"
        >
          <template v-if="metricsError">
            <a-result status="error" title="加载失败" style="padding: 12px" />
          </template>
          <template v-else>
            <a-statistic
              title="高危操作次数"
              :value="metrics ? num(metrics.risk_operation_count) : '-'"
              :value-style="{ color: '#b26a00', fontSize: '28px' }"
            />
          </template>
        </a-card>
      </a-col>
      <a-col :xs="24" :sm="12" :lg="6">
        <a-card :loading="metricsLoading" class="kpi-card">
          <template v-if="metricsError">
            <a-result status="error" title="加载失败" style="padding: 12px" />
          </template>
          <template v-else>
            <a-statistic
              title="接口 TP95（ms）"
              :value="metrics ? num(metrics.tp95_ms) : '-'"
              :value-style="{ color: '#0b6b8a', fontSize: '28px' }"
            />
          </template>
        </a-card>
      </a-col>
      <a-col :xs="24" :sm="12" :lg="6">
        <a-card :loading="metricsLoading" class="kpi-card">
          <template v-if="metricsError">
            <a-result status="error" title="加载失败" style="padding: 12px" />
          </template>
          <template v-else>
            <a-statistic
              title="接口错误率"
              :value="metrics ? pct(metrics.error_rate) : '-'"
              :value-style="{
                color: (metrics?.error_rate ?? 0) > 0.05 ? '#b42318' : '#2e7d32',
                fontSize: '28px',
              }"
            />
          </template>
        </a-card>
      </a-col>
    </a-row>

    <!-- 线索统计 + 安全指标卡片 -->
    <a-row :gutter="[16, 16]" class="kpi-row">
      <!-- 线索复核统计 -->
      <a-col :xs="24" :lg="12">
        <a-card
          title="线索复核统计"
          :loading="clueStatsLoading"
          hoverable
          @click="router.push('/admin/clues/review')"
        >
          <template v-if="clueStatsError">
            <a-alert type="error" message="数据加载失败，请稍后刷新" show-icon />
          </template>
          <template v-else-if="clueStats">
            <a-row :gutter="16">
              <a-col :span="8">
                <a-statistic title="总线索数" :value="clueStats.total_clues" />
              </a-col>
              <a-col :span="8">
                <a-statistic
                  title="疑似线索"
                  :value="clueStats.suspected_count"
                  :value-style="{ color: '#b26a00' }"
                />
              </a-col>
              <a-col :span="8">
                <a-statistic title="平均复核（分钟）" :value="clueStats.avg_review_minutes" />
              </a-col>
            </a-row>
            <a-row :gutter="16" style="margin-top: 16px">
              <a-col :span="12">
                <a-statistic
                  title="已确认覆盖"
                  :value="clueStats.overridden_count"
                  :value-style="{ color: '#2e7d32' }"
                />
              </a-col>
              <a-col :span="12">
                <a-statistic
                  title="已拒绝"
                  :value="clueStats.rejected_count"
                  :value-style="{ color: '#b42318' }"
                />
              </a-col>
            </a-row>
          </template>
        </a-card>
      </a-col>

      <!-- 安全指标 -->
      <a-col :xs="24" :lg="12">
        <a-card
          title="安全指标"
          :loading="secMetricsLoading"
          hoverable
          @click="router.push('/admin/audit')"
        >
          <template v-if="secMetricsError">
            <a-alert type="error" message="数据加载失败，请稍后刷新" show-icon />
          </template>
          <template v-else-if="secMetrics">
            <a-row :gutter="16">
              <a-col :span="12">
                <a-statistic
                  title="失败登录次数"
                  :value="secMetrics.failed_login_count"
                  :value-style="{
                    color: secMetrics.failed_login_count > 10 ? '#b42318' : '#18242d',
                  }"
                />
              </a-col>
              <a-col :span="12">
                <a-statistic
                  title="封禁用户数"
                  :value="secMetrics.banned_user_count"
                  :value-style="{ color: '#b42318' }"
                />
              </a-col>
            </a-row>
            <a-row :gutter="16" style="margin-top: 16px">
              <a-col :span="12">
                <a-statistic
                  title="高危操作次数"
                  :value="secMetrics.risk_operation_count"
                  :value-style="{ color: '#b26a00' }"
                />
              </a-col>
              <a-col :span="12">
                <a-statistic title="CAPTCHA 触发" :value="secMetrics.captcha_trigger_count" />
              </a-col>
            </a-row>
          </template>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<style scoped>
.dashboard-page {
  width: 100%;
}
.dashboard-toolbar {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
}
.toolbar-label {
  margin-right: 12px;
  color: #18242d;
  font-size: 14px;
}
.kpi-row {
  margin-bottom: 16px;
}
.kpi-card {
  cursor: pointer;
}
</style>
