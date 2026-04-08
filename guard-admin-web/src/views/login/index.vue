<script setup lang="ts">
import { reactive, ref, h } from 'vue'
import { useRouter } from 'vue-router'
import { UserOutlined, LockOutlined } from '@ant-design/icons-vue'
import { useAuthStore } from '@/stores/auth'
defineOptions({
  name: 'LoginPage',
})

const router = useRouter()
const authStore = useAuthStore()

// 表单数据
const formState = reactive<{ username: string; password: string }>({
  username: '',
  password: '',
})

// 加载状态
const loading = ref(false)

// 表单验证规则
const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度在 3 到 20 个字符', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度在 6 到 20 个字符', trigger: 'blur' },
  ],
}

// 表单引用
const formRef = ref()

// 提交登录
const handleLogin = async () => {
  try {
    // 验证表单
    await formRef.value.validate()

    loading.value = true

    // 调用登录接口
    await authStore.login(formState)

    // 登录成功，跳转到首页
    router.push('/admin/dashboard')
  } catch (error) {
    console.error('登录失败:', error)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-container">
    <!-- 背景装饰 -->
    <div class="login-background">
      <div class="circle circle-1"></div>
      <div class="circle circle-2"></div>
      <div class="circle circle-3"></div>
    </div>

    <!-- 登录卡片 -->
    <div class="login-card">
      <!-- 顶部标题 -->
      <div class="login-header">
        <div class="logo">
          <img src="../../../public/icon.png" alt="Logo" class="logo-image" />
        </div>
        <h1 class="title">码上回家</h1>
        <p class="subtitle">阿尔兹海默症患者协同寻回系统</p>
        <p class="subtitle">后台管理</p>
      </div>

      <!-- 登录表单 -->
      <a-form
        ref="formRef"
        :model="formState"
        :rules="rules"
        class="login-form"
        @finish="handleLogin"
      >
        <a-form-item name="username">
          <a-input
            v-model:value="formState.username"
            size="large"
            placeholder="请输入用户名"
            :prefix="h(UserOutlined)"
          >
          </a-input>
        </a-form-item>

        <a-form-item name="password">
          <a-input-password
            v-model:value="formState.password"
            size="large"
            placeholder="请输入密码"
            :prefix="h(LockOutlined)"
            @pressEnter="handleLogin"
          >
          </a-input-password>
        </a-form-item>

        <a-form-item>
          <a-button type="primary" size="large" block :loading="loading" html-type="submit">
            登录
          </a-button>
        </a-form-item>
      </a-form>

      <!-- 底部提示 -->
      <div class="login-footer">
        <p class="demo-hint">copyright © 2025 码上回家</p>
      </div>
    </div>
  </div>
</template>

<style scoped lang="less">
.login-container {
  position: relative;
  width: 100vw;
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1677ff 0%, #0958d9 60%, #003eb3 100%);
  overflow: hidden;
}

// 背景动画圆圈
.login-background {
  position: absolute;
  width: 100%;
  height: 100%;
  overflow: hidden;
  z-index: 0;

  .circle {
    position: absolute;
    border-radius: 50%;
    background: rgba(255, 255, 255, 0.1);
    animation: float 20s infinite ease-in-out;
  }

  .circle-1 {
    width: 300px;
    height: 300px;
    top: -100px;
    left: -100px;
    animation-delay: 0s;
  }

  .circle-2 {
    width: 200px;
    height: 200px;
    bottom: -50px;
    right: -50px;
    animation-delay: 5s;
  }

  .circle-3 {
    width: 150px;
    height: 150px;
    top: 50%;
    right: 10%;
    animation-delay: 10s;
  }
}

@keyframes float {
  0%,
  100% {
    transform: translateY(0) scale(1);
  }
  50% {
    transform: translateY(-20px) scale(1.1);
  }
}

// 登录卡片
.login-card {
  position: relative;
  z-index: 1;
  width: 420px;
  padding: 48px 40px;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.2);
}

// 头部
.login-header {
  text-align: center;
  margin-bottom: 40px;

  .logo {
    margin-bottom: 16px;

    .logo-image {
      width: 64px;
      height: 64px;
    }
  }

  .title {
    font-size: 24px;
    font-weight: 600;
    color: #333;
    margin: 0 0 8px 0;
  }

  .subtitle {
    font-size: 14px;
    color: #999;
    margin: 0;
  }
}

// 表单
.login-form {
  .form-options {
    display: flex;
    justify-content: space-between;
    align-items: center;

    .forgot-link {
      color: #1677ff;
      &:hover {
        color: #0958d9;
      }
    }
  }

  :deep(.ant-input-affix-wrapper) {
    padding: 12px 15px;
  }

  :deep(.ant-btn) {
    height: 48px;
    font-size: 16px;
    font-weight: 500;
    background: linear-gradient(135deg, #1677ff 0%, #0958d9 100%);
    border: none;

    &:hover {
      background: linear-gradient(135deg, #4096ff 0%, #1677ff 100%);
    }
  }
}

// 底部
.login-footer {
  margin-top: 24px;
  text-align: center;

  .demo-hint {
    font-size: 12px;
    color: #999;
    padding: 12px;
    background: #f5f5f5;
    border-radius: 8px;
    margin: 0;
    line-height: 1.6;
  }
}

// 响应式
@media (max-width: 768px) {
  .login-card {
    width: 90%;
    padding: 32px 24px;
  }

  .login-header .title {
    font-size: 20px;
  }
}
</style>
