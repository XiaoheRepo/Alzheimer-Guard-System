// src/utils/format.ts
// 统一的日期/数字/脱敏格式化，配合 i18n

import dayjs from 'dayjs'
import relativeTime from 'dayjs/plugin/relativeTime'
import utc from 'dayjs/plugin/utc'
import timezone from 'dayjs/plugin/timezone'

dayjs.extend(relativeTime)
dayjs.extend(utc)
dayjs.extend(timezone)

export function fmtDateTime(v?: string | number | null): string {
  if (!v) return '-'
  const d = dayjs(v)
  return d.isValid() ? d.format('YYYY-MM-DD HH:mm:ss') : '-'
}

export function fmtDate(v?: string | number | null): string {
  if (!v) return '-'
  const d = dayjs(v)
  return d.isValid() ? d.format('YYYY-MM-DD') : '-'
}

export function fmtFromNow(v?: string | number | null): string {
  if (!v) return '-'
  const d = dayjs(v)
  return d.isValid() ? d.fromNow() : '-'
}

/** 手机号脱敏（HC-07） */
export function maskPhone(v?: string | null): string {
  if (!v) return '-'
  if (v.length < 7) return v
  return `${v.slice(0, 3)}****${v.slice(-4)}`
}

/** 邮箱脱敏 */
export function maskEmail(v?: string | null): string {
  if (!v) return '-'
  const [name, domain] = v.split('@')
  if (!domain || !name) return v
  if (name.length <= 2) return `${name[0]}*@${domain}`
  return `${name.slice(0, 2)}****@${domain}`
}

/** 身份证/长文本脱敏 */
export function maskIdCard(v?: string | null): string {
  if (!v) return '-'
  if (v.length < 10) return v
  return `${v.slice(0, 4)}**********${v.slice(-4)}`
}

/** ID 短展示（前 8 位） */
export function shortIdText(v?: string | null): string {
  if (!v) return '-'
  return v.length > 8 ? v.slice(0, 8) + '…' : v
}

export function truncate(v: string | undefined | null, max = 20): string {
  if (!v) return '-'
  return v.length > max ? v.slice(0, max) + '…' : v
}

export default dayjs
