import request from './request'
import type { PageOffset, PageCursor } from '@/types'

// ── 物资工单状态 ──────────────────────────────────────────────────
export type OrderStatus =
  | 'PENDING'
  | 'PROCESSING'
  | 'CANCEL_PENDING'
  | 'SHIPPED'
  | 'EXCEPTION'
  | 'COMPLETED'
  | 'CANCELLED'

// ── 标签状态 ──────────────────────────────────────────────────────
export type TagStatus = 'UNBOUND' | 'ALLOCATED' | 'BOUND' | 'LOST' | 'VOID'
export type TagType = 'QR_CODE' | 'NFC'

// ── 工单列表项（管理端） ──────────────────────────────────────────
export interface MaterialOrderItem {
  order_id: string
  patient_id: string
  applicant_user_id: string
  quantity: number
  status: OrderStatus
  apply_note: string
  created_at: string
}

// ── 工单详情（管理端） ────────────────────────────────────────────
export interface MaterialOrderDetail {
  order_id: string
  order_no: string
  patient_id: string
  applicant_user_id: string
  tag_code: string | null
  quantity: number
  status: OrderStatus
  delivery_address: string | null
  tracking_number: string | null
  cancel_reason: string | null
  exception_desc: string | null
  created_at: string
  updated_at: string
}

// ── 工单时间线条目 ────────────────────────────────────────────────
export interface OrderTimelineItem {
  timeline_id: string
  from_status: string
  to_status: string
  operator_user_id: string
  remark: string | null
  created_at: string
}

// ── 标签列表项 ────────────────────────────────────────────────────
export interface TagItem {
  tag_code: string
  tag_type: TagType
  status: TagStatus
  patient_id: string | null
  updated_at: string
}

// ── 标签详情 ──────────────────────────────────────────────────────
export interface TagDetail {
  tag_code: string
  tag_type: TagType
  status: TagStatus
  patient_id: string | null
  order_id: string | null
  batch_no: string | null
  last_bound_at: string | null
  updated_at: string
}

// ── 查询参数 ──────────────────────────────────────────────────────
export interface OrderListParams {
  page_no?: number
  page_size?: number
  patient_id?: string
  applicant_user_id?: string
  status?: string
}

export interface TagListParams {
  page_no?: number
  page_size?: number
  status?: TagStatus
  patient_id?: string
}

// ── API 函数 ──────────────────────────────────────────────────────

/** 3.6.3 管理端工单列表 */
export const getAdminOrderList = (
  params?: OrderListParams
): Promise<PageOffset<MaterialOrderItem>> =>
  request.get('/admin/material/orders', { params }) as unknown as Promise<
    PageOffset<MaterialOrderItem>
  >

/** 3.4.20 管理端工单详情 */
export const getAdminOrderDetail = (orderId: string): Promise<MaterialOrderDetail> =>
  request.get(`/admin/material/orders/${orderId}`) as unknown as Promise<MaterialOrderDetail>

/** 3.4.21 工单状态流转时间线 */
export const getOrderTimeline = (
  orderId: string,
  params?: { cursor?: string; page_size?: number }
): Promise<PageCursor<OrderTimelineItem>> =>
  request.get(`/admin/material/orders/${orderId}/timeline`, {
    params,
  }) as unknown as Promise<PageCursor<OrderTimelineItem>>

/** 3.4.7 审核通过 */
export const approveOrder = (
  orderId: string,
  body?: { approve_note?: string }
): Promise<{ order_id: string; status: OrderStatus; tag_code: string; approved_at: string }> =>
  request.put(`/admin/material/orders/${orderId}/approve`, body) as unknown as Promise<{
    order_id: string
    status: OrderStatus
    tag_code: string
    approved_at: string
  }>

/** 3.4.8 同意取消申请 */
export const cancelApproveOrder = (
  orderId: string,
  body: { reason: string }
): Promise<{ order_id: string; status: OrderStatus; cancelled_at: string }> =>
  request.put(`/admin/material/orders/${orderId}/cancel/approve`, body) as unknown as Promise<{
    order_id: string
    status: OrderStatus
    cancelled_at: string
  }>

/** 3.4.9 拒绝取消申请 */
export const cancelRejectOrder = (
  orderId: string,
  body: { reason: string }
): Promise<{ order_id: string; status: OrderStatus }> =>
  request.put(`/admin/material/orders/${orderId}/cancel/reject`, body) as unknown as Promise<{
    order_id: string
    status: OrderStatus
  }>

/** 3.4.10 发货 */
export const shipOrder = (
  orderId: string,
  body: { tracking_number: string; ship_note?: string }
): Promise<{ order_id: string; status: OrderStatus; tracking_number: string; shipped_at: string }> =>
  request.put(`/admin/material/orders/${orderId}/ship`, body) as unknown as Promise<{
    order_id: string
    status: OrderStatus
    tracking_number: string
    shipped_at: string
  }>

/** 3.4.11 报告物流异常 */
export const reportLogisticsException = (
  orderId: string,
  body: { exception_desc: string }
): Promise<{ order_id: string; status: OrderStatus; exception_desc: string }> =>
  request.put(`/admin/material/orders/${orderId}/logistics-exception`, body) as unknown as Promise<{
    order_id: string
    status: OrderStatus
    exception_desc: string
  }>

/** 3.4.12 补发 */
export const reshipOrder = (
  orderId: string,
  body: { tracking_number: string; reship_note?: string }
): Promise<{ order_id: string; status: OrderStatus; tracking_number: string; reshipped_at: string }> =>
  request.put(`/admin/material/orders/${orderId}/reship`, body) as unknown as Promise<{
    order_id: string
    status: OrderStatus
    tracking_number: string
    reshipped_at: string
  }>

/** 3.4.13 关闭物流异常 */
export const closeException = (
  orderId: string,
  body: { close_note?: string }
): Promise<{ order_id: string; status: OrderStatus; closed_at: string }> =>
  request.put(`/admin/material/orders/${orderId}/close-exception`, body) as unknown as Promise<{
    order_id: string
    status: OrderStatus
    closed_at: string
  }>

/** 3.4.22 标签列表 */
export const getTagList = (params?: TagListParams): Promise<PageOffset<TagItem>> =>
  request.get('/admin/tags', { params }) as unknown as Promise<PageOffset<TagItem>>

/** 3.4.23 标签详情 */
export const getTagDetail = (tagCode: string): Promise<TagDetail> =>
  request.get(`/admin/tags/${tagCode}`) as unknown as Promise<TagDetail>

/** 3.4.6 作废标签 */
export const voidTag = (
  tagCode: string,
  body: { void_reason: string }
): Promise<{ tag_code: string; status: TagStatus; void_reason: string; void_at: string }> =>
  request.post(`/admin/tags/${tagCode}/void`, body) as unknown as Promise<{
    tag_code: string
    status: TagStatus
    void_reason: string
    void_at: string
  }>

/** 3.4.16 重置标签 */
export const resetTag = (
  tagCode: string,
  body: { reset_reason: string }
): Promise<{ tag_code: string; status: TagStatus; reset_at: string }> =>
  request.post(`/admin/tags/${tagCode}/reset`, body) as unknown as Promise<{
    tag_code: string
    status: TagStatus
    reset_at: string
  }>

/** 3.4.17 恢复标签 */
export const recoverTag = (
  tagCode: string,
  body: { recover_reason: string }
): Promise<{ tag_code: string; status: TagStatus; recovered_at: string }> =>
  request.post(`/admin/tags/${tagCode}/recover`, body) as unknown as Promise<{
    tag_code: string
    status: TagStatus
    recovered_at: string
  }>

/** 3.4.15 导入标签（批量） */
export const importTags = (file: File): Promise<{ imported_count: number; batch_no: string }> => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/admin/tags/import', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  }) as unknown as Promise<{ imported_count: number; batch_no: string }>
}
