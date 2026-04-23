// src/api/patient.ts
// 管理员端患者档案：对齐 API_V2.0.md §3.3（admin/patients）
import { http } from '@/utils/request'
import type { CursorPage, OffsetPage } from '@/types/common'
import type { PatientStatus } from '@/types/enums'

export interface AdminPatientListItem {
  patient_id: string
  profile_no?: string
  short_code?: string
  patient_name: string
  gender?: 'MALE' | 'FEMALE' | 'UNKNOWN'
  age?: number
  status: PatientStatus
  primary_guardian?: {
    user_id: string
    nickname: string
    phone?: string
  }
  guardian_count?: number
  bound_tag_count?: number
  active_task_id?: string | null
  created_at: string
  profile_version?: number
}

export interface AdminPatientDetail extends AdminPatientListItem {
  birthday?: string
  avatar_url?: string
  chronic_diseases?: string[]
  medication?: string[]
  allergy?: string[]
  appearance?: Record<string, unknown>
  fence?: Record<string, unknown>
  guardian_list?: Array<{
    user_id: string
    nickname: string
    phone?: string
    relation_role: 'PRIMARY' | 'MEMBER'
    relation_status: 'ACTIVE' | 'REVOKED'
  }>
}

export type AdminPatientListResponse =
  | OffsetPage<AdminPatientListItem>
  | CursorPage<AdminPatientListItem>

/** GET /api/v1/admin/patients */
export function listAdminPatients(
  params: Record<string, unknown>,
): Promise<AdminPatientListResponse> {
  return http.get<AdminPatientListResponse>('/api/v1/admin/patients', { params })
}

/** GET /api/v1/admin/patients/{patient_id} */
export function getAdminPatient(patientId: string): Promise<AdminPatientDetail> {
  return http.get<AdminPatientDetail>(`/api/v1/admin/patients/${encodeURIComponent(patientId)}`)
}

/** 强制转移主监护响应（API_V2.0.md §3.3.17） */
export interface ForceTransferPrimaryResponse {
  patient_id: string
  previous_primary_user_id: string
  new_primary_user_id: string
  transferred_at: string
  audit_log_id?: string
}

/** POST /api/v1/admin/patients/{patient_id}/guardians/force-transfer */
export function forceTransferPrimary(
  patientId: string,
  body: { target_user_id: string; reason: string; evidence_url?: string },
): Promise<ForceTransferPrimaryResponse> {
  return http.post<ForceTransferPrimaryResponse>(
    `/api/v1/admin/patients/${encodeURIComponent(patientId)}/guardians/force-transfer`,
    body,
    { headers: { 'X-Confirm-Level': 'CONFIRM_3' } },
  )
}
