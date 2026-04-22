<!-- src/views/me/ProfileView.vue / P-12 -->
<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import { useAuthStore } from '@/stores/auth'
import { useAppStore, type ThemeMode } from '@/stores/app'
import { changePassword } from '@/api/me'
import PageHeader from '@/components/common/PageHeader.vue'
import type { ApiError } from '@/utils/request'
import type { AppLocale } from '@/locales'

const { t } = useI18n()
const auth = useAuthStore()
const app = useAppStore()

const pwdForm = reactive({ old_password: '', new_password: '', confirm: '' })
const submitting = ref(false)

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
    pwdForm.old_password = ''
    pwdForm.new_password = ''
    pwdForm.confirm = ''
  } catch (e) {
    message.error((e as ApiError)?.message || t('error.UNKNOWN'))
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="page-container">
    <PageHeader :title="t('menu.me')" />

    <a-card :title="t('page.me.profile.title')">
      <a-descriptions bordered :column="2" size="small">
        <a-descriptions-item :label="t('page.me.col.username')">{{
          auth.user?.username
        }}</a-descriptions-item>
        <a-descriptions-item :label="t('page.me.col.nickname')">{{
          auth.user?.nickname
        }}</a-descriptions-item>
        <a-descriptions-item :label="t('page.me.col.role')">{{
          auth.user?.role
        }}</a-descriptions-item>
        <a-descriptions-item :label="t('page.me.col.email')">{{
          auth.user?.email || '-'
        }}</a-descriptions-item>
        <a-descriptions-item :label="t('page.me.col.phone')">{{
          auth.user?.phone || '-'
        }}</a-descriptions-item>
        <a-descriptions-item :label="t('page.me.col.lastLogin')">{{
          auth.user?.last_login_at || '-'
        }}</a-descriptions-item>
      </a-descriptions>
    </a-card>

    <a-card :title="t('page.me.pref.title')" style="margin-top: 16px">
      <a-form :label-col="{ span: 4 }" :wrapper-col="{ span: 8 }">
        <a-form-item :label="t('common.locale')">
          <a-select :value="app.locale" @update:value="(v: AppLocale) => app.setLocale(v)">
            <a-select-option value="zh-CN">中文</a-select-option>
            <a-select-option value="en-US">English</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item :label="t('common.theme')">
          <a-radio-group
            :value="app.themeMode"
            @update:value="(v: ThemeMode) => app.setThemeMode(v)"
          >
            <a-radio-button value="light">{{ t('common.themeLight') }}</a-radio-button>
            <a-radio-button value="dark">{{ t('common.themeDark') }}</a-radio-button>
            <a-radio-button value="system">{{ t('common.themeSystem') }}</a-radio-button>
          </a-radio-group>
        </a-form-item>
      </a-form>
    </a-card>

    <a-card :title="t('page.me.pwd.title')" style="margin-top: 16px">
      <a-form :label-col="{ span: 4 }" :wrapper-col="{ span: 8 }" @submit.prevent="onPwdSubmit">
        <a-form-item :label="t('page.me.pwd.old')" required>
          <a-input-password v-model:value="pwdForm.old_password" autocomplete="current-password" />
        </a-form-item>
        <a-form-item :label="t('page.me.pwd.new')" required>
          <a-input-password v-model:value="pwdForm.new_password" autocomplete="new-password" />
        </a-form-item>
        <a-form-item :label="t('page.me.pwd.confirm')" required>
          <a-input-password v-model:value="pwdForm.confirm" autocomplete="new-password" />
        </a-form-item>
        <a-form-item :wrapper-col="{ offset: 4 }">
          <a-button type="primary" :loading="submitting" html-type="submit">
            {{ t('common.submit') }}
          </a-button>
        </a-form-item>
      </a-form>
    </a-card>
  </div>
</template>
