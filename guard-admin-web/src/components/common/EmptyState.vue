<!-- src/components/common/EmptyState.vue -->
<script setup lang="ts">
defineProps<{ title?: string; description?: string }>()
</script>
<template>
  <a-empty :description="description">
    <template v-if="title" #description>
      <div>{{ title }}</div>
      <div v-if="description" class="text-muted">{{ description }}</div>
    </template>
    <slot />
  </a-empty>
</template>
