import { createApp } from 'vue'
import { createPinia } from 'pinia'
import piniaPluginPersistedstate from 'pinia-plugin-persistedstate'
import Antd from 'ant-design-vue'
import 'ant-design-vue/dist/reset.css'

import App from './App.vue'
import router from './router'
import { i18n } from './locales'
import { registerDirectives } from './directives'
import { configureRequest } from './utils/request'
import { useAuthStore } from './stores/auth'

import './styles/global.less'

const pinia = createPinia()
pinia.use(piniaPluginPersistedstate)

const app = createApp(App)

app.use(pinia)
app.use(router)
app.use(i18n)
app.use(Antd)

registerDirectives(app)

// 注入 request 所需的 token 读取与 401 处理
configureRequest({
  getAccessToken: () => {
    const auth = useAuthStore()
    return auth.accessToken
  },
  onUnauthorized: () => {
    const auth = useAuthStore()
    const prev = router.currentRoute.value.fullPath
    void auth.logout()
    if (router.currentRoute.value.path !== '/login') {
      void router.push({
        path: '/login',
        query: prev && prev !== '/' ? { redirect: prev } : undefined,
      })
    }
  },
})

app.mount('#app')
