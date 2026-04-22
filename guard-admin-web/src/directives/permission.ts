// src/directives/permission.ts
// v-permission：按角色白名单隐藏元素（HC-Auth 角色仅 ADMIN / SUPER_ADMIN）
import type { Directive } from 'vue'
import { useAuthStore } from '@/stores/auth'
import type { Role } from '@/types/common'

type PermValue = Role | Role[] | { role: Role | Role[] }

function check(value: PermValue | undefined): boolean {
  const auth = useAuthStore()
  if (!auth.user) return false
  const role = auth.user.role
  if (!value) return true
  const allowed: Role[] = Array.isArray(value)
    ? value
    : typeof value === 'string'
      ? [value]
      : Array.isArray(value.role)
        ? value.role
        : [value.role]
  // SUPER_ADMIN 隐式通配
  if (role === 'SUPER_ADMIN') return true
  return allowed.includes(role)
}

export const vPermission: Directive<HTMLElement, PermValue> = {
  mounted(el, binding) {
    if (!check(binding.value)) {
      el.parentNode?.removeChild(el)
    }
  },
  updated(el, binding) {
    if (!check(binding.value)) {
      el.style.display = 'none'
    } else {
      el.style.display = ''
    }
  },
}
