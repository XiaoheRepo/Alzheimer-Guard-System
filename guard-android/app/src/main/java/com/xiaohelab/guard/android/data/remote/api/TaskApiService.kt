package com.xiaohelab.guard.android.data.remote.api

import com.xiaohelab.guard.android.core.network.dto.ApiResponseDto
import com.xiaohelab.guard.android.data.remote.dto.CloseTaskRequestDto
import com.xiaohelab.guard.android.data.remote.dto.CreateTaskRequestDto
import com.xiaohelab.guard.android.data.remote.dto.TaskDto
import com.xiaohelab.guard.android.data.remote.dto.TaskEventDto
import com.xiaohelab.guard.android.data.remote.dto.TrajectoryPointDto
import com.xiaohelab.guard.android.data.remote.dto.WsTicketResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface TaskApiService {
    @GET("api/v1/rescue/tasks")
    suspend fun getTasks(
        @Query("patient_id") patientId: String? = null,
        @Query("status") status: String? = null,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20
    ): Response<ApiResponseDto<List<TaskDto>>>

    @POST("api/v1/rescue/tasks")
    suspend fun createTask(@Body request: CreateTaskRequestDto): Response<ApiResponseDto<TaskDto>>

    @GET("api/v1/rescue/tasks/{id}/snapshot")
    suspend fun getTaskSnapshot(@Path("id") taskId: String): Response<ApiResponseDto<TaskDto>>

    @POST("api/v1/rescue/tasks/{id}/close")
    suspend fun closeTask(
        @Path("id") taskId: String,
        @Body request: CloseTaskRequestDto
    ): Response<ApiResponseDto<Unit>>

    @GET("api/v1/rescue/tasks/{id}/trajectory/latest")
    suspend fun getLatestTrajectory(
        @Path("id") taskId: String
    ): Response<ApiResponseDto<List<TrajectoryPointDto>>>

    @GET("api/v1/rescue/tasks/{id}/events/poll")
    suspend fun pollEvents(
        @Path("id") taskId: String,
        @Query("after_event_id") afterEventId: String? = null
    ): Response<ApiResponseDto<List<TaskEventDto>>>

    @POST("api/v1/ws/tickets")
    suspend fun getWsTicket(): Response<ApiResponseDto<WsTicketResponseDto>>
}
