// src/router/routes.ts
import type { RouteRecordRaw } from 'vue-router'

export const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    component: () => import('@/layouts/BlankLayout.vue'),
    children: [
      {
        path: '',
        name: 'Login',
        component: () => import('@/views/auth/LoginView.vue'),
        meta: { title: 'page.auth.login.title', public: true },
      },
    ],
  },
  {
    path: '/reset-password',
    component: () => import('@/layouts/BlankLayout.vue'),
    children: [
      {
        path: '',
        name: 'ResetPassword',
        component: () => import('@/views/auth/ResetPasswordView.vue'),
        meta: { title: 'page.auth.reset.title', public: true },
      },
    ],
  },
  {
    path: '/',
    component: () => import('@/layouts/AppLayout.vue'),
    redirect: '/dashboard',
    meta: { roles: ['ADMIN', 'SUPER_ADMIN'] },
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/DashboardView.vue'),
        meta: {
          title: 'menu.dashboard',
          icon: 'DashboardOutlined',
          roles: ['ADMIN', 'SUPER_ADMIN'],
          keepAlive: true,
        },
      },
      {
        path: 'tasks',
        name: 'TaskList',
        component: () => import('@/views/task/TaskListView.vue'),
        meta: {
          title: 'menu.task',
          icon: 'AlertOutlined',
          roles: ['ADMIN', 'SUPER_ADMIN'],
          keepAlive: true,
        },
      },
      {
        path: 'tasks/:taskId',
        name: 'TaskDetail',
        component: () => import('@/views/task/TaskDetailView.vue'),
        meta: { title: 'page.task.detail.title', roles: ['ADMIN', 'SUPER_ADMIN'], hidden: true },
      },
      {
        path: 'clues/review',
        name: 'ClueReview',
        component: () => import('@/views/clue/ClueReviewView.vue'),
        meta: {
          title: 'menu.clueReview',
          icon: 'SafetyCertificateOutlined',
          roles: ['ADMIN', 'SUPER_ADMIN'],
          keepAlive: true,
        },
      },
      {
        path: 'clues',
        name: 'ClueList',
        component: () => import('@/views/clue/ClueListView.vue'),
        meta: {
          title: 'menu.clueAll',
          icon: 'OrderedListOutlined',
          roles: ['ADMIN', 'SUPER_ADMIN'],
          keepAlive: true,
        },
      },
      {
        path: 'patients',
        name: 'AdminPatientList',
        component: () => import('@/views/patient/PatientListView.vue'),
        meta: {
          title: 'menu.patient',
          icon: 'TeamOutlined',
          roles: ['ADMIN', 'SUPER_ADMIN'],
          keepAlive: true,
        },
      },
      {
        path: 'patients/:patientId',
        name: 'AdminPatientDetail',
        component: () => import('@/views/patient/PatientDetailView.vue'),
        meta: { title: 'page.patient.detail.title', roles: ['ADMIN', 'SUPER_ADMIN'], hidden: true },
      },
      {
        path: 'material/orders',
        name: 'OrderList',
        component: () => import('@/views/material/OrderListView.vue'),
        meta: {
          title: 'menu.materialOrder',
          icon: 'GiftOutlined',
          roles: ['ADMIN', 'SUPER_ADMIN'],
          keepAlive: true,
        },
      },
      {
        path: 'material/orders/:orderId',
        name: 'OrderDetail',
        component: () => import('@/views/material/OrderDetailView.vue'),
        meta: { title: 'page.material.order.title', roles: ['ADMIN', 'SUPER_ADMIN'], hidden: true },
      },
      {
        path: 'tags/inventory',
        name: 'TagInventory',
        component: () => import('@/views/material/TagInventoryView.vue'),
        meta: {
          title: 'menu.tagInventory',
          icon: 'TagsOutlined',
          roles: ['ADMIN', 'SUPER_ADMIN'],
          keepAlive: true,
        },
      },
      {
        path: 'tags/batch-jobs/:jobId',
        name: 'TagBatchJob',
        component: () => import('@/views/material/TagBatchJobView.vue'),
        meta: { title: 'page.tag.job.title', roles: ['ADMIN', 'SUPER_ADMIN'], hidden: true },
      },
      {
        path: 'notifications',
        name: 'Notifications',
        component: () => import('@/views/notification/InboxView.vue'),
        meta: {
          title: 'menu.notification',
          icon: 'BellOutlined',
          roles: ['ADMIN', 'SUPER_ADMIN'],
          keepAlive: true,
        },
      },
      {
        path: 'admin/configs',
        name: 'AdminConfig',
        component: () => import('@/views/admin/ConfigView.vue'),
        meta: {
          title: 'menu.adminConfig',
          icon: 'SettingOutlined',
          roles: ['ADMIN', 'SUPER_ADMIN'],
          keepAlive: true,
        },
      },
      {
        path: 'admin/dicts',
        name: 'AdminDict',
        component: () => import('@/views/admin/DictView.vue'),
        meta: {
          title: 'menu.adminDict',
          icon: 'BookOutlined',
          roles: ['ADMIN', 'SUPER_ADMIN'],
          keepAlive: true,
        },
      },
      {
        path: 'admin/logs',
        name: 'AdminLog',
        component: () => import('@/views/admin/AuditLogView.vue'),
        meta: {
          title: 'menu.adminLog',
          icon: 'FileSearchOutlined',
          roles: ['ADMIN', 'SUPER_ADMIN'],
          keepAlive: true,
        },
      },
      {
        path: 'admin/dead-letter',
        name: 'AdminDead',
        component: () => import('@/views/admin/DeadLetterView.vue'),
        meta: {
          title: 'menu.adminDead',
          icon: 'WarningOutlined',
          roles: ['SUPER_ADMIN'],
          keepAlive: true,
        },
      },
      {
        path: 'admin/users',
        name: 'AdminUser',
        component: () => import('@/views/admin/UserListView.vue'),
        meta: {
          title: 'menu.adminUser',
          icon: 'UserOutlined',
          roles: ['ADMIN', 'SUPER_ADMIN'],
          keepAlive: true,
        },
      },
      {
        path: 'me',
        name: 'Me',
        component: () => import('@/views/me/ProfileView.vue'),
        meta: { title: 'menu.me', roles: ['ADMIN', 'SUPER_ADMIN'], hidden: true },
      },
    ],
  },
  {
    path: '/401',
    component: () => import('@/views/error/401.vue'),
    meta: { public: true },
  },
  {
    path: '/403',
    component: () => import('@/views/error/403.vue'),
    meta: { public: true },
  },
  {
    path: '/500',
    component: () => import('@/views/error/500.vue'),
    meta: { public: true },
  },
  {
    path: '/:pathMatch(.*)*',
    component: () => import('@/views/error/404.vue'),
    meta: { public: true },
  },
]
