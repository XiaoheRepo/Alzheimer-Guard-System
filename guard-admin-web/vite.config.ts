import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue(), vueDevTools()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  build: {
    sourcemap: 'hidden',
    rollupOptions: {
      output: {
        manualChunks: {
          'vendor-vue': ['vue', 'vue-router', 'pinia'],
          'vendor-antdv': ['ant-design-vue'],
          'vendor-echarts': ['echarts', 'vue-echarts'],
          'vendor-i18n': ['vue-i18n', 'dayjs'],
        },
      },
    },
  },
  server: {
    port: 5173,
    host: '0.0.0.0',
  },
})
