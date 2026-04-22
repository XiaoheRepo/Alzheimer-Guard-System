<script setup lang="ts">
import { computed, watch } from 'vue'
import { ConfigProvider } from 'ant-design-vue'
import zhCN from 'ant-design-vue/es/locale/zh_CN'
import enUS from 'ant-design-vue/es/locale/en_US'
import { useAppStore } from '@/stores/app'
import { lightTheme, darkTheme } from '@/styles/antdv'
import { setLocale } from '@/locales'

const app = useAppStore()

const antdvLocale = computed(() => (app.locale === 'zh-CN' ? zhCN : enUS))
const antdvTheme = computed(() =>
  app.effectiveTheme === 'dark' ? darkTheme : lightTheme,
)

watch(
  () => app.locale,
  (l) => {
    void setLocale(l)
  },
  { immediate: true },
)

watch(
  () => app.effectiveTheme,
  (t) => {
    document.documentElement.dataset.theme = t
  },
  { immediate: true },
)
</script>

<template>
  <ConfigProvider :locale="antdvLocale" :theme="antdvTheme">
    <RouterView />
  </ConfigProvider>
</template>

<style>
#app {
  height: 100vh;
}
</style>
