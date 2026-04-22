<!-- src/views/error/403.vue -->
<script setup lang="ts"></script>
<template>
  <a-result status="403" :title="$t('error.forbidden')" :sub-title="$t('page.error.403.sub')">
    <template #extra>
      <a-button type="primary" @click="$router.push('/')">
        {{ $t('common.backHome') }}
      </a-button>
    </template>
  </a-result>
</template>
