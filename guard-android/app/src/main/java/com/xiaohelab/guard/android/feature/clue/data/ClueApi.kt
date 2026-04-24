package com.xiaohelab.guard.android.feature.clue.data

import com.xiaohelab.guard.android.core.network.ApiEnvelope
import com.xiaohelab.guard.android.feature.task.data.LocationDto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * M5-B 线索域 API。
 * HC-ID-String: clue_id / task_id 均为 String。
 * HC-02: review_state 由服务端维护，客户端只展示。
 * 错误码: E_CLUE_4041 / E_CLUE_4031。
 */
interface ClueApi {
    @GET("/api/v1/rescue-tasks/{task_id}/clues")
    suspend fun listClues(
        @Path("task_id") taskId: String,
    ): Response<ApiEnvelope<ClueListDto>>

    @POST("/api/v1/rescue-tasks/{task_id}/clues")
    suspend fun createClue(
        @Path("task_id") taskId: String,
        @Body body: CreateClueRequest,
    ): Response<ApiEnvelope<ClueDto>>

    @GET("/api/v1/rescue-tasks/{task_id}/clues/{clue_id}")
    suspend fun getClue(
        @Path("task_id") taskId: String,
        @Path("clue_id") clueId: String,
    ): Response<ApiEnvelope<ClueDto>>
}

@Serializable
data class ClueListDto(val items: List<ClueDto> = emptyList())

/** HC-ID-String: clue_id / task_id 为 String。HC-02: review_state 只读展示。 */
@Serializable
data class ClueDto(
    @SerialName("clue_id") val clueId: String,
    @SerialName("task_id") val taskId: String,
    val type: String,
    val content: String,
    /** HC-02: 审核状态由服务端维护，不得客户端推算。 */
    @SerialName("review_state") val reviewState: String,
    val location: LocationDto? = null,
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class CreateClueRequest(
    val type: String,
    val content: String,
    /** HC-Coord: 如提供坐标，附 coord_system = "GCJ-02"。 */
    val location: LocationDto? = null,
)
