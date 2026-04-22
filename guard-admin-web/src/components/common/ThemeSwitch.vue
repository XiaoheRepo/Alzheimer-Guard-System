<!-- src/components/common/ThemeSwitch.vue -->
<script setup lang="ts">
import { BulbOutlined } from '@ant-design/icons-vue'
import { useAppStore, type ThemeMode } from '@/stores/app'
import { useI18n } from 'vue-i18n'

const app = useAppStore()
const { t } = useI18n()

function onChange(mode: ThemeMode) {
  app.setThemeMode(mode)
}
</script>

<template>
  <a-dropdown>
    <a-button type="text" :aria-label="t('common.switchTheme')">
      <template #icon><BulbOutlined /></template>
      {{
        app.themeMode === 'light'
          ? t('common.themeLight')
          : app.themeMode === 'dark'
            ? t('common.themeDark')
            : t('common.themeSystem')
      }}
    </a-button>
    <template #overlay>
      <a-menu @click="({ key }) => onChange(key as ThemeMode)">
        <a-menu-item key="light">{{ t('common.themeLight') }}</a-menu-item>
        <a-menu-item key="dark">{{ t('common.themeDark') }}</a-menu-item>
        <a-menu-item key="system">{{ t('common.themeSystem') }}</a-menu-item>
      </a-menu>
    </template>
  </a-dropdown>
</template>
