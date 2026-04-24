package com.xiaohelab.guard.android.feature.mat.data

import com.xiaohelab.guard.android.core.network.ApiEnvelope
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * M3-B 物资域 API。
 * HC-ID-String: order_id / patient_id 均为 String。
 * HC-02: state 由服务端维护，客户端只展示不推算。
 * 错误码: E_MAT_4041 / E_MAT_4291。
 */
interface MaterialOrderApi {
    @GET("/api/v1/patients/{patient_id}/material-orders")
    suspend fun listOrders(
        @Path("patient_id") patientId: String,
    ): Response<ApiEnvelope<MaterialOrderListDto>>

    @POST("/api/v1/patients/{patient_id}/material-orders")
    suspend fun createOrder(
        @Path("patient_id") patientId: String,
        @Body body: MaterialOrderCreateRequest,
    ): Response<ApiEnvelope<MaterialOrderDto>>

    @GET("/api/v1/patients/{patient_id}/material-orders/{order_id}")
    suspend fun getOrder(
        @Path("patient_id") patientId: String,
        @Path("order_id") orderId: String,
    ): Response<ApiEnvelope<MaterialOrderDto>>
}

@Serializable
data class MaterialOrderListDto(val items: List<MaterialOrderDto> = emptyList())

/** HC-ID-String: order_id / patient_id 均为 String。HC-02: state 只读展示。 */
@Serializable
data class MaterialOrderDto(
    @SerialName("order_id") val orderId: String,
    @SerialName("patient_id") val patientId: String,
    @SerialName("item_code") val itemCode: String,
    val quantity: Int,
    /** HC-02: 状态由服务端维护。 */
    val state: String,
    @SerialName("delivery_address") val deliveryAddress: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class MaterialOrderCreateRequest(
    @SerialName("item_code") val itemCode: String,
    val quantity: Int,
    @SerialName("delivery_address") val deliveryAddress: String? = null,
)
