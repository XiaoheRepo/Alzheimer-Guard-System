package com.xiaohelab.guard.android.core.network.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/** 统一响应包装（对应 §1.4 通用响应结构） */
@Serializable
data class ApiResponseDto<T>(
    val code: String,
    val message: String,
    val trace_id: String? = null,
    val data: T? = null
) {
    val isSuccess: Boolean get() = code == "OK"
}

/** Offset 分页响应包装 */
@Serializable
data class PagedResponseDto<T>(
    val items: List<T>,
    val page_no: Int = 1,
    val page_size: Int = 20,
    val total: Int = 0,
    val has_next: Boolean = false
)

/** Cursor 分页响应包装（适用于通知、事件流等追加写入型列表） */
@Serializable
data class CursorPagedResponseDto<T>(
    val items: List<T>,
    val page_size: Int = 20,
    val next_cursor: String? = null,
    val has_next: Boolean = false
)
