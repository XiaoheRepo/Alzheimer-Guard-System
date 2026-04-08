package com.xiaohelab.guard.android.domain.repository

import com.xiaohelab.guard.android.core.common.ApiResult
import com.xiaohelab.guard.android.domain.model.CreateTagOrderRequest
import com.xiaohelab.guard.android.domain.model.TagOrder

interface OrderRepository {
    suspend fun getOrders(): ApiResult<List<TagOrder>>
    suspend fun getOrderById(orderId: String): ApiResult<TagOrder>
    suspend fun createOrder(request: CreateTagOrderRequest): ApiResult<TagOrder>
    suspend fun cancelOrder(orderId: String): ApiResult<Unit>
}
