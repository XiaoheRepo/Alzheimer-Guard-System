<!-- src/components/common/LocaleSwitch.vue -->
<script setup lang="ts">
import { TranslationOutlined } from '@ant-design/icons-vue'
import { useAppStore } from '@/stores/app'
import type { AppLocale } from '@/locales'

const app = useAppStore()

function onChange({ key }: { key: string }) {
  app.setLocale(key as AppLocale)
}
</script>

<template>
  <a-dropdown>
    <a-button type="text" :aria-label="$t('common.switchLocale')">
      <template #icon><TranslationOutlined /></template>
      {{ app.locale === 'zh-CN' ? '中文' : 'EN' }}
    </a-button>
    <template #overlay>
      <a-menu @click="onChange">
        <a-menu-item key="zh-CN">中文</a-menu-item>
        <a-menu-item key="en-US">English</a-menu-item>
      </a-menu>
    </template>
  </a-dropdown>
</template>
