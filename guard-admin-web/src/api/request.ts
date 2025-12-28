/**
 * Axios 请求封装
 */

import axios, { AxiosError } from 'axios'
import type { AxiosInstance, AxiosResponse } from 'axios'
import { message } from 'ant-design-vue'
import type { ApiResponse } from '@/types'
import { getToken, removeToken } from '../utils/auth'

// 创建 axios 实例
const request: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
})

// 请求拦截器
request.interceptors.request.use(
  (config) => {
    // 自动注入 Token
    const token = getToken()
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error: AxiosError) => {
    console.error('请求错误:', error)
    return Promise.reject(error)
  },
)

// 响应拦截器
request.interceptors.response.use(
  (response: AxiosResponse<ApiResponse>) => {
    const res = response.data

    // 根据业务状态码判断
    if (res.code !== 200) {
      message.error(res.message || '请求失败')

      // 401 未授权 - 跳转登录
      if (res.code === 401) {
        removeToken()
        window.location.href = '/login'
      }

      // 403 无权限
      if (res.code === 403) {
        message.error('您没有权限执行此操作')
      }

      return Promise.reject(new Error(res.message || '请求失败'))
    }

    // 返回完整的 response，而不是 res.data
    return response
  },
  (error: AxiosError<ApiResponse>) => {
    console.error('响应错误:', error)

    // 网络错误处理
    if (!error.response) {
      message.error('网络连接失败，请检查网络')
      return Promise.reject(error)
    }

    const { status, data } = error.response

    switch (status) {
      case 400:
        message.error(data?.message || '请求参数错误')
        break
      case 401:
        message.error('登录已过期，请重新登录')
        removeToken()
        window.location.href = '/login'
        break
      case 403:
        message.error('没有权限访问')
        break
      case 404:
        message.error('请求的资源不存在')
        break
      case 500:
        message.error('服务器错误，请稍后重试')
        break
      default:
        message.error(data?.message || '请求失败')
    }

    return Promise.reject(error)
  },
)

export default request
