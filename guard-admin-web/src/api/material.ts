// src/api/material.ts
// 对齐 API_V2.0.md §3.4（物资工单）
import { http } from '@/utils/request'
import type { CursorPage, OffsetPage } from '@/types/common'
import type { OrderState } from '@/types/enums'

export interface MaterialOrder {
  order_id: string
  patient_id: string
  patient?: { patient_id: string; display_name?: string; patient_name?: string }
  state: OrderState
  items: Array<{ material_type: string; quantity: number }>
  logistics?: {
    carrier?: string
    tracking_no?: string
    shipped_at?: string | null
  }
  receiver?: {
    name?: string
    phone?: string
    address?: string
  }
  remark?: string
  created_at: string
  updated_at?: string
  trace_id?: string
}

export type OrderListResponse =
  | OffsetPage<MaterialOrder>
  | CursorPage<MaterialOrder>

/** GET /api/v1/material/orders */
export function listOrders(
  params: Record<string, unknown>,
): Promise<OrderListResponse> {
  return http.get<OrderListResponse>('/api/v1/material/orders', { params })
}

/** POST /api/v1/material/orders/{order_id}/approve */
export function approveOrder(
  orderId: string,
  body: { reviewer_note?: string; request_time: string },
): Promise<MaterialOrder> {
  return http.post<MaterialOrder>(
    `/api/v1/material/orders/${encodeURIComponent(orderId)}/approve`,
    body,
  )
}

/** POST /api/v1/material/orders/{order_id}/ship */
export function shipOrder(
  orderId: string,
  body: {
    carrier: string
    tracking_no: string
    request_time: string
  },
): Promise<MaterialOrder> {
  return http.post<MaterialOrder>(
    `/api/v1/material/orders/${encodeURIComponent(orderId)}/ship`,
    body,
  )
}

/** POST /api/v1/material/orders/{order_id}/resolve-exception */
export function resolveOrderException(
  orderId: string,
  body: {
    action: 'RESHIP' | 'VOID'
    reason: string
    new_tracking_no?: string
    new_carrier?: string
    request_time: string
  },
): Promise<MaterialOrder> {
  return http.post<MaterialOrder>(
    `/api/v1/material/orders/${encodeURIComponent(orderId)}/resolve-exception`,
    body,
  )
}

/** POST /api/v1/material/orders/{order_id}/cancel */
export function cancelOrder(
  orderId: string,
  body: { reason: string; request_time: string },
): Promise<MaterialOrder> {
  return http.post<MaterialOrder>(
    `/api/v1/material/orders/${encodeURIComponent(orderId)}/cancel`,
    body,
  )
}
