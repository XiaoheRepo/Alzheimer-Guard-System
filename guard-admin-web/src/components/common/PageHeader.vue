<!-- src/components/common/PageHeader.vue -->
<script setup lang="ts">
defineProps<{ title?: string; subTitle?: string }>()
</script>
<template>
  <div class="page-header">
    <div class="title">
      <slot name="title">{{ title }}</slot>
    </div>
    <div v-if="subTitle || $slots.subTitle" class="sub-title text-muted">
      <slot name="subTitle">{{ subTitle }}</slot>
    </div>
    <div v-if="$slots.extra" class="extra">
      <slot name="extra" />
    </div>
  </div>
</template>
<style scoped>
.page-header {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 0 0 16px;
  flex-wrap: wrap;
}
.title {
  font-size: 20px;
  font-weight: 600;
}
.extra {
  margin-left: auto;
  display: flex;
  gap: 8px;
  align-items: center;
}
</style>
