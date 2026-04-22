// src/stores/app.ts
import { defineStore } from 'pinia'
import { usePreferredColorScheme } from '@vueuse/core'
import type { AppLocale } from '@/locales'

export type ThemeMode = 'light' | 'dark' | 'system'

export const useAppStore = defineStore('app', {
  state: () => ({
    locale: 'zh-CN' as AppLocale,
    themeMode: 'system' as ThemeMode,
    sidebarCollapsed: false,
  }),
  getters: {
    effectiveTheme(state): 'light' | 'dark' {
      if (state.themeMode !== 'system') return state.themeMode
      return usePreferredColorScheme().value === 'dark' ? 'dark' : 'light'
    },
  },
  actions: {
    setLocale(locale: AppLocale) {
      this.locale = locale
    },
    setThemeMode(mode: ThemeMode) {
      this.themeMode = mode
    },
    toggleSidebar() {
      this.sidebarCollapsed = !this.sidebarCollapsed
    },
  },
  persist: {
    key: 'app',
    pick: ['locale', 'themeMode', 'sidebarCollapsed'],
  },
})
