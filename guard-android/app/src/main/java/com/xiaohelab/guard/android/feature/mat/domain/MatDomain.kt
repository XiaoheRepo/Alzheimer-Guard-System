package com.xiaohelab.guard.android.feature.mat.domain

import com.xiaohelab.guard.android.core.common.MhResult
import com.xiaohelab.guard.android.core.network.handleEnvelope
import com.xiaohelab.guard.android.feature.mat.data.MaterialOrderApi
import com.xiaohelab.guard.android.feature.mat.data.MaterialOrderCreateRequest
import com.xiaohelab.guard.android.feature.mat.data.MaterialOrderDto
import com.xiaohelab.guard.android.feature.mat.data.MaterialOrderListDto
import javax.inject.Inject

interface MaterialOrderRepository {
    suspend fun listOrders(patientId: String): MhResult<MaterialOrderListDto>
    suspend fun createOrder(patientId: String, itemCode: String, quantity: Int, deliveryAddress: String?): MhResult<MaterialOrderDto>
    suspend fun getOrder(patientId: String, orderId: String): MhResult<MaterialOrderDto>
}

class MaterialOrderRepositoryImpl @Inject constructor(private val api: MaterialOrderApi) : MaterialOrderRepository {
    override suspend fun listOrders(patientId: String) = handleEnvelope { api.listOrders(patientId) }
    override suspend fun createOrder(patientId: String, itemCode: String, quantity: Int, deliveryAddress: String?) =
        handleEnvelope { api.createOrder(patientId, MaterialOrderCreateRequest(itemCode, quantity, deliveryAddress)) }
    override suspend fun getOrder(patientId: String, orderId: String) = handleEnvelope { api.getOrder(patientId, orderId) }
}

// --- UseCases ---
class ListMaterialOrdersUseCase @Inject constructor(private val repo: MaterialOrderRepository) {
    suspend operator fun invoke(patientId: String) = repo.listOrders(patientId)
}

class CreateMaterialOrderUseCase @Inject constructor(private val repo: MaterialOrderRepository) {
    suspend operator fun invoke(patientId: String, itemCode: String, quantity: Int, deliveryAddress: String?) =
        repo.createOrder(patientId, itemCode, quantity, deliveryAddress)
}

class GetMaterialOrderUseCase @Inject constructor(private val repo: MaterialOrderRepository) {
    suspend operator fun invoke(patientId: String, orderId: String) = repo.getOrder(patientId, orderId)
}
