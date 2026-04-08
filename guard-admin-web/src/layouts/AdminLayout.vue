<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  DashboardOutlined,
  UnorderedListOutlined,
  AuditOutlined,
  TagsOutlined,
  TeamOutlined,
  SafetyCertificateOutlined,
  SettingOutlined,
  AlertOutlined,
  MenuUnfoldOutlined,
  MenuFoldOutlined,
  LogoutOutlined,
  UserOutlined,
} from '@ant-design/icons-vue'
import { useAuthStore } from '@/stores/auth'

const collapsed = ref(false)
const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const selectedKeys = computed(() => [route.name as string])

const menuItems = computed(() => {
  const all = [
    { key: 'Dashboard', label: '运营看板', icon: DashboardOutlined, path: '/admin/dashboard' },
    { key: 'Tasks', label: '任务治理', icon: UnorderedListOutlined, path: '/admin/tasks' },
    { key: 'ClueReview', label: '线索复核', icon: AuditOutlined, path: '/admin/clues/review' },
    { key: 'Material', label: '标签与物资', icon: TagsOutlined, path: '/admin/material' },
    { key: 'Users', label: '用户治理', icon: TeamOutlined, path: '/admin/users' },
    { key: 'Audit', label: '审计与安全', icon: SafetyCertificateOutlined, path: '/admin/audit' },
    { key: 'SysConfig', label: '系统配置', icon: SettingOutlined, path: '/admin/config' },
    {
      key: 'DeadLetter',
      label: 'DEAD 干预',
      icon: AlertOutlined,
      path: '/admin/dead-letter',
      superOnly: true,
    },
  ]
  return all.filter((item) => !item.superOnly || authStore.isSuperAdmin)
})

const handleMenuClick = ({ key }: { key: string }) => {
  const item = menuItems.value.find((m) => m.key === key)
  if (item) router.push(item.path)
}

const handleLogout = async () => {
  await authStore.logout()
  router.push('/login')
}
</script>

<template>
  <a-layout class="admin-layout">
    <a-layout-sider v-model:collapsed="collapsed" :trigger="null" collapsible width="220">
      <div class="logo">
        <span v-if="!collapsed" class="logo-text">码上回家·管理端</span>
        <span v-else class="logo-text">管</span>
      </div>
      <a-menu theme="dark" mode="inline" :selected-keys="selectedKeys" @click="handleMenuClick">
        <a-menu-item v-for="item in menuItems" :key="item.key">
          <component :is="item.icon" />
          <span>{{ item.label }}</span>
        </a-menu-item>
      </a-menu>
    </a-layout-sider>

    <a-layout>
      <a-layout-header class="header">
        <div class="header-left">
          <menu-unfold-outlined v-if="collapsed" class="trigger" @click="collapsed = !collapsed" />
          <menu-fold-outlined v-else class="trigger" @click="collapsed = !collapsed" />
          <span class="page-title">{{ route.meta.title }}</span>
        </div>
        <div class="header-right">
          <a-dropdown placement="bottomRight">
            <a-space class="user-info" style="cursor: pointer">
              <user-outlined />
              <span>{{ authStore.role }}</span>
            </a-space>
            <template #overlay>
              <a-menu>
                <a-menu-item key="logout" @click="handleLogout">
                  <logout-outlined /> 退出登录
                </a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </div>
      </a-layout-header>

      <a-layout-content class="content">
        <router-view />
      </a-layout-content>
    </a-layout>
  </a-layout>
</template>

<style scoped lang="less">
.admin-layout {
  min-height: 100vh;
}

.logo {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.3);
  overflow: hidden;
}

.logo-text {
  color: #fff;
  font-size: 15px;
  font-weight: 600;
  white-space: nowrap;
}

.header {
  background: #fff;
  padding: 0 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);

  .trigger {
    font-size: 18px;
    cursor: pointer;
    transition: color 0.3s;
    &:hover {
      color: #1890ff;
    }
  }

  .page-title {
    margin-left: 16px;
    font-size: 16px;
    font-weight: 500;
    color: #18242d;
  }
}

.header-left {
  display: flex;
  align-items: center;
}

.header-right {
  display: flex;
  align-items: center;
}

.user-info {
  padding: 4px 8px;
  border-radius: 4px;
  &:hover {
    background: #f5f5f5;
  }
}

.content {
  margin: 24px;
  padding: 24px;
  background: #fff;
  min-height: 280px;
  border-radius: 8px;
}
</style>
