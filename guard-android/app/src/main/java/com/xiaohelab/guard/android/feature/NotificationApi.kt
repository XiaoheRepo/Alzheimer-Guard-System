package com.xiaohelab.guard.android.feature.notification.data

import com.xiaohelab.guard.android.core.network.ApiEnvelope
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * M6 通知中心 API。
 * HC-ID-String: notification_id 为 String。
 * 错误码: E_NOTIF_4041。
 */
interface NotificationApi {
    @GET("/api/v1/notifications")
    suspend fun listNotifications(): Response<ApiEnvelope<NotificationListDto>>

    @POST("/api/v1/notifications/{notification_id}/read")
    suspend fun markRead(
        @Path("notification_id") notificationId: String,
    ): Response<ApiEnvelope<Unit>>

    @POST("/api/v1/notifications/read-all")
    suspend fun markAllRead(): Response<ApiEnvelope<Unit>>
}

@Serializable
data class NotificationListDto(
    val items: List<NotificationDto> = emptyList(),
    @SerialName("unread_count") val unreadCount: Int = 0,
    val total: Int = 0,
)

/** HC-ID-String: notification_id 为 String。 */
@Serializable
data class NotificationDto(
    @SerialName("notification_id") val notificationId: String,
    val type: String,
    val title: String,
    val body: String,
    val read: Boolean,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("deep_link") val deepLink: String? = null,
)
