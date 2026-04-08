package com.xiaohelab.guard.android.domain.model

enum class OrderStatus {
    PENDING_PAYMENT, PAID, SHIPPED, DELIVERED, CANCELLED, UNKNOWN
}

data class TagOrder(
    val id: String,
    val patientId: String,
    val tagSku: String,
    val quantity: Int,
    val totalAmount: Long,
    val status: OrderStatus,
    val shippingAddress: String?,
    val trackingNumber: String?,
    val createdAt: String,
    val paidAt: String?
)

data class CreateTagOrderRequest(
    val patientId: String,
    val tagSku: String,
    val quantity: Int,
    val shippingAddress: String
)
