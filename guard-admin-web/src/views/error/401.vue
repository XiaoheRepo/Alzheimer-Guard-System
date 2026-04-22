<!-- src/views/error/401.vue -->
<script setup lang="ts"></script>
<template>
  <a-result status="401" :title="$t('error.E_AUTH_4011')" :sub-title="$t('page.error.401.sub')">
    <template #extra>
      <a-button type="primary" @click="$router.push('/login')">
        {{ $t('common.backToLogin') }}
      </a-button>
    </template>
  </a-result>
</template>
