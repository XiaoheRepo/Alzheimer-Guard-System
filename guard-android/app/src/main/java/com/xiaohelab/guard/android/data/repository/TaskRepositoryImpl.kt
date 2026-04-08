package com.xiaohelab.guard.android.data.repository

import com.xiaohelab.guard.android.core.common.ApiResult
import com.xiaohelab.guard.android.core.database.dao.TaskSnapshotDao
import com.xiaohelab.guard.android.core.database.entity.TaskSnapshotEntity
import com.xiaohelab.guard.android.core.network.NetworkModule.safeApiCall
import com.xiaohelab.guard.android.data.mapper.toDomain
import com.xiaohelab.guard.android.data.remote.api.TaskApiService
import com.xiaohelab.guard.android.data.remote.dto.CloseTaskRequestDto
import com.xiaohelab.guard.android.data.remote.dto.CreateTaskRequestDto
import com.xiaohelab.guard.android.domain.model.CloseTaskRequest
import com.xiaohelab.guard.android.domain.model.Task
import com.xiaohelab.guard.android.domain.model.TaskEvent
import com.xiaohelab.guard.android.domain.model.TrajectoryPoint
import com.xiaohelab.guard.android.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepositoryImpl @Inject constructor(
    private val api: TaskApiService,
    private val snapshotDao: TaskSnapshotDao
) : TaskRepository {

    override fun observeTasks(): Flow<List<Task>> =
        snapshotDao.observeAll().map { list ->
            list.map { entity ->
                Task(
                    id = entity.taskId,
                    patientId = entity.patientId,
                    patientNameMasked = entity.patientNameMasked,
                    status = com.xiaohelab.guard.android.domain.model.TaskStatus.valueOf(entity.status),
                    source = com.xiaohelab.guard.android.domain.model.TaskSource.valueOf(entity.source),
                    startTime = entity.startTime,
                    latestEventTime = entity.latestEventTime,
                    remark = entity.remark,
                    posterUrl = entity.posterUrl,
                    version = entity.version
                )
            }
        }

    override suspend fun fetchTasks(patientId: String?): ApiResult<List<Task>> {
        val result = safeApiCall { api.getTasks(patientId = patientId) }
        if (result is ApiResult.Success) {
            val entities = result.data.map { dto ->
                TaskSnapshotEntity(
                    taskId = dto.id,
                    patientId = dto.patientId,
                    patientNameMasked = dto.patientNameMasked,
                    status = dto.status.uppercase(),
                    source = dto.source.uppercase(),
                    startTime = dto.startTime,
                    latestEventTime = dto.latestEventTime,
                    remark = dto.remark,
                    posterUrl = dto.posterUrl,
                    version = dto.version
                )
            }
            snapshotDao.upsertAll(entities)
        }
        return result.map { list -> list.map { it.toDomain() } }
    }

    override suspend fun fetchTaskById(taskId: String): ApiResult<Task> {
        val result = safeApiCall { api.getTaskSnapshot(taskId) }
        if (result is ApiResult.Success) {
            val dto = result.data
            val entity = TaskSnapshotEntity(
                taskId = dto.id,
                patientId = dto.patientId,
                patientNameMasked = dto.patientNameMasked,
                status = dto.status.uppercase(),
                source = dto.source.uppercase(),
                startTime = dto.startTime,
                latestEventTime = dto.latestEventTime,
                remark = dto.remark,
                posterUrl = dto.posterUrl,
                version = dto.version
            )
            snapshotDao.upsert(entity)
        }
        return result.map { it.toDomain() }
    }

    override suspend fun createTask(
        patientId: String, source: String, remark: String?
    ): ApiResult<Task> =
        safeApiCall { api.createTask(CreateTaskRequestDto(patientId, source, remark)) }
            .map { it.toDomain() }

    override suspend fun closeTask(taskId: String, request: CloseTaskRequest): ApiResult<Unit> =
        safeApiCall {
            api.closeTask(
                taskId,
                CloseTaskRequestDto(
                    reason = request.reason.name,
                    remarks = request.remarks
                )
            )
        }

    override suspend fun getLatestTrajectory(taskId: String): ApiResult<List<TrajectoryPoint>> =
        safeApiCall { api.getLatestTrajectory(taskId) }.map { list -> list.map { it.toDomain() } }

    override suspend fun pollEvents(taskId: String, afterEventId: String?): ApiResult<List<TaskEvent>> =
        safeApiCall { api.pollEvents(taskId, afterEventId) }.map { list -> list.map { it.toDomain() } }

    override suspend fun getWsTicket(): ApiResult<String> =
        safeApiCall { api.getWsTicket() }.map { it.ticket }
}
