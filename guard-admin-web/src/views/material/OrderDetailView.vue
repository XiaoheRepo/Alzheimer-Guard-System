<!-- src/views/material/OrderDetailView.vue / P-06b -->
<script setup lang="ts">
import { ref, computed, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { message, Modal } from 'ant-design-vue'
import {
  listOrders,
  approveOrder,
  shipOrder,
  resolveOrderException,
  cancelOrder,
  type MaterialOrder,
} from '@/api/material'
import PageHeader from '@/components/common/PageHeader.vue'
import StatusBadge from '@/components/domain/StatusBadge.vue'
import CopyableText from '@/components/common/CopyableText.vue'
import PermissionButton from '@/components/domain/PermissionButton.vue'
import { fmtDateTime } from '@/utils/format'
import type { ApiError } from '@/utils/request'

const route = useRoute()
const router = useRouter()
const { t } = useI18n()

const orderId = computed(() => route.params.orderId as string)
const data = ref<MaterialOrder | null>(null)
const loading = ref(false)

const shipDlg = reactive({ open: false, carrier: '', tracking_no: '', submitting: false })
const excDlg = reactive({
  open: false,
  action: 'RESHIP' as 'RESHIP' | 'VOID',
  reason: '',
  new_carrier: '',
  new_tracking_no: '',
  submitting: false,
})

async function load() {
  loading.value = true
  try {
    // 后端无单项 GET，使用 list 精确过滤
    const res = await listOrders({ order_id: orderId.value, size: 1 })
    const items = (res as { items?: MaterialOrder[] }).items || []
    data.value = items[0] || null
  } finally {
    loading.value = false
  }
}

onMounted(load)

async function onApprove() {
  Modal.confirm({
    title: t('page.material.approve.title'),
    content: t('page.material.approve.content'),
    okText: t('common.confirm'),
    cancelText: t('common.cancel'),
    onOk: async () => {
      try {
        await approveOrder(orderId.value, { request_time: new Date().toISOString() })
        message.success(t('common.success'))
        await load()
      } catch (e) {
        message.error((e as ApiError)?.message || t('error.UNKNOWN'))
      }
    },
  })
}

async function onShipSubmit() {
  if (!shipDlg.carrier || !shipDlg.tracking_no) {
    message.warning(t('page.material.ship.fill'))
    return
  }
  shipDlg.submitting = true
  try {
    await shipOrder(orderId.value, {
      carrier: shipDlg.carrier,
      tracking_no: shipDlg.tracking_no,
      request_time: new Date().toISOString(),
    })
    message.success(t('common.success'))
    shipDlg.open = false
    await load()
  } catch (e) {
    message.error((e as ApiError)?.message || t('error.UNKNOWN'))
  } finally {
    shipDlg.submitting = false
  }
}

async function onExcSubmit() {
  if (excDlg.reason.trim().length < 10) {
    message.warning(t('page.material.exc.reasonMin'))
    return
  }
  excDlg.submitting = true
  try {
    await resolveOrderException(orderId.value, {
      action: excDlg.action,
      reason: excDlg.reason,
      new_carrier: excDlg.new_carrier || undefined,
      new_tracking_no: excDlg.new_tracking_no || undefined,
      request_time: new Date().toISOString(),
    })
    message.success(t('common.success'))
    excDlg.open = false
    await load()
  } catch (e) {
    message.error((e as ApiError)?.message || t('error.UNKNOWN'))
  } finally {
    excDlg.submitting = false
  }
}

async function onCancel() {
  Modal.confirm({
    title: t('page.material.cancel.title'),
    content: t('page.material.cancel.content'),
    okText: t('common.confirm'),
    cancelText: t('common.cancel'),
    onOk: async () => {
      try {
        await cancelOrder(orderId.value, {
          reason: 'admin cancel',
          request_time: new Date().toISOString(),
        })
        message.success(t('common.success'))
        await load()
      } catch (e) {
        message.error((e as ApiError)?.message || t('error.UNKNOWN'))
      }
    },
  })
}
</script>

<template>
  <div class="page-container" v-loading="loading">
    <PageHeader :title="t('page.material.order.title')">
      <template #extra>
        <a-button @click="router.back()">{{ t('common.back') }}</a-button>
        <PermissionButton
          v-if="data?.state === 'PENDING_AUDIT'"
          type="primary"
          :roles="['ADMIN', 'SUPER_ADMIN']"
          @click="onApprove"
        >
          {{ t('page.material.approve.btn') }}
        </PermissionButton>
        <PermissionButton
          v-if="data?.state === 'PENDING_SHIP'"
          type="primary"
          :roles="['ADMIN', 'SUPER_ADMIN']"
          @click="shipDlg.open = true"
        >
          {{ t('page.material.ship.btn') }}
        </PermissionButton>
        <PermissionButton
          v-if="data?.state === 'EXCEPTION'"
          type="primary"
          :roles="['ADMIN', 'SUPER_ADMIN']"
          @click="excDlg.open = true"
        >
          {{ t('page.material.exc.btn') }}
        </PermissionButton>
        <PermissionButton
          v-if="data && ['PENDING_AUDIT', 'PENDING_SHIP'].includes(data.state)"
          danger
          :roles="['ADMIN', 'SUPER_ADMIN']"
          @click="onCancel"
        >
          {{ t('page.material.cancel.btn') }}
        </PermissionButton>
      </template>
    </PageHeader>

    <a-card v-if="data">
      <a-descriptions bordered :column="2" size="small">
        <a-descriptions-item :label="t('page.material.col.orderId')">
          <CopyableText :text="data.order_id" />
        </a-descriptions-item>
        <a-descriptions-item :label="t('page.material.col.state')">
          <StatusBadge kind="orderState" :value="data.state" />
        </a-descriptions-item>
        <a-descriptions-item :label="t('page.material.col.patient')">
          {{ data.patient?.patient_name || data.patient_id }}
        </a-descriptions-item>
        <a-descriptions-item :label="t('page.material.col.createdAt')">
          {{ fmtDateTime(data.created_at) }}
        </a-descriptions-item>
        <a-descriptions-item :label="t('page.material.col.items')" :span="2">
          <a-tag v-for="(it, i) in data.items" :key="i" style="margin-right: 8px">
            {{ it.material_type }} × {{ it.quantity }}
          </a-tag>
        </a-descriptions-item>
        <a-descriptions-item
          :label="t('page.material.col.receiver')"
          :span="2"
          v-if="data.receiver"
        >
          {{ data.receiver.name }} · {{ data.receiver.phone }} · {{ data.receiver.address }}
        </a-descriptions-item>
        <a-descriptions-item
          :label="t('page.material.col.logistics')"
          :span="2"
          v-if="data.logistics?.tracking_no"
        >
          {{ data.logistics.carrier }} · {{ data.logistics.tracking_no }}
          <span v-if="data.logistics.shipped_at" class="text-muted">
            · {{ fmtDateTime(data.logistics.shipped_at) }}
          </span>
        </a-descriptions-item>
      </a-descriptions>
    </a-card>

    <a-modal
      v-model:open="shipDlg.open"
      :title="t('page.material.ship.title')"
      :confirm-loading="shipDlg.submitting"
      :ok-text="t('common.submit')"
      :cancel-text="t('common.cancel')"
      @ok="onShipSubmit"
    >
      <a-form layout="vertical">
        <a-form-item :label="t('page.material.ship.carrier')" required>
          <a-input v-model:value="shipDlg.carrier" />
        </a-form-item>
        <a-form-item :label="t('page.material.ship.trackingNo')" required>
          <a-input v-model:value="shipDlg.tracking_no" />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="excDlg.open"
      :title="t('page.material.exc.title')"
      :confirm-loading="excDlg.submitting"
      :ok-text="t('common.submit')"
      :cancel-text="t('common.cancel')"
      @ok="onExcSubmit"
    >
      <a-form layout="vertical">
        <a-form-item :label="t('page.material.exc.action')" required>
          <a-radio-group v-model:value="excDlg.action">
            <a-radio value="RESHIP">{{ t('page.material.exc.reship') }}</a-radio>
            <a-radio value="VOID">{{ t('page.material.exc.void') }}</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item :label="t('page.material.exc.reason')" required>
          <a-textarea v-model:value="excDlg.reason" :rows="3" :maxlength="500" show-count />
        </a-form-item>
        <template v-if="excDlg.action === 'RESHIP'">
          <a-form-item :label="t('page.material.ship.carrier')">
            <a-input v-model:value="excDlg.new_carrier" />
          </a-form-item>
          <a-form-item :label="t('page.material.ship.trackingNo')">
            <a-input v-model:value="excDlg.new_tracking_no" />
          </a-form-item>
        </template>
      </a-form>
    </a-modal>
  </div>
</template>
