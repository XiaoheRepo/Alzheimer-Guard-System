package com.xiaohelab.guard.android.feature.tag.data

import com.xiaohelab.guard.android.core.network.ApiEnvelope
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

/**
 * M3-A 标签域 API。
 * HC-ID-String: tag_code / patient_id 均为 String。
 * HC-02: state 由服务端维护，客户端不推算。
 * 错误码: E_TAG_4041 / E_TAG_4091 / E_TAG_4031。
 */
interface TagApi {
    @GET("/api/v1/patients/{patient_id}/tags")
    suspend fun listTags(
        @Path("patient_id") patientId: String,
    ): Response<ApiEnvelope<TagListDto>>

    @POST("/api/v1/patients/{patient_id}/tags")
    suspend fun bindTag(
        @Path("patient_id") patientId: String,
        @Body body: TagBindRequest,
    ): Response<ApiEnvelope<TagDto>>

    @PUT("/api/v1/patients/{patient_id}/tags/{tag_code}")
    suspend fun updateTag(
        @Path("patient_id") patientId: String,
        @Path("tag_code") tagCode: String,
        @Body body: TagUpdateRequest,
    ): Response<ApiEnvelope<TagDto>>

    @DELETE("/api/v1/patients/{patient_id}/tags/{tag_code}")
    suspend fun unbindTag(
        @Path("patient_id") patientId: String,
        @Path("tag_code") tagCode: String,
    ): Response<ApiEnvelope<Unit>>
}

@Serializable
data class TagListDto(val items: List<TagDto> = emptyList())

/** HC-ID-String: tag_code / patient_id 为 String。HC-02: state 只读展示。 */
@Serializable
data class TagDto(
    @SerialName("tag_code") val tagCode: String,
    @SerialName("device_type") val deviceType: String,
    val alias: String? = null,
    /** HC-02: 状态由服务端维护，客户端不得自行推算。 */
    val state: String,
    @SerialName("bound_at") val boundAt: String? = null,
    @SerialName("patient_id") val patientId: String,
)

@Serializable
data class TagBindRequest(
    @SerialName("tag_code") val tagCode: String,
    @SerialName("device_type") val deviceType: String,
    val alias: String? = null,
)

@Serializable
data class TagUpdateRequest(
    val alias: String? = null,
)
