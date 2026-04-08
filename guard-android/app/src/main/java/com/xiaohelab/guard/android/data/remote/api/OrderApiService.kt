package com.xiaohelab.guard.android.data.remote.api

import com.xiaohelab.guard.android.core.network.dto.ApiResponseDto
import com.xiaohelab.guard.android.data.remote.dto.CreateOrderRequestDto
import com.xiaohelab.guard.android.data.remote.dto.TagOrderDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface OrderApiService {
    @GET("api/v1/orders")
    suspend fun getOrders(): Response<ApiResponseDto<List<TagOrderDto>>>

    @GET("api/v1/orders/{id}")
    suspend fun getOrderById(@Path("id") id: String): Response<ApiResponseDto<TagOrderDto>>

    @POST("api/v1/orders")
    suspend fun createOrder(@Body request: CreateOrderRequestDto): Response<ApiResponseDto<TagOrderDto>>

    @DELETE("api/v1/orders/{id}")
    suspend fun cancelOrder(@Path("id") id: String): Response<ApiResponseDto<Unit>>
}
