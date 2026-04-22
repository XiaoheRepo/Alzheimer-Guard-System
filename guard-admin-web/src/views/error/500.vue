<!-- src/views/error/500.vue -->
<script setup lang="ts"></script>
<template>
  <a-result status="500" :title="$t('page.error.500.title')" :sub-title="$t('page.error.500.sub')">
    <template #extra>
      <a-button type="primary" @click="$router.back()">
        {{ $t('common.back') }}
      </a-button>
    </template>
  </a-result>
</template>
