<script setup lang="ts">
import { onMounted } from 'vue'

onMounted(() => {
  console.log('Dashboard 页面加载')
})
</script>

<template>
  <div class="dashboard">
    <a-card title="欢迎使用阿尔茨海默病监护系统管理后台">
      <p>这是工作台页面，后续将添加统计卡片、图表等功能。</p>
    </a-card>
  </div>
</template>

<style scoped>
.dashboard {
  width: 100%;
}
</style>
