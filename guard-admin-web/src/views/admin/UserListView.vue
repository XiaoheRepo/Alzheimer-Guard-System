<!-- src/views/admin/UserListView.vue / P-14 -->
<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { message, Modal } from 'ant-design-vue'
import {
  listAdminUsers,
  updateAdminUser,
  disableAdminUser,
  enableAdminUser,
  deleteAdminUser,
  type AdminUserListItem,
} from '@/api/admin'
import type { Role, UserStatus } from '@/types/common'
import PageHeader from '@/components/common/PageHeader.vue'
import ProTable from '@/components/common/ProTable.vue'
import StatusBadge from '@/components/domain/StatusBadge.vue'
import PermissionButton from '@/components/domain/PermissionButton.vue'
import CopyableText from '@/components/common/CopyableText.vue'
import { fmtDateTime, maskPhone, maskEmail } from '@/utils/format'
import type { ApiError } from '@/utils/request'

const { t } = useI18n()

const filter = reactive({
  role: undefined as Role | undefined,
  status: undefined as UserStatus | undefined,
  keyword: '',
})
const data = ref<AdminUserListItem[]>([])
const loading = ref(false)
const nextCursor = ref('')
const cursorHist = ref<string[]>([''])
const cursorIdx = ref(0)

const editDlg = reactive({
  open: false,
  user: null as AdminUserListItem | null,
  nickname: '',
  email: '',
  phone: '',
  role: 'ADMIN' as Role,
  submitting: false,
})

const columns = [
  { title: t('page.user.col.username'), key: 'username', width: 140 },
  { title: t('page.user.col.nickname'), dataIndex: 'nickname', width: 140 },
  { title: t('page.user.col.role'), key: 'role', width: 120 },
  { title: t('page.user.col.status'), key: 'status', width: 100 },
  { title: t('page.user.col.email'), key: 'email' },
  { title: t('page.user.col.phone'), key: 'phone', width: 140 },
  { title: t('page.user.col.lastLogin'), key: 'last_login_at', width: 180 },
  { title: t('common.action'), key: 'action', width: 220, fixed: 'right' },
]

async function load() {
  loading.value = true
  try {
    const res = await listAdminUsers({
      role: filter.role,
      status: filter.status,
      keyword: filter.keyword || undefined,
      cursor: cursorHist.value[cursorIdx.value] || undefined,
      size: 30,
    })
    data.value = (res as { items?: AdminUserListItem[] }).items || []
    nextCursor.value = (res as { next_cursor?: string }).next_cursor || ''
  } finally {
    loading.value = false
  }
}

onMounted(load)

function openEdit(u: AdminUserListItem) {
  editDlg.user = u
  editDlg.nickname = u.nickname
  editDlg.email = u.email || ''
  editDlg.phone = u.phone || ''
  editDlg.role = u.role
  editDlg.open = true
}

async function onSave() {
  if (!editDlg.user) return
  editDlg.submitting = true
  try {
    await updateAdminUser(editDlg.user.user_id, {
      nickname: editDlg.nickname,
      email: editDlg.email,
      phone: editDlg.phone,
      role: editDlg.role,
    })
    message.success(t('common.success'))
    editDlg.open = false
    await load()
  } catch (e) {
    message.error((e as ApiError)?.message || t('error.UNKNOWN'))
  } finally {
    editDlg.submitting = false
  }
}

async function onDisable(u: AdminUserListItem) {
  Modal.confirm({
    title: t('page.user.disable.title'),
    content: t('page.user.disable.content', { name: u.username }),
    okText: t('common.confirm'),
    cancelText: t('common.cancel'),
    onOk: async () => {
      try {
        await disableAdminUser(u.user_id, { reason: 'admin disable' })
        message.success(t('common.success'))
        await load()
      } catch (e) {
        message.error((e as ApiError)?.message || t('error.UNKNOWN'))
      }
    },
  })
}

async function onEnable(u: AdminUserListItem) {
  try {
    await enableAdminUser(u.user_id)
    message.success(t('common.success'))
    await load()
  } catch (e) {
    message.error((e as ApiError)?.message || t('error.UNKNOWN'))
  }
}

async function onDelete(u: AdminUserListItem) {
  Modal.confirm({
    title: t('page.user.delete.title'),
    content: t('page.user.delete.content', { name: u.username }),
    okType: 'danger',
    okText: t('common.confirm'),
    cancelText: t('common.cancel'),
    onOk: async () => {
      try {
        await deleteAdminUser(u.user_id, { reason: 'admin delete' })
        message.success(t('common.success'))
        await load()
      } catch (e) {
        message.error((e as ApiError)?.message || t('error.UNKNOWN'))
      }
    },
  })
}

function doSearch() {
  cursorHist.value = ['']
  cursorIdx.value = 0
  load()
}
function nextPage() {
  if (!nextCursor.value) return
  cursorHist.value.push(nextCursor.value)
  cursorIdx.value += 1
  load()
}
function prevPage() {
  if (cursorIdx.value === 0) return
  cursorIdx.value -= 1
  load()
}
</script>

<template>
  <div class="page-container">
    <PageHeader :title="t('menu.adminUser')" />
    <ProTable
      :columns="columns"
      :data-source="data"
      :loading="loading"
      :row-key="(r: AdminUserListItem) => r.user_id"
      :pagination="false"
      :scroll="{ x: 1400 }"
    >
      <template #filter>
        <a-form layout="inline">
          <a-form-item :label="t('page.user.col.role')">
            <a-select v-model:value="filter.role" allow-clear style="width: 160px">
              <a-select-option value="FAMILY">{{ t('field.userRole.FAMILY') }}</a-select-option>
              <a-select-option value="ADMIN">{{ t('field.userRole.ADMIN') }}</a-select-option>
              <a-select-option value="SUPER_ADMIN">{{ t('field.userRole.SUPER_ADMIN') }}</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item :label="t('page.user.col.status')">
            <a-select v-model:value="filter.status" allow-clear style="width: 160px">
              <a-select-option value="ACTIVE">{{ t('field.userStatus.ACTIVE') }}</a-select-option>
              <a-select-option value="DISABLED">{{ t('field.userStatus.DISABLED') }}</a-select-option>
              <a-select-option value="DEACTIVATED">{{ t('field.userStatus.DEACTIVATED') }}</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item :label="t('common.keyword')">
            <a-input v-model:value="filter.keyword" allow-clear />
          </a-form-item>
          <a-form-item>
            <a-button type="primary" @click="doSearch">{{ t('common.search') }}</a-button>
          </a-form-item>
        </a-form>
      </template>

      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'username'">
          <CopyableText :text="record.username" />
        </template>
        <template v-else-if="column.key === 'role'">
          <StatusBadge kind="userRole" :value="record.role" />
        </template>
        <template v-else-if="column.key === 'status'">
          <StatusBadge kind="userStatus" :value="record.status" />
        </template>
        <template v-else-if="column.key === 'email'">
          {{ maskEmail(record.email) }}
        </template>
        <template v-else-if="column.key === 'phone'">
          {{ maskPhone(record.phone) }}
        </template>
        <template v-else-if="column.key === 'last_login_at'">
          {{ fmtDateTime(record.last_login_at) }}
        </template>
        <template v-else-if="column.key === 'action'">
          <a-space size="small">
            <PermissionButton
              type="link"
              size="small"
              :roles="['ADMIN', 'SUPER_ADMIN']"
              @click="openEdit(record)"
            >{{ t('common.edit') }}</PermissionButton>
            <PermissionButton
              v-if="record.status === 'ACTIVE'"
              type="link"
              size="small"
              :roles="['SUPER_ADMIN']"
              @click="onDisable(record)"
            >{{ t('page.user.disable.btn') }}</PermissionButton>
            <PermissionButton
              v-else-if="record.status === 'DISABLED'"
              type="link"
              size="small"
              :roles="['SUPER_ADMIN']"
              @click="onEnable(record)"
            >{{ t('page.user.enable.btn') }}</PermissionButton>
            <PermissionButton
              danger
              type="link"
              size="small"
              :roles="['SUPER_ADMIN']"
              @click="onDelete(record)"
            >{{ t('common.delete') }}</PermissionButton>
          </a-space>
        </template>
      </template>
    </ProTable>

    <div style="margin-top: 12px; text-align: right">
      <a-space>
        <a-button :disabled="cursorIdx === 0" @click="prevPage">{{ t('common.prev') }}</a-button>
        <a-button :disabled="!nextCursor" type="primary" @click="nextPage">{{ t('common.next') }}</a-button>
      </a-space>
    </div>

    <a-modal
      v-model:open="editDlg.open"
      :title="t('page.user.edit.title')"
      :confirm-loading="editDlg.submitting"
      :ok-text="t('common.submit')"
      :cancel-text="t('common.cancel')"
      @ok="onSave"
    >
      <a-form v-if="editDlg.user" layout="vertical">
        <a-form-item :label="t('page.user.col.username')">
          <a-typography-text code>{{ editDlg.user.username }}</a-typography-text>
        </a-form-item>
        <a-form-item :label="t('page.user.col.nickname')" required>
          <a-input v-model:value="editDlg.nickname" />
        </a-form-item>
        <a-form-item :label="t('page.user.col.email')">
          <a-input v-model:value="editDlg.email" />
        </a-form-item>
        <a-form-item :label="t('page.user.col.phone')">
          <a-input v-model:value="editDlg.phone" />
        </a-form-item>
        <a-form-item :label="t('page.user.col.role')">
          <PermissionButton
            v-if="false"
            :roles="['SUPER_ADMIN']"
            style="display: none"
          />
          <a-select v-model:value="editDlg.role">
            <a-select-option value="FAMILY">FAMILY</a-select-option>
            <a-select-option value="ADMIN">ADMIN</a-select-option>
            <a-select-option value="SUPER_ADMIN">SUPER_ADMIN</a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>
