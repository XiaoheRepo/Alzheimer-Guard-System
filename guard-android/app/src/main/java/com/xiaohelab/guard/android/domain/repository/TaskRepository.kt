package com.xiaohelab.guard.android.domain.repository

import com.xiaohelab.guard.android.core.common.ApiResult
import com.xiaohelab.guard.android.domain.model.CloseTaskRequest
import com.xiaohelab.guard.android.domain.model.Task
import com.xiaohelab.guard.android.domain.model.TaskEvent
import com.xiaohelab.guard.android.domain.model.TrajectoryPoint
import kotlinx.coroutines.flow.Flow

interface TaskRepository {
    fun observeTasks(): Flow<List<Task>>
    suspend fun fetchTasks(patientId: String?): ApiResult<List<Task>>
    suspend fun fetchTaskById(taskId: String): ApiResult<Task>
    suspend fun createTask(patientId: String, source: String, remark: String?): ApiResult<Task>
    suspend fun closeTask(taskId: String, request: CloseTaskRequest): ApiResult<Unit>
    suspend fun getLatestTrajectory(taskId: String): ApiResult<List<TrajectoryPoint>>
    suspend fun pollEvents(taskId: String, afterEventId: String?): ApiResult<List<TaskEvent>>
    suspend fun getWsTicket(): ApiResult<String>
}
