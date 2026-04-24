package com.xiaohelab.guard.android.feature.task.domain

import com.xiaohelab.guard.android.core.common.MhResult
import com.xiaohelab.guard.android.core.network.handleEnvelope
import com.xiaohelab.guard.android.feature.task.data.CreateTaskRequest
import com.xiaohelab.guard.android.feature.task.data.LocationDto
import com.xiaohelab.guard.android.feature.task.data.RescueTaskApi
import com.xiaohelab.guard.android.feature.task.data.RescueTaskDto
import com.xiaohelab.guard.android.feature.task.data.RescueTaskListDto
import javax.inject.Inject

interface TaskRepository {
    suspend fun listTasks(patientId: String? = null, status: String? = null): MhResult<RescueTaskListDto>
    suspend fun createTask(patientId: String, description: String?, lastSeenLocation: LocationDto?): MhResult<RescueTaskDto>
    suspend fun getTask(taskId: String): MhResult<RescueTaskDto>
    suspend fun cancelTask(taskId: String): MhResult<Unit>
}

class TaskRepositoryImpl @Inject constructor(private val api: RescueTaskApi) : TaskRepository {
    override suspend fun listTasks(patientId: String?, status: String?) =
        handleEnvelope { api.listTasks(patientId, status) }

    override suspend fun createTask(patientId: String, description: String?, lastSeenLocation: LocationDto?) =
        handleEnvelope { api.createTask(CreateTaskRequest(patientId, description, lastSeenLocation)) }

    override suspend fun getTask(taskId: String) = handleEnvelope { api.getTask(taskId) }

    override suspend fun cancelTask(taskId: String) =
        when (val r = handleEnvelope { api.cancelTask(taskId) }) {
            is MhResult.Success -> MhResult.Success(Unit, r.trace)
            is MhResult.Failure -> r
        }
}

// --- UseCases ---
class ListTasksUseCase @Inject constructor(private val repo: TaskRepository) {
    suspend operator fun invoke(patientId: String? = null, status: String? = null) = repo.listTasks(patientId, status)
}

class CreateTaskUseCase @Inject constructor(private val repo: TaskRepository) {
    suspend operator fun invoke(patientId: String, description: String?, lastSeenLocation: LocationDto?) =
        repo.createTask(patientId, description, lastSeenLocation)
}

class GetTaskUseCase @Inject constructor(private val repo: TaskRepository) {
    suspend operator fun invoke(taskId: String) = repo.getTask(taskId)
}

class CancelTaskUseCase @Inject constructor(private val repo: TaskRepository) {
    suspend operator fun invoke(taskId: String) = repo.cancelTask(taskId)
}
