<!-- src/views/admin/DictView.vue / P-16 -->
<script setup lang="ts">
import ConfigView from './ConfigView.vue'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()
</script>

<template>
  <ConfigView group="dict" :title="t('menu.adminDict')" />
</template>
