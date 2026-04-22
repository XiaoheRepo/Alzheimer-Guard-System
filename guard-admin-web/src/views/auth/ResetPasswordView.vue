<!-- src/views/auth/ResetPasswordView.vue -->
<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import { passwordResetConfirm } from '@/api/auth'
import type { ApiError } from '@/utils/request'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()

const form = reactive({ token: '', new_password: '', confirm: '' })
const loading = ref(false)

onMounted(() => {
  const tk = route.query.token as string | undefined
  if (tk) form.token = tk
})

async function onSubmit() {
  if (!form.token || !form.new_password) {
    message.warning(t('page.auth.reset.fillAll'))
    return
  }
  if (form.new_password !== form.confirm) {
    message.warning(t('page.auth.reset.mismatch'))
    return
  }
  loading.value = true
  try {
    await passwordResetConfirm({
      token: form.token,
      new_password: form.new_password,
    })
    message.success(t('page.auth.reset.success'))
    await router.push('/login')
  } catch (e) {
    const err = e as ApiError
    message.error(err?.message || t('error.UNKNOWN'))
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="reset-page">
    <a-card class="reset-card" :title="t('page.auth.reset.title')">
      <a-form layout="vertical" @submit.prevent="onSubmit">
        <a-form-item :label="t('page.auth.reset.token')" required>
          <a-input v-model:value="form.token" size="large" />
        </a-form-item>
        <a-form-item :label="t('page.auth.reset.newPwd')" required>
          <a-input-password
            v-model:value="form.new_password"
            size="large"
            autocomplete="new-password"
          />
        </a-form-item>
        <a-form-item :label="t('page.auth.reset.confirmPwd')" required>
          <a-input-password v-model:value="form.confirm" size="large" autocomplete="new-password" />
        </a-form-item>
        <a-button type="primary" size="large" block :loading="loading" html-type="submit">
          {{ t('common.submit') }}
        </a-button>
        <div style="text-align: center; margin-top: 8px">
          <a-button type="link" @click="$router.push('/login')">
            {{ t('common.backToLogin') }}
          </a-button>
        </div>
      </a-form>
    </a-card>
  </div>
</template>

<style scoped>
.reset-page {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}
.reset-card {
  width: 480px;
}
</style>
