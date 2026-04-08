/**
 * 线索治理 API
 * 依据 doc/API_from_SRS_SADD_LLD.md §3.2
 */

import request from './request'
import type { PageOffset } from '@/types'

export type ClueStatus = 'PENDING' | 'OVERRIDDEN' | 'REJECTED' | 'UNKNOWN'

export interface ClueItem {
  clue_id: string
  task_id: string
  reporter_user_id: string
  risk_score: number
  status: ClueStatus
  created_at: string
}

export interface ClueReviewParams {
  page_no?: number
  page_size?: number
}

export interface OverrideBody {
  reason: string
}

export interface AssignBody {
  assignee_user_id: string
  reason?: string
}

/** GET /api/v1/admin/clues/review/queue */
export const getClueReviewQueue = (params?: ClueReviewParams): Promise<PageOffset<ClueItem>> =>
  request.get('/admin/clues/review/queue', { params }) as unknown as Promise<PageOffset<ClueItem>>

/** GET /api/v1/admin/clues/{clue_id} */
export const getClueDetail = (clueId: string): Promise<ClueItem> =>
  request.get(`/admin/clues/${clueId}`) as unknown as Promise<ClueItem>

/** POST /api/v1/clues/{clue_id}/override */
export const overrideClue = (clueId: string, body: OverrideBody): Promise<unknown> =>
  request.post(`/clues/${clueId}/override`, body)

/** POST /api/v1/clues/{clue_id}/reject */
export const rejectClue = (clueId: string, body: OverrideBody): Promise<unknown> =>
  request.post(`/clues/${clueId}/reject`, body)

/** POST /api/v1/admin/clues/{clue_id}/assign */
export const assignClue = (clueId: string, body: AssignBody): Promise<unknown> =>
  request.post(`/admin/clues/${clueId}/assign`, body)

/** GET /api/v1/admin/clues/suspected */
export const getSuspectedClues = (): Promise<PageOffset<ClueItem>> =>
  request.get('/admin/clues/suspected') as unknown as Promise<PageOffset<ClueItem>>
