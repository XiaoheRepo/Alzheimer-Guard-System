package com.xiaohelab.guard.android.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TagOrderDto(
    val id: String,
    @SerialName("patient_id") val patientId: String,
    @SerialName("tag_sku") val tagSku: String,
    val quantity: Int,
    @SerialName("total_amount") val totalAmount: Long,
    val status: String,
    @SerialName("shipping_address") val shippingAddress: String? = null,
    @SerialName("tracking_number") val trackingNumber: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("paid_at") val paidAt: String? = null
)

@Serializable
data class CreateOrderRequestDto(
    @SerialName("patient_id") val patientId: String,
    @SerialName("tag_sku") val tagSku: String,
    val quantity: Int,
    @SerialName("shipping_address") val shippingAddress: String
)
