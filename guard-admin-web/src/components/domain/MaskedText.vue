<!-- src/components/domain/MaskedText.vue -->
<script setup lang="ts">
defineProps<{ text?: string | null }>()
</script>
<template>
  <span class="text-mono">{{ text || '-' }}</span>
</template>
