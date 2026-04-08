<script setup lang="ts">
import { theme } from 'ant-design-vue'

const { defaultAlgorithm } = theme

const themeConfig = {
  algorithm: defaultAlgorithm,
  token: {
    colorPrimary: '#1677ff',
    colorLink: '#1677ff',
    colorSuccess: '#52c41a',
    colorWarning: '#faad14',
    colorError: '#ff4d4f',
    borderRadius: 6,
  },
}
</script>

<template>
  <a-config-provider :theme="themeConfig">
    <router-view />
  </a-config-provider>
</template>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

#app {
  height: 100vh;
}
</style>
