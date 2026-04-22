<!-- src/layouts/BlankLayout.vue -->
<script setup lang="ts"></script>

<template>
  <div class="blank-layout">
    <RouterView />
  </div>
</template>

<style scoped>
.blank-layout {
  min-height: 100vh;
  width: 100%;
  display: flex;
  align-items: stretch;
  justify-content: center;
  background: linear-gradient(135deg, #f97316 0%, #0ea5e9 100%);
}
html[data-theme='dark'] .blank-layout {
  background: #141414;
}
</style>
