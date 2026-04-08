/**
 * Axios 请求封装
 * 依据 web_admin_handbook.md §4.1-4.3：
 *   - 全请求注入 X-Trace-Id
 *   - 写请求（POST/PUT/PATCH/DELETE）注入 X-Request-Id
 *   - 响应拦截统一处理字符串业务错误码（E_GOV_4011 等）
 *   - 成功时直接返回 body.data，调用方无需再解包
 */

import axios from 'axios'
import type { AxiosError, InternalAxiosRequestConfig } from 'axios'
import { message } from 'ant-design-vue'
import { getToken, clearSession } from '@/utils/auth'

const WRITE_METHODS = new Set(['POST', 'PUT', 'PATCH', 'DELETE'])

function genTraceId(): string {
  return `trc_web_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`
}

function genRequestId(): string {
  return `req_web_${Date.now()}_${Math.random().toString(36).slice(2, 10)}`
}

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' },
})

// 请求拦截器
request.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    // 全请求必须带 X-Trace-Id
    config.headers['X-Trace-Id'] = genTraceId()

    // 写请求必须带 X-Request-Id，禁止复用（每次请求生成新 ID）
    if (WRITE_METHODS.has((config.method ?? 'GET').toUpperCase())) {
      config.headers['X-Request-Id'] = genRequestId()
    }

    const token = getToken()
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }

    return config
  },
  (error: AxiosError) => Promise.reject(error),
)

// 响应拦截器：统一处理业务错误码（字符串）
request.interceptors.response.use(
  (resp) => {
    const body = resp.data
    if (!body || body.code !== 'OK') {
      const code: string = body?.code ?? 'E_UNKNOWN'
      const msg: string = body?.message ?? '请求失败'
      const traceId: string | undefined = body?.trace_id

      // E_GOV_4011：鉴权失败，清会话并跳转登录
      if (code === 'E_GOV_4011') {
        message.error('登录已失效，请重新登录')
        clearSession()
        window.location.href = '/login'
        return Promise.reject({ code, message: msg, traceId })
      }

      // E_GOV_4031：账号已封禁
      if (code === 'E_GOV_4031') {
        message.error('账号已被封禁，请联系管理员')
        clearSession()
        window.location.href = '/login'
        return Promise.reject({ code, message: msg, traceId })
      }

      // E_GOV_4030：角色权限不足
      if (code === 'E_GOV_4030') {
        message.error('权限不足，无法执行此操作')
        return Promise.reject({ code, message: msg, traceId })
      }

      // E_GOV_4032：高危操作仅限 SUPERADMIN
      if (code === 'E_GOV_4032') {
        message.error('此操作仅限超级管理员执行')
        return Promise.reject({ code, message: msg, traceId })
      }

      // E_GOV_4291/4292：限流，调用方自行读取 Retry-After 处理倒计时
      if (code === 'E_GOV_4291' || code === 'E_GOV_4292') {
        message.warning('操作过于频繁，请稍后再试')
        return Promise.reject({ code, message: msg, traceId })
      }

      // E_GOV_5002：审计写入失败，关键操作已回滚
      if (code === 'E_GOV_5002') {
        message.error('操作失败：审计写入异常，已回滚')
        return Promise.reject({ code, message: msg, traceId })
      }

      message.error(msg)
      return Promise.reject({ code, message: msg, traceId })
    }

    // 成功：直接返回 data 部分，调用方无需再解包
    return body.data
  },
  (error: AxiosError) => {
    if (!error.response) {
      message.error('网络连接失败，请检查网络')
      return Promise.reject(error)
    }
    // HTTP 层错误兜底（通常 body 中已含业务错误码，此处仅做降级提示）
    const msg = (error.response.data as { message?: string })?.message ?? '服务器错误'
    message.error(msg)
    return Promise.reject(error)
  },
)

export default request
