<!-- src/layouts/AppLayout.vue -->
<script setup lang="ts">
import { computed, h, ref, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import {
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  LogoutOutlined,
  UserOutlined,
  DashboardOutlined,
  AlertOutlined,
  SafetyCertificateOutlined,
  OrderedListOutlined,
  TeamOutlined,
  GiftOutlined,
  TagsOutlined,
  BellOutlined,
  SettingOutlined,
  BookOutlined,
  FileSearchOutlined,
  WarningOutlined,
} from '@ant-design/icons-vue'
import type { Component } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useAppStore } from '@/stores/app'
import { useI18n } from 'vue-i18n'
import { routes as appRoutes } from '@/router/routes'
import type { Role } from '@/types/common'
import LocaleSwitch from '@/components/common/LocaleSwitch.vue'
import ThemeSwitch from '@/components/common/ThemeSwitch.vue'
import NotificationBell from '@/components/common/NotificationBell.vue'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const app = useAppStore()
const { t } = useI18n()

const iconMap: Record<string, Component> = {
  DashboardOutlined,
  AlertOutlined,
  SafetyCertificateOutlined,
  OrderedListOutlined,
  TeamOutlined,
  GiftOutlined,
  TagsOutlined,
  BellOutlined,
  SettingOutlined,
  BookOutlined,
  FileSearchOutlined,
  WarningOutlined,
  UserOutlined,
}

interface MenuEntry {
  key: string
  path: string
  title: string
  icon?: string
}

function flattenMenu(): MenuEntry[] {
  const root = appRoutes.find((r) => r.path === '/')
  if (!root || !root.children) return []
  const role = auth.user?.role
  return root.children
    .filter((c: RouteRecordRaw) => {
      if (c.meta?.hidden) return false
      const roles = c.meta?.roles as Role[] | undefined
      if (!roles || !role) return !!role
      if (role === 'SUPER_ADMIN') return true
      return roles.includes(role)
    })
    .map((c: RouteRecordRaw) => ({
      key: `/${c.path}`,
      path: `/${c.path}`,
      title: (c.meta?.title as string) || c.path,
      icon: c.meta?.icon as string | undefined,
    }))
}

const menu = computed<MenuEntry[]>(() => flattenMenu())

const selectedKeys = ref<string[]>([route.path])
watch(
  () => route.path,
  (p) => {
    const hit = menu.value.find((m) => p === m.path || p.startsWith(m.path + '/'))
    selectedKeys.value = hit ? [hit.path] : [p]
  },
  { immediate: true },
)

function onMenuClick(info: { key: string }) {
  if (info.key !== route.path) router.push(info.key)
}

async function onLogout() {
  await auth.logout()
  await router.push('/login')
}

function goMe() {
  router.push('/me')
}

const breadcrumb = computed(() => {
  const parts: { title: string }[] = [{ title: t('menu.home') }]
  const matched = route.matched.filter((m) => m.meta?.title)
  for (const m of matched) {
    parts.push({ title: t(m.meta!.title as string) })
  }
  return parts
})
</script>

<template>
  <a-layout class="app-layout">
    <a-layout-sider
      v-model:collapsed="app.sidebarCollapsed"
      collapsible
      :trigger="null"
      :width="220"
      :collapsed-width="64"
      theme="dark"
    >
      <div class="logo">
        <span v-if="!app.sidebarCollapsed">{{ t('app.name') }}</span>
        <span v-else>CH</span>
      </div>
      <a-menu theme="dark" mode="inline" :selected-keys="selectedKeys" @click="onMenuClick">
        <a-menu-item v-for="m in menu" :key="m.path">
          <template v-if="m.icon" #icon>
            <component :is="iconMap[m.icon]" />
          </template>
          <span>{{ t(m.title) }}</span>
        </a-menu-item>
      </a-menu>
    </a-layout-sider>

    <a-layout>
      <a-layout-header class="app-header">
        <a-button
          type="text"
          class="collapse-btn"
          @click="app.toggleSidebar"
          :aria-label="$t('common.toggleSidebar')"
        >
          <MenuFoldOutlined v-if="!app.sidebarCollapsed" />
          <MenuUnfoldOutlined v-else />
        </a-button>

        <a-breadcrumb :items="breadcrumb" class="breadcrumb" />

        <div class="header-right">
          <NotificationBell />
          <LocaleSwitch />
          <ThemeSwitch />
          <a-dropdown>
            <a-button type="text">
              <template #icon><UserOutlined /></template>
              {{ auth.user?.nickname || auth.user?.username || '-' }}
            </a-button>
            <template #overlay>
              <a-menu>
                <a-menu-item key="me" @click="goMe">
                  <UserOutlined />
                  <span>{{ t('menu.me') }}</span>
                </a-menu-item>
                <a-menu-divider />
                <a-menu-item key="logout" @click="onLogout">
                  <LogoutOutlined />
                  <span>{{ t('common.logout') }}</span>
                </a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </div>
      </a-layout-header>

      <a-layout-content class="app-content">
        <router-view v-slot="{ Component }">
          <keep-alive>
            <component :is="Component" v-if="$route.meta.keepAlive" />
          </keep-alive>
          <component :is="Component" v-if="!$route.meta.keepAlive" />
        </router-view>
      </a-layout-content>
    </a-layout>
  </a-layout>
</template>

<style scoped>
.app-layout {
  min-height: 100vh;
}
.logo {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-weight: 700;
  letter-spacing: 2px;
  background: rgba(0, 0, 0, 0.2);
}
.app-header {
  background: var(--bg-elevated);
  display: flex;
  align-items: center;
  padding: 0 16px;
  gap: 16px;
  border-bottom: 1px solid var(--border-color);
}
.collapse-btn {
  font-size: 18px;
}
.breadcrumb {
  flex: 1;
}
.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}
.app-content {
  padding: 16px;
  background: var(--bg-layout);
  min-height: calc(100vh - 56px);
}
</style>
