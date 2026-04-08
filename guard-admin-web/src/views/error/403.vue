<template>
  <div style="padding: 80px; text-align: center">
    <h2>403 - 无访问权限</h2>
    <p>您没有权限访问该页面，请联系管理员。</p>
    <a-button type="primary" @click="$router.push('/admin/dashboard')">返回首页</a-button>
  </div>
</template>
