<!-- src/views/auth/LoginView.vue / P-01 登录 -->
<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import { UserOutlined, LockOutlined } from '@ant-design/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { passwordResetRequest } from '@/api/auth'
import type { ApiError } from '@/utils/request'
import LocaleSwitch from '@/components/common/LocaleSwitch.vue'

const { t } = useI18n()
const auth = useAuthStore()
const router = useRouter()
const route = useRoute()

const form = reactive({ username: '', password: '' })
const loading = ref(false)
const showReset = ref(false)
const resetEmail = ref('')
const resetLoading = ref(false)

async function onSubmit() {
  if (!form.username || !form.password) {
    message.warning(t('page.auth.login.fillAll'))
    return
  }
  loading.value = true
  try {
    await auth.login({ username: form.username, password: form.password })
    if (!auth.isBackofficeAllowed()) {
      message.error(t('error.E_AUTH_4031'))
      await auth.logout()
      return
    }
    const redirect = (route.query.redirect as string) || '/dashboard'
    await router.push(redirect)
  } catch (e) {
    const err = e as ApiError
    message.error(err?.message || t('error.UNKNOWN'))
  } finally {
    loading.value = false
  }
}

async function onResetSubmit() {
  if (!resetEmail.value) {
    message.warning(t('page.auth.reset.fillEmail'))
    return
  }
  resetLoading.value = true
  try {
    await passwordResetRequest({ email: resetEmail.value })
    message.success(t('page.auth.reset.sent'))
    showReset.value = false
  } catch (e) {
    const err = e as ApiError
    message.error(err?.message || t('error.UNKNOWN'))
  } finally {
    resetLoading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <div class="login-topbar">
      <LocaleSwitch />
    </div>
    <a-card class="login-card">
      <div class="brand">
        <div class="logo">CH</div>
        <div class="title">{{ t('app.name') }}</div>
        <div class="sub">{{ t('page.auth.login.subtitle') }}</div>
      </div>
      <a-form layout="vertical" @submit.prevent="onSubmit">
        <a-form-item :label="t('page.auth.login.username')" required>
          <a-input
            v-model:value="form.username"
            size="large"
            :placeholder="t('page.auth.login.usernamePH')"
            autocomplete="username"
          >
            <template #prefix><UserOutlined /></template>
          </a-input>
        </a-form-item>
        <a-form-item :label="t('page.auth.login.password')" required>
          <a-input-password
            v-model:value="form.password"
            size="large"
            :placeholder="t('page.auth.login.passwordPH')"
            autocomplete="current-password"
            @keyup.enter="onSubmit"
          >
            <template #prefix><LockOutlined /></template>
          </a-input-password>
        </a-form-item>
        <a-button type="primary" size="large" block :loading="loading" html-type="submit">
          {{ t('page.auth.login.submit') }}
        </a-button>
        <div class="forgot">
          <a-button type="link" @click="showReset = true">
            {{ t('page.auth.login.forgot') }}
          </a-button>
        </div>
      </a-form>
    </a-card>

    <a-modal
      v-model:open="showReset"
      :title="t('page.auth.reset.title')"
      :confirm-loading="resetLoading"
      :ok-text="t('common.submit')"
      :cancel-text="t('common.cancel')"
      @ok="onResetSubmit"
    >
      <p class="text-muted">{{ t('page.auth.reset.hint') }}</p>
      <a-input
        v-model:value="resetEmail"
        type="email"
        :placeholder="t('page.auth.reset.emailPH')"
        size="large"
      />
    </a-modal>
  </div>
</template>

<style scoped>
.login-page {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
}
.login-topbar {
  position: absolute;
  top: 16px;
  right: 16px;
}
.login-card {
  width: 420px;
  padding: 16px 8px;
}
.brand {
  text-align: center;
  margin-bottom: 16px;
}
.logo {
  width: 56px;
  height: 56px;
  background: var(--color-primary);
  color: #fff;
  line-height: 56px;
  text-align: center;
  border-radius: 12px;
  margin: 0 auto 12px;
  font-weight: 700;
  font-size: 20px;
  letter-spacing: 2px;
}
.title {
  font-size: 22px;
  font-weight: 600;
}
.sub {
  color: var(--text-muted);
  margin-top: 4px;
}
.forgot {
  text-align: right;
  margin-top: 8px;
}
</style>
