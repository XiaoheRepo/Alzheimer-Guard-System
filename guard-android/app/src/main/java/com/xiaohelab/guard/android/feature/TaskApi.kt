package com.xiaohelab.guard.android.feature.task.data

import com.xiaohelab.guard.android.core.network.ApiEnvelope
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * M5-A 任务域 API。
 * HC-ID-String: task_id / patient_id 均为 String。
 * HC-02: status 由服务端维护，客户端不推算。
 * HC-Coord: last_seen_location 使用 GCJ-02，上报附 coord_system。
 * 错误码: E_TASK_4041 / E_TASK_4091 / E_TASK_4031 / E_TASK_4221。
 */
interface RescueTaskApi {
    @GET("/api/v1/rescue-tasks")
    suspend fun listTasks(
        @Query("patient_id") patientId: String? = null,
        @Query("status") status: String? = null,
    ): Response<ApiEnvelope<RescueTaskListDto>>

    @POST("/api/v1/rescue-tasks")
    suspend fun createTask(
        @Body body: CreateTaskRequest,
    ): Response<ApiEnvelope<RescueTaskDto>>

    @GET("/api/v1/rescue-tasks/{task_id}")
    suspend fun getTask(
        @Path("task_id") taskId: String,
    ): Response<ApiEnvelope<RescueTaskDto>>

    @POST("/api/v1/rescue-tasks/{task_id}/cancel")
    suspend fun cancelTask(
        @Path("task_id") taskId: String,
    ): Response<ApiEnvelope<Unit>>
}

@Serializable
data class RescueTaskListDto(val items: List<RescueTaskDto> = emptyList())

/** HC-ID-String: task_id / patient_id 为 String。HC-02: status 只读展示。 */
@Serializable
data class RescueTaskDto(
    @SerialName("task_id") val taskId: String,
    @SerialName("patient_id") val patientId: String,
    /** HC-02: 状态由服务端 state.changed 事件维护，客户端不推算。 */
    val status: String,
    val description: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("last_seen_location") val lastSeenLocation: LocationDto? = null,
)

/**
 * HC-Coord: 坐标系统一 GCJ-02（高德原始坐标）上报，附 coord_system 字段。
 * 服务端返回 WGS84；展示时才做反向转换。
 */
@Serializable
data class LocationDto(
    val lat: Double,
    val lng: Double,
    val address: String? = null,
    /** HC-Coord: 上报时应为 "GCJ-02"，服务端返回时可能为 "WGS84"。 */
    @SerialName("coord_system") val coordSystem: String? = null,
)

@Serializable
data class CreateTaskRequest(
    @SerialName("patient_id") val patientId: String,
    val description: String? = null,
    /** HC-Coord: coord_system = "GCJ-02" */
    @SerialName("last_seen_location") val lastSeenLocation: LocationDto? = null,
)
