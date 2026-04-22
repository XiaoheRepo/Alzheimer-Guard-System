// src/api/tag.ts
// 对齐 API_V2.0.md §3.4（标签库存 / 批量发号）
import { http } from '@/utils/request'

export interface TagInventorySummary {
  unbound?: number
  allocated?: number
  bound?: number
  suspected_lost?: number
  lost?: number
  voided?: number
  trend?: { daily?: Array<{ date: string; value: number; series?: string }> }
  [key: string]: unknown
}

export interface BatchJob {
  job_id: string
  status: 'PENDING' | 'RUNNING' | 'DONE' | 'FAILED'
  total: number
  generated: number
  created_at: string
  updated_at?: string
  remark?: string
  download_url?: string | null
  error_message?: string
}

/** GET /api/v1/tags/inventory/summary */
export function getInventorySummary(): Promise<TagInventorySummary> {
  return http.get<TagInventorySummary>('/api/v1/tags/inventory/summary')
}

/** POST /api/v1/tags/batch-generate */
export function batchGenerate(body: {
  count: number
  remark?: string
}): Promise<{ job_id: string }> {
  return http.post<{ job_id: string }>('/api/v1/tags/batch-generate', body)
}

/** GET /api/v1/tags/batch-generate/jobs/{job_id} */
export function getBatchJob(jobId: string): Promise<BatchJob> {
  return http.get<BatchJob>(
    `/api/v1/tags/batch-generate/jobs/${encodeURIComponent(jobId)}`,
  )
}
