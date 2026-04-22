// src/api/clue.ts
// 对齐 API_V2.0.md §3.2
import { http } from '@/utils/request'
import type { CursorPage, OffsetPage } from '@/types/common'
import type { ClueReviewState, RiskLevel } from '@/types/enums'

export interface ClueListItem {
  clue_id: string
  task_id: string
  patient_id?: string
  reported_at: string
  review_state: ClueReviewState
  risk_level?: RiskLevel
  confidence?: number
  location_name?: string
  lat?: number
  lng?: number
  media?: Array<{ type: 'IMAGE' | 'VIDEO'; url: string; thumb_url?: string }>
  ai_summary?: string
  review_history?: Array<{
    actor_id: string
    action: string
    reason?: string
    trace_id?: string
    reviewed_at: string
  }>
}

export interface ClueListResponse
  extends Partial<OffsetPage<ClueListItem>>,
    Partial<CursorPage<ClueListItem>> {
  items: ClueListItem[]
}

/** GET /api/v1/clues */
export function listClues(params: Record<string, unknown>): Promise<ClueListResponse> {
  return http.get<ClueListResponse>('/api/v1/clues', { params })
}

/** POST /api/v1/clues/{clue_id}/override */
export function overrideClue(
  clueId: string,
  body: { reason: string; request_time: string },
): Promise<ClueListItem> {
  return http.post<ClueListItem>(`/api/v1/clues/${encodeURIComponent(clueId)}/override`, body)
}

/** POST /api/v1/clues/{clue_id}/reject */
export function rejectClue(
  clueId: string,
  body: { reason: string; request_time: string },
): Promise<ClueListItem> {
  return http.post<ClueListItem>(`/api/v1/clues/${encodeURIComponent(clueId)}/reject`, body)
}
