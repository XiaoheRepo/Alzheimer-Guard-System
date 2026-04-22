// src/utils/request.ts
// 统一 Axios 封装（WAHB §9 / HC-03/04/08）

import axios, { type AxiosError, type AxiosInstance, type AxiosRequestConfig } from 'axios'
import { message, Modal, notification } from 'ant-design-vue'
import { i18n } from '@/locales'
import { shortId } from '@/utils/uuid'

export interface ApiError {
  code: string
  message: string
  traceId: string
  raw?: unknown
}

type InternalHandlers = {
  onUnauthorized?: () => void
  getAccessToken?: () => string | null | undefined
}

const handlers: InternalHandlers = {}

export function configureRequest(h: InternalHandlers): void {
  Object.assign(handlers, h)
}

const BASE_URL: string =
  import.meta.env.VITE_API_BASE_URL?.replace(/\/$/, '') || ''

export const request: AxiosInstance = axios.create({
  baseURL: BASE_URL,
  timeout: 20000,
  // 保留字符串原样，避免 axios 对超长整型 ID 做 Number 转换
  transformResponse: [(data: unknown) => data],
})

request.interceptors.request.use((config) => {
  const token = handlers.getAccessToken?.()
  if (token) {
    config.headers.set('Authorization', `Bearer ${token}`)
  }

  // HC-04：Trace-Id
  const existingTrace = config.headers.get('X-Trace-Id') as string | null
  config.headers.set('X-Trace-Id', existingTrace || shortId('trc_'))

  // HC-03：写接口幂等 Request-Id
  const method = (config.method ?? 'get').toLowerCase()
  if (['post', 'put', 'delete', 'patch'].includes(method)) {
    if (!config.headers.get('X-Request-Id')) {
      config.headers.set('X-Request-Id', shortId('req_'))
    }
  }

  // HC-08：严禁客户端伪造保留头
  config.headers.delete('X-User-Id')
  config.headers.delete('X-User-Role')

  if (!config.headers.get('Content-Type')) {
    config.headers.set('Content-Type', 'application/json')
  }

  return config
})

function safeParse(raw: unknown): {
  code?: string
  message?: string
  trace_id?: string
  data?: unknown
} | null {
  if (typeof raw !== 'string') return raw as Record<string, unknown> | null
  try {
    return JSON.parse(raw)
  } catch {
    return null
  }
}

function i18nMessage(code: string, fallback?: string): string {
  const key = `error.${code}`
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const te: any = i18n.global.te
  if (te(key)) return i18n.global.t(key) as string
  return fallback || (i18n.global.t('error.UNKNOWN') as string)
}

function handleBizError(
  body: ReturnType<typeof safeParse>,
  traceId: string,
  status: number,
  raw?: unknown,
): Promise<never> {
  const code: string = body?.code ?? `E_HTTP_${status || 'NET'}`
  const msg = i18nMessage(code, body?.message as string)

  if (status === 401 || code === 'E_AUTH_4011' || code === 'E_GOV_4011') {
    handlers.onUnauthorized?.()
  } else if (status === 403 || code === 'E_AUTH_4031') {
    Modal.warning({
      title: i18n.global.t('error.forbidden') as string,
      content: `${msg}\nTrace: ${traceId}`,
    })
  } else if (status === 429 || code === 'E_REQ_4291') {
    notification.warning({
      message: i18n.global.t('error.rateLimited') as string,
      description: msg,
    })
  } else if (!code.startsWith('E_HTTP_NET')) {
    message.error(`${msg} (Trace: ${String(traceId).slice(0, 12)})`)
  } else {
    message.error(i18n.global.t('error.network') as string)
  }

  const err: ApiError = { code, message: msg, traceId, raw }
  return Promise.reject(err)
}

request.interceptors.response.use(
  (resp) => {
    const parsed = safeParse(resp.data)
    const traceId =
      (resp.headers['x-trace-id'] as string) ||
      (parsed?.trace_id as string) ||
      ''
    if (parsed && parsed.code === 'ok') {
      return parsed.data as unknown
    }
    return handleBizError(parsed, traceId, resp.status)
  },
  (err: AxiosError) => {
    const parsed = safeParse(err?.response?.data)
    const traceId =
      (err?.response?.headers?.['x-trace-id'] as string | undefined) ||
      (parsed?.trace_id as string | undefined) ||
      'N/A'
    return handleBizError(parsed, traceId, err?.response?.status ?? 0, err)
  },
)

/** 便捷方法，保留 T 作为返回 data 类型 */
export const http = {
  get: <T = unknown>(url: string, config?: AxiosRequestConfig) =>
    request.get(url, config) as unknown as Promise<T>,
  post: <T = unknown>(url: string, data?: unknown, config?: AxiosRequestConfig) =>
    request.post(url, data, config) as unknown as Promise<T>,
  put: <T = unknown>(url: string, data?: unknown, config?: AxiosRequestConfig) =>
    request.put(url, data, config) as unknown as Promise<T>,
  patch: <T = unknown>(url: string, data?: unknown, config?: AxiosRequestConfig) =>
    request.patch(url, data, config) as unknown as Promise<T>,
  delete: <T = unknown>(url: string, config?: AxiosRequestConfig) =>
    request.delete(url, config) as unknown as Promise<T>,
}
