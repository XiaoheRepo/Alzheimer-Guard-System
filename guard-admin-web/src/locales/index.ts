// src/locales/index.ts
import { createI18n } from 'vue-i18n'
import zhCN from './zh-CN'
import enUS from './en-US'

export type AppLocale = 'zh-CN' | 'en-US'

export const i18n = createI18n({
  legacy: false,
  globalInjection: true,
  locale: 'zh-CN',
  fallbackLocale: 'zh-CN',
  messages: { 'zh-CN': zhCN, 'en-US': enUS },
  missingWarn: import.meta.env.DEV,
  silentFallbackWarn: true,
})

export async function setLocale(locale: AppLocale): Promise<void> {
  i18n.global.locale.value = locale
  const dayjs = (await import('dayjs')).default
  if (locale === 'zh-CN') {
    await import('dayjs/locale/zh-cn')
    dayjs.locale('zh-cn')
  } else {
    await import('dayjs/locale/en')
    dayjs.locale('en')
  }
  document.documentElement.lang = locale
}

export function t(key: string, ...args: unknown[]): string {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  return (i18n.global.t as any)(key, ...args)
}
