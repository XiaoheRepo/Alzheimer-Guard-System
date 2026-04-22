// src/router/guards.ts
import type { Router } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { i18n } from '@/locales'
import type { Role } from '@/types/common'

export function setupGuards(router: Router): void {
  router.beforeEach(async (to) => {
    const auth = useAuthStore()

    if (to.meta.public) return true

    if (!auth.accessToken) {
      return {
        path: '/login',
        query: to.fullPath !== '/' ? { redirect: to.fullPath } : undefined,
      }
    }

    if (!auth.user) {
      try {
        await auth.fetchCurrentUser()
      } catch {
        return { path: '/login', query: { redirect: to.fullPath } }
      }
    }

    // 非管理端角色一律拒绝
    if (!auth.isBackofficeAllowed()) {
      return { path: '/403' }
    }

    const allowed = to.meta.roles as Role[] | undefined
    if (allowed && auth.user && !allowed.includes(auth.user.role)) {
      return { path: '/403' }
    }
    return true
  })

  router.afterEach((to) => {
    const titleKey = (to.meta.title as string) || 'app.name'
    const te = i18n.global.te as unknown as (k: string) => boolean
    const title = te(titleKey) ? (i18n.global.t(titleKey) as string) : titleKey
    const appName = i18n.global.t('app.name') as string
    document.title = `${title} · ${appName}`
  })
}
