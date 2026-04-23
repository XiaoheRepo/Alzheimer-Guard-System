<!-- src/views/admin/UserListView.vue / P-14 -->
<script setup lang="ts">
import { reactive, ref, onMounted, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import {
  listAdminUsers,
  updateAdminUser,
  disableAdminUser,
  enableAdminUser,
  deleteAdminUser,
  createAdmin,
  type AdminUserListItem,
} from '@/api/admin'
import type { Role, UserStatus } from '@/types/common'
import { useAuthStore } from '@/stores/auth'
import PageHeader from '@/components/common/PageHeader.vue'
import ProTable from '@/components/common/ProTable.vue'
import StatusBadge from '@/components/domain/StatusBadge.vue'
import PermissionButton from '@/components/domain/PermissionButton.vue'
import CopyableText from '@/components/common/CopyableText.vue'
import { fmtDateTime, maskPhone, maskEmail } from '@/utils/format'
import type { ApiError } from '@/utils/request'

const { t } = useI18n()
const auth = useAuthStore()
const canEditRole = computed(() => auth.isSuperAdmin)

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

const createDlg = reactive({
  open: false,
  username: '',
  email: '',
  nickname: '',
  reason: '',
  submitting: false,
})

const disableDlg = reactive({
  open: false,
  target: null as AdminUserListItem | null,
  reason: '',
  submitting: false,
})

const deleteDlg = reactive({
  open: false,
  target: null as AdminUserListItem | null,
  reason: '',
  submitting: false,
})

const enableDlg = reactive({
  open: false,
  target: null as AdminUserListItem | null,
  reason: '',
  submitting: false,
})

const tempPwdDlg = reactive({
  open: false,
  username: '',
  tempPassword: '',
  note: '',
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
  const roleChanged = editDlg.role !== editDlg.user.role
  try {
    await updateAdminUser(
      editDlg.user.user_id,
      {
        nickname: editDlg.nickname,
        email: editDlg.email,
        phone: editDlg.phone,
        role: editDlg.role,
      },
      roleChanged ? 'CONFIRM_2' : undefined,
    )
    message.success(t('common.success'))
    editDlg.open = false
    await load()
  } catch (e) {
    message.error((e as ApiError)?.message || t('error.UNKNOWN'))
  } finally {
    editDlg.submitting = false
  }
}

async function onCreate() {
  if (!createDlg.username.trim() || !createDlg.email.trim()) {
    message.warning(t('common.fillRequired'))
    return
  }
  if (createDlg.reason.trim().length < 10) {
    message.warning(t('page.user.create.reasonMin'))
    return
  }
  createDlg.submitting = true
  try {
    const res = await createAdmin({
      username: createDlg.username,
      email: createDlg.email,
      nickname: createDlg.nickname || undefined,
      reason: createDlg.reason,
    })
    createDlg.open = false
    Object.assign(createDlg, { username: '', email: '', nickname: '', reason: '' })
    tempPwdDlg.username = res.username
    tempPwdDlg.tempPassword = res.temp_password
    tempPwdDlg.note = res.temp_password_note
    tempPwdDlg.open = true
    await load()
  } catch (e) {
    message.error((e as ApiError)?.message || t('error.UNKNOWN'))
  } finally {
    createDlg.submitting = false
  }
}

function openDisable(u: AdminUserListItem) {
  disableDlg.target = u
  disableDlg.reason = ''
  disableDlg.open = true
}

async function onDisableSubmit() {
  if (!disableDlg.target) return
  disableDlg.submitting = true
  try {
    await disableAdminUser(disableDlg.target.user_id, { reason: disableDlg.reason.trim() })
    message.success(t('common.success'))
    disableDlg.open = false
    await load()
  } catch (e) {
    message.error((e as ApiError)?.message || t('error.UNKNOWN'))
  } finally {
    disableDlg.submitting = false
  }
}

function openEnable(u: AdminUserListItem) {
  enableDlg.target = u
  enableDlg.reason = ''
  enableDlg.open = true
}

async function onEnableSubmit() {
  if (!enableDlg.target) return
  enableDlg.submitting = true
  try {
    await enableAdminUser(enableDlg.target.user_id, { reason: enableDlg.reason.trim() })
    message.success(t('common.success'))
    enableDlg.open = false
    await load()
  } catch (e) {
    message.error((e as ApiError)?.message || t('error.UNKNOWN'))
  } finally {
    enableDlg.submitting = false
  }
}

function openDelete(u: AdminUserListItem) {
  deleteDlg.target = u
  deleteDlg.reason = ''
  deleteDlg.open = true
}

async function onDeleteSubmit() {
  if (deleteDlg.reason.trim().length < 20) {
    message.warning(t('page.user.delete.reasonMin'))
    return
  }
  if (!deleteDlg.target) return
  deleteDlg.submitting = true
  try {
    await deleteAdminUser(deleteDlg.target.user_id, { reason: deleteDlg.reason.trim() })
    message.success(t('common.success'))
    deleteDlg.open = false
    await load()
  } catch (e) {
    message.error((e as ApiError)?.message || t('error.UNKNOWN'))
  } finally {
    deleteDlg.submitting = false
  }
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
              <a-select-option value="SUPER_ADMIN">{{
                t('field.userRole.SUPER_ADMIN')
              }}</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item :label="t('page.user.col.status')">
            <a-select v-model:value="filter.status" allow-clear style="width: 160px">
              <a-select-option value="ACTIVE">{{ t('field.userStatus.ACTIVE') }}</a-select-option>
              <a-select-option value="DISABLED">{{
                t('field.userStatus.DISABLED')
              }}</a-select-option>
              <a-select-option value="DEACTIVATED">{{
                t('field.userStatus.DEACTIVATED')
              }}</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item :label="t('common.keyword')">
            <a-input v-model:value="filter.keyword" allow-clear />
          </a-form-item>
          <a-form-item>
            <a-button type="primary" @click="doSearch">{{ t('common.search') }}</a-button>
          </a-form-item>
          <a-form-item v-if="auth.isSuperAdmin">
            <a-button type="primary" @click="createDlg.open = true">
              + {{ t('page.user.create.btn') }}
            </a-button>
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
              >{{ t('common.edit') }}</PermissionButton
            >
            <PermissionButton
              v-if="record.status === 'ACTIVE'"
              type="link"
              size="small"
              :roles="['SUPER_ADMIN']"
              @click="openDisable(record)"
              >{{ t('page.user.disable.btn') }}</PermissionButton
            >
            <PermissionButton
              v-else-if="record.status === 'DISABLED'"
              type="link"
              size="small"
              :roles="['SUPER_ADMIN']"
              @click="openEnable(record)"
              >{{ t('page.user.enable.btn') }}</PermissionButton
            >
            <PermissionButton
              danger
              type="link"
              size="small"
              :roles="['SUPER_ADMIN']"
              @click="openDelete(record)"
              >{{ t('common.delete') }}</PermissionButton
            >
          </a-space>
        </template>
      </template>
    </ProTable>

    <div style="margin-top: 12px; text-align: right">
      <a-space>
        <a-button :disabled="cursorIdx === 0" @click="prevPage">{{ t('common.prev') }}</a-button>
        <a-button :disabled="!nextCursor" type="primary" @click="nextPage">{{
          t('common.next')
        }}</a-button>
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
          <a-select v-model:value="editDlg.role" :disabled="!canEditRole">
            <a-select-option value="FAMILY">FAMILY</a-select-option>
            <a-select-option value="ADMIN">ADMIN</a-select-option>
            <a-select-option value="SUPER_ADMIN">SUPER_ADMIN</a-select-option>
          </a-select>
          <div v-if="!canEditRole" class="text-muted" style="font-size: 12px">
            {{ t('page.user.roleEditOnlySuper') }}
          </div>
        </a-form-item>
      </a-form>
    </a-modal>
    <!-- 新增管理员弹窗（仅 SUPER_ADMIN 可用） -->
    <a-modal
      v-model:open="createDlg.open"
      :title="t('page.user.create.title')"
      :confirm-loading="createDlg.submitting"
      :ok-text="t('common.submit')"
      :cancel-text="t('common.cancel')"
      @ok="onCreate"
    >
      <a-form layout="vertical">
        <a-form-item :label="t('page.user.col.username')" required>
          <a-input
            v-model:value="createDlg.username"
            :placeholder="t('page.user.create.usernamePlaceholder')"
          />
        </a-form-item>
        <a-form-item :label="t('page.user.col.email')" required>
          <a-input v-model:value="createDlg.email" placeholder="admin@org.com" />
        </a-form-item>
        <a-form-item :label="t('page.user.col.nickname')">
          <a-input v-model:value="createDlg.nickname" />
        </a-form-item>
        <a-form-item :label="t('page.user.create.reason')" required>
          <a-textarea
            v-model:value="createDlg.reason"
            :rows="3"
            :placeholder="t('page.user.create.reasonPlaceholder')"
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 启用确认弹窗 -->
    <a-modal
      v-model:open="enableDlg.open"
      :title="t('page.user.enable.title')"
      :confirm-loading="enableDlg.submitting"
      :ok-text="t('common.confirm')"
      :cancel-text="t('common.cancel')"
      @ok="onEnableSubmit"
    >
      <p>{{ t('page.user.enable.content', { name: enableDlg.target?.username }) }}</p>
      <a-form layout="vertical" style="margin-top: 12px">
        <a-form-item :label="t('page.user.enable.reason')">
          <a-textarea
            v-model:value="enableDlg.reason"
            :rows="3"
            :placeholder="t('page.user.enable.reasonPlaceholder')"
            :maxlength="256"
            show-count
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 禁用确认弹窗 -->
    <a-modal
      v-model:open="disableDlg.open"
      :title="t('page.user.disable.title')"
      :confirm-loading="disableDlg.submitting"
      :ok-text="t('common.confirm')"
      :cancel-text="t('common.cancel')"
      ok-type="danger"
      @ok="onDisableSubmit"
    >
      <p>{{ t('page.user.disable.content', { name: disableDlg.target?.username }) }}</p>
      <a-form layout="vertical" style="margin-top: 12px">
        <a-form-item :label="t('page.user.disable.reason')">
          <a-textarea
            v-model:value="disableDlg.reason"
            :rows="3"
            :placeholder="t('page.user.disable.reasonPlaceholder')"
            :maxlength="256"
            show-count
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 删除确认弹窗 -->
    <a-modal
      v-model:open="deleteDlg.open"
      :title="t('page.user.delete.title')"
      :confirm-loading="deleteDlg.submitting"
      :ok-text="t('common.confirm')"
      :cancel-text="t('common.cancel')"
      ok-type="danger"
      @ok="onDeleteSubmit"
    >
      <a-alert
        type="error"
        :message="t('page.user.delete.content', { name: deleteDlg.target?.username })"
        show-icon
        style="margin-bottom: 12px"
      />
      <a-form layout="vertical">
        <a-form-item :label="t('page.user.delete.reason')" required>
          <a-textarea
            v-model:value="deleteDlg.reason"
            :rows="3"
            :placeholder="t('page.user.delete.reasonPlaceholder')"
            :maxlength="256"
            show-count
          />
          <div class="text-muted" style="font-size: 12px">
            {{ t('page.user.delete.reasonMin') }}
          </div>
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 初始密码一次性展示弹窗 -->
    <a-modal
      v-model:open="tempPwdDlg.open"
      :title="t('page.user.create.tempPwdTitle')"
      :footer="null"
    >
      <a-alert type="warning" :message="tempPwdDlg.note" show-icon style="margin-bottom: 16px" />
      <a-descriptions :column="1" bordered>
        <a-descriptions-item :label="t('page.user.col.username')">
          <CopyableText :text="tempPwdDlg.username" />
        </a-descriptions-item>
        <a-descriptions-item :label="t('page.user.create.tempPwd')">
          <CopyableText :text="tempPwdDlg.tempPassword" />
        </a-descriptions-item>
      </a-descriptions>
      <div style="margin-top: 16px; text-align: right">
        <a-button type="primary" @click="tempPwdDlg.open = false">{{
          t('common.confirm')
        }}</a-button>
      </div>
    </a-modal>
  </div>
</template>
