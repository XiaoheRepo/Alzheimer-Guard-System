package com.xiaohelab.guard.android.data.repository

import com.xiaohelab.guard.android.core.common.ApiResult
import com.xiaohelab.guard.android.core.network.safeApiCall
import com.xiaohelab.guard.android.core.common.map
import com.xiaohelab.guard.android.core.common.map
import com.xiaohelab.guard.android.data.mapper.toDomain
import com.xiaohelab.guard.android.data.remote.api.OrderApiService
import com.xiaohelab.guard.android.data.remote.dto.CreateOrderRequestDto
import com.xiaohelab.guard.android.domain.model.CreateTagOrderRequest
import com.xiaohelab.guard.android.domain.model.TagOrder
import com.xiaohelab.guard.android.domain.repository.OrderRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OrderRepositoryImpl @Inject constructor(
    private val api: OrderApiService
) : OrderRepository {

    override suspend fun getOrders(): ApiResult<List<TagOrder>> =
        safeApiCall { api.getOrders() }.map { list -> list.map { it.toDomain() } }

    override suspend fun getOrderById(orderId: String): ApiResult<TagOrder> =
        safeApiCall { api.getOrderById(orderId) }.map { it.toDomain() }

    override suspend fun createOrder(request: CreateTagOrderRequest): ApiResult<TagOrder> =
        safeApiCall {
            api.createOrder(
                CreateOrderRequestDto(
                    request.patientId,
                    request.tagSku,
                    request.quantity,
                    request.shippingAddress
                )
            )
        }.map { it.toDomain() }

    override suspend fun cancelOrder(orderId: String): ApiResult<Unit> =
        safeApiCall { api.cancelOrder(orderId) }
}
