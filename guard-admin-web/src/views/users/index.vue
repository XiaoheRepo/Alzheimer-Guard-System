<template>
  <div class="page-container">
    <a-card :bordered="false">
      <!-- 筛选栏 -->
      <a-space wrap style="margin-bottom: 12px">
        <a-input
          v-model:value="params.keyword"
          placeholder="搜索用户名/ID"
          allow-clear
          style="width: 180px"
          @press-enter="() => { params.page_no = 1; load() }"
        />
        <a-select
          v-model:value="params.role"
          placeholder="角色"
          allow-clear
          style="width: 140px"
          @change="() => { params.page_no = 1; load() }"
        >
          <a-select-option value="ADMIN">管理员</a-select-option>
          <a-select-option value="SUPERADMIN">超级管理员</a-select-option>
          <a-select-option value="FAMILY">家属</a-select-option>
          <a-select-option value="PATIENT">患者</a-select-option>
        </a-select>
        <a-select
          v-model:value="params.status"
          placeholder="状态"
          allow-clear
          style="width: 120px"
          @change="() => { params.page_no = 1; load() }"
        >
          <a-select-option value="NORMAL">正常</a-select-option>
          <a-select-option value="BANNED">封禁</a-select-option>
        </a-select>
        <a-button type="primary" @click="() => { params.page_no = 1; load() }">查询</a-button>
        <a-button @click="resetFilters">重置</a-button>
      </a-space>

      <a-table
        :columns="columns"
        :data-source="items"
        :loading="loading"
        :pagination="{
          current: params.page_no,
          pageSize: params.page_size,
          total: total,
          showSizeChanger: true,
          showTotal: (t: number) => `共 ${t} 条`,
        }"
        row-key="user_id"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-tag :color="record.status === 'BANNED' ? 'error' : 'success'">
              {{ record.status === 'BANNED' ? '封禁' : '正常' }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a-button
                type="link"
                size="small"
                :danger="record.status === 'NORMAL'"
                @click="openStatusModal(record)"
              >
                {{ record.status === 'NORMAL' ? '封禁' : '解封' }}
              </a-button>
              <a-button
                v-if="isSuperAdmin"
                type="link"
                size="small"
                @click="openResetPwdModal(record)"
              >重置密码</a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 封禁/解封弹窗 -->
    <a-modal
      v-model:open="statusModalVisible"
      :title="currentUser?.status === 'NORMAL' ? '封禁用户' : '解封用户'"
      :ok-button-props="{ loading: statusSubmitting, disabled: statusReason.length < 5 }"
      :ok-type="currentUser?.status === 'NORMAL' ? 'danger' : 'primary'"
      @ok="submitStatus"
    >
      <p>用户：{{ currentUser?.username }}（{{ currentUser?.user_id }}）</p>
      <a-form layout="vertical">
        <a-form-item label="原因（5-256 字）" required>
          <a-textarea
            v-model:value="statusReason"
            :rows="3"
            :maxlength="256"
            show-count
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 重置密码弹窗（仅 SUPERADMIN） -->
    <a-modal
      v-model:open="resetPwdModalVisible"
      title="重置用户密码"
      :ok-button-props="{ loading: resetPwdSubmitting, disabled: resetPwdReason.length < 5, danger: true }"
      ok-text="确认重置"
      @ok="submitResetPwd"
    >
      <p>用户：{{ currentUser?.username }}（{{ currentUser?.user_id }}）</p>
      <a-alert
        message="重置后用户密码将变为系统默认密码，请告知用户及时修改。"
        type="warning"
        show-icon
        style="margin-bottom: 12px"
      />
      <a-form layout="vertical">
        <a-form-item label="操作原因（5-256 字）" required>
          <a-textarea
            v-model:value="resetPwdReason"
            :rows="3"
            :maxlength="256"
            show-count
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import {
  getUserList,
  updateUserStatus,
  resetUserPassword,
  type AdminUserItem,
  type UserListParams,
} from '@/api/users'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const isSuperAdmin = authStore.isSuperAdmin

const params = reactive<UserListParams & { page_no: number; page_size: number }>({
  page_no: 1,
  page_size: 20,
})
const items = ref<AdminUserItem[]>([])
const total = ref(0)
const loading = ref(false)

const columns = [
  { title: '用户 ID', dataIndex: 'user_id', key: 'user_id', width: 120 },
  { title: '用户名', dataIndex: 'username', key: 'username', width: 140 },
  { title: '角色', dataIndex: 'role', key: 'role', width: 120 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 80 },
  { title: '最近登录', dataIndex: 'last_login_at', key: 'last_login_at', width: 180 },
  { title: '创建时间', dataIndex: 'created_at', key: 'created_at', width: 180 },
  { title: '操作', key: 'action', width: 160, fixed: 'right' as const },
]

async function load() {
  loading.value = true
  try {
    const res = await getUserList(params)
    items.value = res.items
    total.value = res.total
  } catch {
    message.error('加载用户列表失败')
  } finally {
    loading.value = false
  }
}

function handleTableChange(pagination: { current: number; pageSize: number }) {
  params.page_no = pagination.current
  params.page_size = pagination.pageSize
  load()
}

function resetFilters() {
  params.keyword = undefined
  params.role = undefined
  params.status = undefined
  params.page_no = 1
  load()
}

// ── 封禁/解封 ─────────────────────────────────────────────────────
const statusModalVisible = ref(false)
const currentUser = ref<AdminUserItem | null>(null)
const statusReason = ref('')
const statusSubmitting = ref(false)

function openStatusModal(user: AdminUserItem) {
  currentUser.value = user
  statusReason.value = ''
  statusModalVisible.value = true
}

async function submitStatus() {
  if (!currentUser.value) return
  statusSubmitting.value = true
  try {
    const newStatus = currentUser.value.status === 'NORMAL' ? 'BANNED' : 'NORMAL'
    await updateUserStatus(currentUser.value.user_id, {
      status: newStatus,
      reason: statusReason.value,
    })
    message.success(newStatus === 'BANNED' ? '用户已封禁' : '用户已解封')
    statusModalVisible.value = false
    await load()
  } catch {
    // error handled by interceptor
  } finally {
    statusSubmitting.value = false
  }
}

// ── 重置密码 ──────────────────────────────────────────────────────
const resetPwdModalVisible = ref(false)
const resetPwdReason = ref('')
const resetPwdSubmitting = ref(false)

function openResetPwdModal(user: AdminUserItem) {
  currentUser.value = user
  resetPwdReason.value = ''
  resetPwdModalVisible.value = true
}

async function submitResetPwd() {
  if (!currentUser.value) return
  resetPwdSubmitting.value = true
  try {
    await resetUserPassword(currentUser.value.user_id, { reason: resetPwdReason.value })
    message.success('密码已重置')
    resetPwdModalVisible.value = false
  } catch {
    // error handled by interceptor
  } finally {
    resetPwdSubmitting.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.page-container {
  padding: 16px;
}
</style>
