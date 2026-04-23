<!-- src/views/me/ProfileView.vue / P-12 -->
<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import { UserOutlined, LockOutlined, SettingOutlined, EditOutlined } from '@ant-design/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { useAppStore, type ThemeMode } from '@/stores/app'
import { changePassword } from '@/api/me'
import type { ApiError } from '@/utils/request'
import type { AppLocale } from '@/locales'
import { fmtDateTime } from '@/utils/format'

const { t } = useI18n()
const auth = useAuthStore()
const app = useAppStore()

// ---------- 头像 ----------
const avatarText = computed(() => {
  const name = auth.user?.nickname || auth.user?.username || '?'
  return name.charAt(0).toUpperCase()
})

const roleColorMap: Record<string, string> = {
  SUPER_ADMIN: '#f97316',
  ADMIN: '#0ea5e9',
  FAMILY: '#22c55e',
}
const roleColor = computed(() => roleColorMap[auth.user?.role ?? ''] ?? '#999')

// ---------- 编辑资料对话框 ----------
const editDlg = reactive({
  open: false,
  nickname: '',
  email: '',
  phone: '',
})

function openEdit() {
  editDlg.nickname = auth.user?.nickname ?? ''
  editDlg.email = auth.user?.email ?? ''
  editDlg.phone = auth.user?.phone ?? ''
  editDlg.open = true
}

function onEditOk() {
  // 后端暂未提供 PATCH /users/me，功能待接入
  message.info('个人信息修改功能待后端接口上线')
  editDlg.open = false
}

// ---------- 修改密码对话框 ----------
const pwdDlg = ref(false)
const pwdForm = reactive({ old_password: '', new_password: '', confirm: '' })
const submitting = ref(false)

function openPwd() {
  pwdForm.old_password = ''
  pwdForm.new_password = ''
  pwdForm.confirm = ''
  pwdDlg.value = true
}

async function onPwdSubmit() {
  if (!pwdForm.old_password || !pwdForm.new_password) {
    message.warning(t('page.me.pwd.fillAll'))
    return
  }
  if (pwdForm.new_password !== pwdForm.confirm) {
    message.warning(t('page.auth.reset.mismatch'))
    return
  }
  if (pwdForm.new_password.length < 8) {
    message.warning(t('page.me.pwd.tooShort'))
    return
  }
  submitting.value = true
  try {
    await changePassword({
      old_password: pwdForm.old_password,
      new_password: pwdForm.new_password,
      request_time: new Date().toISOString(),
    })
    message.success(t('common.success'))
    pwdDlg.value = false
  } catch (e) {
    message.error((e as ApiError)?.message || t('error.UNKNOWN'))
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="profile-page">
    <!-- Banner -->
    <div class="profile-banner">
      <div class="profile-avatar">{{ avatarText }}</div>
      <div class="profile-identity">
        <div class="profile-name">{{ auth.user?.nickname || auth.user?.username }}</div>
        <a-tag :color="roleColor" class="profile-role-tag">{{ auth.user?.role }}</a-tag>
      </div>
    </div>

    <div class="profile-body">
      <!-- 基本资料 -->
      <a-card class="profile-card">
        <template #title>
          <UserOutlined style="margin-right: 8px" />{{ t('page.me.profile.title') }}
        </template>
        <template #extra>
          <a-space>
            <a-button size="small" @click="openEdit">
              <template #icon><EditOutlined /></template>
              {{ t('common.editProfile') }}
            </a-button>
            <a-button size="small" type="primary" @click="openPwd">
              <template #icon><LockOutlined /></template>
              {{ t('common.changePassword') }}
            </a-button>
          </a-space>
        </template>
        <a-row :gutter="[24, 16]">
          <a-col :xs="24" :sm="12" :md="8">
            <div class="info-item">
              <div class="info-label">{{ t('page.me.col.username') }}</div>
              <div class="info-value">{{ auth.user?.username || '-' }}</div>
            </div>
          </a-col>
          <a-col :xs="24" :sm="12" :md="8">
            <div class="info-item">
              <div class="info-label">{{ t('page.me.col.nickname') }}</div>
              <div class="info-value">{{ auth.user?.nickname || '-' }}</div>
            </div>
          </a-col>
          <a-col :xs="24" :sm="12" :md="8">
            <div class="info-item">
              <div class="info-label">{{ t('page.me.col.email') }}</div>
              <div class="info-value">{{ auth.user?.email || '-' }}</div>
            </div>
          </a-col>
          <a-col :xs="24" :sm="12" :md="8">
            <div class="info-item">
              <div class="info-label">{{ t('page.me.col.phone') }}</div>
              <div class="info-value">{{ auth.user?.phone || '-' }}</div>
            </div>
          </a-col>
          <a-col :xs="24" :sm="12" :md="8">
            <div class="info-item">
              <div class="info-label">{{ t('page.me.col.lastLogin') }}</div>
              <div class="info-value">{{ fmtDateTime(auth.user?.last_login_at) || '-' }}</div>
            </div>
          </a-col>
        </a-row>
      </a-card>

      <!-- 偏好设置 -->
      <a-card class="profile-card" style="margin-top: 16px">
        <template #title>
          <SettingOutlined style="margin-right: 8px" />{{ t('page.me.pref.title') }}
        </template>
        <a-row :gutter="[32, 20]" align="middle">
          <!-- 语言 -->
          <a-col :xs="24" :sm="8" :md="6">
            <div class="pref-item">
              <div class="pref-label">{{ t('common.locale') }}</div>
              <a-select
                :value="app.locale"
                style="width: 140px"
                @update:value="(v: AppLocale) => app.setLocale(v)"
              >
                <a-select-option value="zh-CN">中文</a-select-option>
                <a-select-option value="en-US">English</a-select-option>
              </a-select>
            </div>
          </a-col>
          <!-- 亮/暗主题 -->
          <a-col :xs="24" :sm="12" :md="10">
            <div class="pref-item">
              <div class="pref-label">{{ t('common.theme') }}</div>
              <a-radio-group
                :value="app.themeMode"
                button-style="solid"
                @update:value="(v: ThemeMode) => app.setThemeMode(v)"
              >
                <a-radio-button value="light">{{ t('common.themeLight') }}</a-radio-button>
                <a-radio-button value="dark">{{ t('common.themeDark') }}</a-radio-button>
                <a-radio-button value="system">{{ t('common.themeSystem') }}</a-radio-button>
              </a-radio-group>
            </div>
          </a-col>
        </a-row>
      </a-card>
    </div>

    <!-- 编辑资料 Modal -->
    <a-modal
      v-model:open="editDlg.open"
      :title="t('common.editProfile')"
      :ok-text="t('common.save')"
      :cancel-text="t('common.cancel')"
      @ok="onEditOk"
    >
      <a-form layout="vertical" style="margin-top: 8px">
        <a-form-item :label="t('page.me.col.nickname')">
          <a-input v-model:value="editDlg.nickname" />
        </a-form-item>
        <a-form-item :label="t('page.me.col.email')">
          <a-input v-model:value="editDlg.email" />
        </a-form-item>
        <a-form-item :label="t('page.me.col.phone')">
          <a-input v-model:value="editDlg.phone" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 修改密码 Modal -->
    <a-modal
      v-model:open="pwdDlg"
      :title="t('page.me.pwd.title')"
      :ok-text="t('common.submit')"
      :confirm-loading="submitting"
      :cancel-text="t('common.cancel')"
      @ok="onPwdSubmit"
    >
      <a-form layout="vertical" style="margin-top: 8px">
        <a-form-item :label="t('page.me.pwd.old')" required>
          <a-input-password v-model:value="pwdForm.old_password" autocomplete="current-password" />
        </a-form-item>
        <a-form-item :label="t('page.me.pwd.new')" required>
          <a-input-password v-model:value="pwdForm.new_password" autocomplete="new-password" />
        </a-form-item>
        <a-form-item :label="t('page.me.pwd.confirm')" required>
          <a-input-password v-model:value="pwdForm.confirm" autocomplete="new-password" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<style scoped>
.profile-page {
  min-height: 100%;
}

.profile-banner {
  display: flex;
  align-items: center;
  gap: 24px;
  padding: 32px 32px 48px;
  background: linear-gradient(135deg, #f97316 0%, #fb923c 45%, #38b2e0 75%, #0ea5e9 100%);
  border-radius: 12px;
  margin-bottom: -32px;
}

.profile-avatar {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.25);
  border: 3px solid rgba(255, 255, 255, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  font-weight: 700;
  color: #fff;
  flex-shrink: 0;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
}

.profile-identity {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.profile-name {
  font-size: 22px;
  font-weight: 700;
  color: #fff;
  line-height: 1;
}

.profile-role-tag {
  font-size: 12px;
  border: 1px solid rgba(255, 255, 255, 0.5);
  background: rgba(255, 255, 255, 0.2);
  color: #fff;
  width: fit-content;
}

.profile-body {
  padding: 0 0 24px;
  position: relative;
  z-index: 1;
}

.profile-card {
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.info-item {
  padding: 4px 0;
}

.info-label {
  font-size: 12px;
  color: var(--ant-color-text-secondary, #888);
  margin-bottom: 4px;
}

.info-value {
  font-size: 14px;
  font-weight: 500;
  color: var(--ant-color-text, #333);
}

.pref-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.pref-label {
  font-size: 12px;
  color: var(--ant-color-text-secondary, #888);
}
</style>
