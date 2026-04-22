<!-- src/views/error/404.vue -->
<script setup lang="ts"></script>
<template>
  <a-result status="404" :title="$t('page.error.404.title')" :sub-title="$t('page.error.404.sub')">
    <template #extra>
      <a-button type="primary" @click="$router.push('/')">
        {{ $t('common.backHome') }}
      </a-button>
    </template>
  </a-result>
</template>
