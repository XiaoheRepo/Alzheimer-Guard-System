<template>
  <div class="page-container">
    <a-page-header title="工单详情" @back="$router.back()" />

    <a-spin :spinning="loading">
      <template v-if="detail">
        <a-card title="工单基本信息" :bordered="false">
          <a-descriptions :column="2" bordered>
            <a-descriptions-item label="工单 ID">{{ detail.order_id }}</a-descriptions-item>
            <a-descriptions-item label="工单编号">{{ detail.order_no }}</a-descriptions-item>
            <a-descriptions-item label="患者 ID">{{ detail.patient_id }}</a-descriptions-item>
            <a-descriptions-item label="申请人">{{ detail.applicant_user_id }}</a-descriptions-item>
            <a-descriptions-item label="标签编码">{{ detail.tag_code ?? '-' }}</a-descriptions-item>
            <a-descriptions-item label="数量">{{ detail.quantity }}</a-descriptions-item>
            <a-descriptions-item label="状态">
              <a-tag :color="orderStatusColors[detail.status]">
                {{ orderStatusLabels[detail.status] ?? detail.status }}
              </a-tag>
            </a-descriptions-item>
            <a-descriptions-item label="物流单号">{{
              detail.tracking_number ?? '-'
            }}</a-descriptions-item>
            <a-descriptions-item label="收货地址" :span="2">{{
              detail.delivery_address ?? '-'
            }}</a-descriptions-item>
            <a-descriptions-item label="取消原因" :span="2">{{
              detail.cancel_reason ?? '-'
            }}</a-descriptions-item>
            <a-descriptions-item label="异常描述" :span="2">{{
              detail.exception_desc ?? '-'
            }}</a-descriptions-item>
            <a-descriptions-item label="创建时间">{{ detail.created_at }}</a-descriptions-item>
            <a-descriptions-item label="更新时间">{{ detail.updated_at }}</a-descriptions-item>
          </a-descriptions>
        </a-card>

        <!-- 操作区 -->
        <a-card title="工单操作" :bordered="false" style="margin-top: 12px">
          <a-space wrap>
            <a-button
              v-if="detail.status === 'PENDING'"
              type="primary"
              @click="openModal('approve')"
              >审核通过</a-button
            >
            <a-button
              v-if="detail.status === 'PROCESSING'"
              type="primary"
              @click="openModal('ship')"
              >发货</a-button
            >
            <a-button v-if="detail.status === 'CANCEL_PENDING'" @click="openModal('cancel_approve')"
              >同意取消</a-button
            >
            <a-button
              v-if="detail.status === 'CANCEL_PENDING'"
              danger
              @click="openModal('cancel_reject')"
              >拒绝取消</a-button
            >
            <a-button
              v-if="detail.status === 'SHIPPED'"
              danger
              @click="openModal('logistics_exception')"
              >报告物流异常</a-button
            >
            <a-button v-if="detail.status === 'EXCEPTION'" @click="openModal('reship')"
              >补发</a-button
            >
            <a-button v-if="detail.status === 'EXCEPTION'" @click="openModal('close_exception')"
              >关闭异常</a-button
            >
          </a-space>
        </a-card>

        <!-- 状态时间线 -->
        <a-card title="流转记录" :bordered="false" style="margin-top: 12px">
          <a-timeline v-if="timeline.length > 0">
            <a-timeline-item v-for="item in timeline" :key="item.timeline_id">
              <p>
                <strong>{{ item.from_status }} → {{ item.to_status }}</strong>
                <span style="margin-left: 12px; color: #999">{{ item.created_at }}</span>
              </p>
              <p v-if="item.remark" style="color: #666">{{ item.remark }}</p>
              <p style="color: #888; font-size: 12px">操作人：{{ item.operator_user_id }}</p>
            </a-timeline-item>
          </a-timeline>
          <a-empty v-else description="暂无流转记录" />
        </a-card>
      </template>
    </a-spin>

    <!-- 操作弹窗 -->
    <a-modal
      v-model:open="modalVisible"
      :title="modalTitles[currentAction]"
      :ok-button-props="{ loading: submitting, disabled: needsReason && actionReason.length < 5 }"
      @ok="submitAction"
    >
      <a-form layout="vertical">
        <a-form-item
          v-if="currentAction === 'ship' || currentAction === 'reship'"
          label="物流单号"
          required
        >
          <a-input v-model:value="trackingNumber" placeholder="请输入快递单号" />
        </a-form-item>
        <a-form-item v-if="needsReason" :label="reasonLabel" required>
          <a-textarea v-model:value="actionReason" :rows="3" :maxlength="256" show-count />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  getAdminOrderDetail,
  getOrderTimeline,
  approveOrder,
  cancelApproveOrder,
  cancelRejectOrder,
  shipOrder,
  reportLogisticsException,
  reshipOrder,
  closeException,
  type OrderStatus,
  type MaterialOrderDetail,
  type OrderTimelineItem,
} from '@/api/material'

const route = useRoute()
const router = useRouter()
const orderId = route.params.orderId as string

const detail = ref<MaterialOrderDetail | null>(null)
const timeline = ref<OrderTimelineItem[]>([])
const loading = ref(false)

const modalVisible = ref(false)
type ActionType =
  | 'approve'
  | 'ship'
  | 'cancel_approve'
  | 'cancel_reject'
  | 'logistics_exception'
  | 'reship'
  | 'close_exception'
const currentAction = ref<ActionType>('approve')
const actionReason = ref('')
const trackingNumber = ref('')
const submitting = ref(false)

const modalTitles: Record<ActionType, string> = {
  approve: '审核通过',
  ship: '确认发货',
  cancel_approve: '同意取消申请',
  cancel_reject: '拒绝取消申请',
  logistics_exception: '报告物流异常',
  reship: '补发',
  close_exception: '关闭物流异常',
}
const needsReason = computed(() =>
  ['cancel_approve', 'cancel_reject', 'logistics_exception'].includes(currentAction.value),
)
const reasonLabel = computed(() => {
  const map: Record<string, string> = {
    cancel_approve: '取消原因（5-256 字）',
    cancel_reject: '拒绝原因（5-256 字）',
    logistics_exception: '异常描述（5-256 字）',
  }
  return map[currentAction.value] ?? '原因'
})

const orderStatusLabels: Record<OrderStatus, string> = {
  PENDING: '待审核',
  PROCESSING: '처理中',
  CANCEL_PENDING: '待取消审核',
  SHIPPED: '已发货',
  EXCEPTION: '物流异常',
  COMPLETED: '已完成',
  CANCELLED: '已取消',
}
const orderStatusColors: Record<OrderStatus, string> = {
  PENDING: 'warning',
  PROCESSING: 'processing',
  CANCEL_PENDING: 'orange',
  SHIPPED: 'blue',
  EXCEPTION: 'error',
  COMPLETED: 'success',
  CANCELLED: 'default',
}

async function load() {
  loading.value = true
  try {
    const [d, t] = await Promise.all([
      getAdminOrderDetail(orderId),
      getOrderTimeline(orderId, { page_size: 50 }),
    ])
    detail.value = d
    timeline.value = t.items
  } catch {
    message.error('加载工单详情失败')
    router.back()
  } finally {
    loading.value = false
  }
}

function openModal(action: ActionType) {
  currentAction.value = action
  actionReason.value = ''
  trackingNumber.value = ''
  modalVisible.value = true
}

async function submitAction() {
  if (!detail.value) return
  submitting.value = true
  try {
    const id = detail.value.order_id
    switch (currentAction.value) {
      case 'approve':
        await approveOrder(id)
        break
      case 'ship':
        await shipOrder(id, { tracking_number: trackingNumber.value })
        break
      case 'cancel_approve':
        await cancelApproveOrder(id, { reason: actionReason.value })
        break
      case 'cancel_reject':
        await cancelRejectOrder(id, { reason: actionReason.value })
        break
      case 'logistics_exception':
        await reportLogisticsException(id, { exception_desc: actionReason.value })
        break
      case 'reship':
        await reshipOrder(id, { tracking_number: trackingNumber.value })
        break
      case 'close_exception':
        await closeException(id, {})
        break
    }
    message.success('操作成功')
    modalVisible.value = false
    await load()
  } catch {
    // error handled by interceptor
  } finally {
    submitting.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.page-container {
  padding: 16px;
}
</style>
