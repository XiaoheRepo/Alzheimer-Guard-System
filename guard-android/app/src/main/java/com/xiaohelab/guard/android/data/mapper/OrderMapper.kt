package com.xiaohelab.guard.android.data.mapper

import com.xiaohelab.guard.android.data.remote.dto.TagOrderDto
import com.xiaohelab.guard.android.domain.model.OrderStatus
import com.xiaohelab.guard.android.domain.model.TagOrder

fun TagOrderDto.toDomain() = TagOrder(
    id = id,
    patientId = patientId,
    tagSku = tagSku,
    quantity = quantity,
    totalAmount = totalAmount,
    status = when (status.uppercase()) {
        "PENDING_PAYMENT" -> OrderStatus.PENDING_PAYMENT
        "PAID" -> OrderStatus.PAID
        "SHIPPED" -> OrderStatus.SHIPPED
        "DELIVERED" -> OrderStatus.DELIVERED
        "CANCELLED" -> OrderStatus.CANCELLED
        else -> OrderStatus.UNKNOWN
    },
    shippingAddress = shippingAddress,
    trackingNumber = trackingNumber,
    createdAt = createdAt,
    paidAt = paidAt
)
