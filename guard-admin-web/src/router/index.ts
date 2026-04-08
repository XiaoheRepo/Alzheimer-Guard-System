/**
 * 路由配置
 * 依据 web_admin_handbook.md §5.1-5.3：路由树、meta 规范、路由守卫
 */

import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw, RouteMeta } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { WHITE_LIST } from '@/constants'

import BlankLayout from '@/layouts/BlankLayout.vue'
import AdminLayout from '@/layouts/AdminLayout.vue'

// 扩展 RouteMeta，与 handbook §5.2 对齐
declare module 'vue-router' {
  interface RouteMeta {
    title?: string
    requiresAuth?: boolean
    /** 允许访问的角色，空数组 = 无限制（仅需登录） */
    roles?: ('ADMIN' | 'SUPERADMIN')[]
    permissionCode?: string
    keepAlive?: boolean
    auditTag?: string
  }
}

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    component: BlankLayout,
    children: [
      {
        path: '',
        name: 'Login',
        component: () => import('@/views/login/index.vue'),
        meta: { title: '登录' },
      },
    ],
  },
  {
    path: '/403',
    component: BlankLayout,
    children: [
      {
        path: '',
        name: 'Forbidden',
        component: () => import('@/views/error/403.vue'),
        meta: { title: '无权限' },
      },
    ],
  },
  {
    path: '/admin',
    redirect: '/admin/dashboard',
    component: AdminLayout,
    children: [
      {
        // ADM-01 运营看板
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: {
          title: '运营看板',
          requiresAuth: true,
          roles: ['ADMIN', 'SUPERADMIN'],
          keepAlive: true,
          auditTag: 'ADM-01',
        },
      },
      {
        // ADM-02 任务治理
        path: 'tasks',
        name: 'Tasks',
        component: () => import('@/views/tasks/index.vue'),
        meta: {
          title: '任务治理',
          requiresAuth: true,
          roles: ['ADMIN', 'SUPERADMIN'],
          keepAlive: true,
          auditTag: 'ADM-02',
        },
      },
      {
        path: 'tasks/:taskId',
        name: 'TaskDetail',
        component: () => import('@/views/tasks/detail.vue'),
        meta: {
          title: '任务详情',
          requiresAuth: true,
          roles: ['ADMIN', 'SUPERADMIN'],
          auditTag: 'ADM-02',
        },
      },
      {
        // ADM-03 线索复核
        path: 'clues/review',
        name: 'ClueReview',
        component: () => import('@/views/clues/review.vue'),
        meta: {
          title: '线索复核',
          requiresAuth: true,
          roles: ['ADMIN', 'SUPERADMIN'],
          keepAlive: true,
          auditTag: 'ADM-03',
        },
      },
      {
        // ADM-04 标签与物资治理
        path: 'material',
        name: 'Material',
        component: () => import('@/views/material/index.vue'),
        meta: {
          title: '标签与物资治理',
          requiresAuth: true,
          roles: ['ADMIN', 'SUPERADMIN'],
          keepAlive: true,
          auditTag: 'ADM-04',
        },
      },
      {
        path: 'material/orders/:orderId',
        name: 'OrderDetail',
        component: () => import('@/views/material/order-detail.vue'),
        meta: {
          title: '工单详情',
          requiresAuth: true,
          roles: ['ADMIN', 'SUPERADMIN'],
          auditTag: 'ADM-04',
        },
      },
      {
        // ADM-05 用户治理
        path: 'users',
        name: 'Users',
        component: () => import('@/views/users/index.vue'),
        meta: {
          title: '用户治理',
          requiresAuth: true,
          roles: ['ADMIN', 'SUPERADMIN'],
          keepAlive: true,
          auditTag: 'ADM-05',
        },
      },
      {
        // ADM-06 审计与安全
        path: 'audit',
        name: 'Audit',
        component: () => import('@/views/audit/index.vue'),
        meta: {
          title: '审计与安全',
          requiresAuth: true,
          roles: ['ADMIN', 'SUPERADMIN'],
          keepAlive: false,
          auditTag: 'ADM-06',
        },
      },
      {
        // ADM-07 系统配置（ADMIN 只读，SUPERADMIN 可编辑）
        path: 'config',
        name: 'SysConfig',
        component: () => import('@/views/config/index.vue'),
        meta: {
          title: '系统配置',
          requiresAuth: true,
          roles: ['ADMIN', 'SUPERADMIN'],
          permissionCode: 'sys:config:view',
          auditTag: 'ADM-07',
        },
      },
      {
        // ADM-08 DEAD 干预与超管操作（仅 SUPERADMIN）
        path: 'dead-letter',
        name: 'DeadLetter',
        component: () => import('@/views/dead-letter/index.vue'),
        meta: {
          title: 'DEAD 干预与超管操作',
          requiresAuth: true,
          roles: ['SUPERADMIN'],
          permissionCode: 'sys:dead:manage',
          auditTag: 'ADM-08',
        },
      },
    ],
  },
  // 根路径兼容旧跳转
  {
    path: '/',
    redirect: '/admin/dashboard',
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/login',
  },
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
})

/**
 * 路由守卫
 * 依据 web_admin_handbook.md §5.3：
 *   1. 白名单直接放行
 *   2. 需要登录但无 token → 跳转 /login
 *   3. 已登录访问 /login → 跳转 /admin/dashboard
 *   4. 有角色限制且当前角色不符 → 跳转 /403
 */
router.beforeEach((to, _from, next) => {
  const authStore = useAuthStore()

  if (WHITE_LIST.includes(to.path)) {
    return next()
  }

  if (to.meta.requiresAuth && !authStore.isLoggedIn) {
    return next({ name: 'Login' })
  }

  if (to.path === '/login' && authStore.isLoggedIn) {
    return next({ path: '/admin/dashboard' })
  }

  const allowedRoles = to.meta.roles ?? []
  if (allowedRoles.length > 0 && authStore.role && !allowedRoles.includes(authStore.role as 'ADMIN' | 'SUPERADMIN')) {
    return next({ name: 'Forbidden' })
  }

  next()
})

export default router
