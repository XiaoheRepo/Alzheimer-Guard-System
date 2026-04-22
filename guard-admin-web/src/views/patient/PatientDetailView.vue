<!-- src/views/patient/PatientDetailView.vue / P-15 -->
<script setup lang="ts">
import { ref, computed, onMounted, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import { getAdminPatient, forceTransferPrimary, type AdminPatientDetail } from '@/api/patient'
import PageHeader from '@/components/common/PageHeader.vue'
import StatusBadge from '@/components/domain/StatusBadge.vue'
import PermissionButton from '@/components/domain/PermissionButton.vue'
import CopyableText from '@/components/common/CopyableText.vue'
import { fmtDateTime } from '@/utils/format'
import { useAuthStore } from '@/stores/auth'
import type { ApiError } from '@/utils/request'

const route = useRoute()
const router = useRouter()
const { t } = useI18n()
const auth = useAuthStore()

const patientId = computed(() => route.params.patientId as string)
const data = ref<AdminPatientDetail | null>(null)
const loading = ref(false)
const showTransfer = ref(false)
const transferForm = reactive({
  target_user_id: '',
  reason: '',
  evidence_url: '',
  confirmText: '',
})
const submitting = ref(false)

async function load() {
  loading.value = true
  try {
    data.value = await getAdminPatient(patientId.value)
  } finally {
    loading.value = false
  }
}

onMounted(load)

async function onTransfer() {
  if (
    !transferForm.target_user_id ||
    transferForm.reason.trim().length < 30 ||
    transferForm.reason.trim().length > 500
  ) {
    message.warning(t('page.patient.transfer.invalid'))
    return
  }
  if (transferForm.confirmText !== 'CONFIRM') {
    message.warning(t('page.patient.transfer.confirmHint'))
    return
  }
  submitting.value = true
  try {
    await forceTransferPrimary(patientId.value, {
      target_user_id: transferForm.target_user_id,
      reason: transferForm.reason,
      evidence_url: transferForm.evidence_url || undefined,
    })
    message.success(t('common.success'))
    showTransfer.value = false
    await load()
  } catch (e) {
    message.error((e as ApiError)?.message || t('error.UNKNOWN'))
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="page-container" v-loading="loading">
    <PageHeader :title="t('page.patient.detail.title')">
      <template #extra>
        <a-button @click="router.back()">{{ t('common.back') }}</a-button>
        <PermissionButton
          :roles="['SUPER_ADMIN']"
          danger
          type="primary"
          @click="showTransfer = true"
        >
          {{ t('page.patient.transfer.btn') }}
        </PermissionButton>
      </template>
    </PageHeader>

    <a-card v-if="data">
      <a-descriptions bordered :column="2" size="small">
        <a-descriptions-item :label="t('page.patient.col.profileNo')">
          <CopyableText :text="data.profile_no || data.patient_id" />
        </a-descriptions-item>
        <a-descriptions-item :label="t('page.patient.col.shortCode')">
          {{ data.short_code || '-' }}
        </a-descriptions-item>
        <a-descriptions-item :label="t('page.patient.col.name')">{{
          data.patient_name
        }}</a-descriptions-item>
        <a-descriptions-item :label="t('page.patient.col.status')">
          <StatusBadge kind="patientStatus" :value="data.status" />
        </a-descriptions-item>
        <a-descriptions-item :label="t('page.patient.col.gender')">{{
          data.gender || '-'
        }}</a-descriptions-item>
        <a-descriptions-item :label="t('page.patient.col.age')">{{
          data.age ?? '-'
        }}</a-descriptions-item>
        <a-descriptions-item :label="t('page.patient.col.createdAt')">{{
          fmtDateTime(data.created_at)
        }}</a-descriptions-item>
        <a-descriptions-item :label="t('page.patient.col.tagCount')">{{
          data.bound_tag_count ?? 0
        }}</a-descriptions-item>
      </a-descriptions>
    </a-card>

    <a-card :title="t('page.patient.guardianList')" style="margin-top: 16px">
      <a-table
        v-if="data?.guardian_list?.length"
        :columns="[
          { title: t('page.patient.col.nickname'), dataIndex: 'nickname' },
          { title: t('page.patient.col.phone'), dataIndex: 'phone' },
          { title: t('page.patient.col.role'), dataIndex: 'relation_role' },
          { title: t('page.patient.col.relationStatus'), dataIndex: 'relation_status' },
        ]"
        :data-source="data.guardian_list"
        :row-key="(r: { user_id: string }) => r.user_id"
        :pagination="false"
        size="small"
      />
      <a-empty v-else />
    </a-card>

    <a-modal
      v-model:open="showTransfer"
      :title="t('page.patient.transfer.title')"
      :confirm-loading="submitting"
      :ok-text="t('common.submit')"
      :cancel-text="t('common.cancel')"
      @ok="onTransfer"
    >
      <a-alert
        type="warning"
        :message="t('page.patient.transfer.warn')"
        show-icon
        style="margin-bottom: 12px"
      />
      <a-form layout="vertical">
        <a-form-item :label="t('page.patient.transfer.target')" required>
          <a-input v-model:value="transferForm.target_user_id" />
        </a-form-item>
        <a-form-item :label="t('page.patient.transfer.reason')" required>
          <a-textarea v-model:value="transferForm.reason" :rows="4" :maxlength="500" show-count />
        </a-form-item>
        <a-form-item :label="t('page.patient.transfer.evidence')">
          <a-input v-model:value="transferForm.evidence_url" />
        </a-form-item>
        <a-form-item :label="t('page.patient.transfer.confirm')" required>
          <a-input v-model:value="transferForm.confirmText" placeholder="CONFIRM" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>
