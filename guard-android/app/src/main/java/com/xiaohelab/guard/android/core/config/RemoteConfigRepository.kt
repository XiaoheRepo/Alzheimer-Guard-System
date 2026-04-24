package com.xiaohelab.guard.android.core.config

import com.xiaohelab.guard.android.core.common.MhResult
import com.xiaohelab.guard.android.core.network.ApiEnvelope
import com.xiaohelab.guard.android.core.network.handleEnvelope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query
import javax.inject.Inject
import javax.inject.Singleton

/**
 * HC-05: 禁止硬编码阈值。全部配置项在应用首启从 `/api/v1/admin/configs` 拉取。
 * 网络异常或首启前，读取方法 fallback 到 [DefaultRemoteConfig]（在 `admin/configs` 接口可用前兜底）。
 *
 * 注：家属端只读取服务端暴露给 FAMILY 的 key（API §5）。未知 key 返回默认值。
 */
@Singleton
class RemoteConfigRepository @Inject constructor(
    private val api: RemoteConfigApi,
) {
    private val _snapshot = MutableStateFlow<Map<String, String>>(emptyMap())
    val snapshot: StateFlow<Map<String, String>> = _snapshot.asStateFlow()

    suspend fun refresh(): MhResult<Unit> {
        val result = handleEnvelope { api.configs() }
        if (result is MhResult.Success) {
            _snapshot.value = result.data.items.associate { it.configKey to it.configValue }
        }
        return when (result) {
            is MhResult.Success -> MhResult.Success(Unit, result.trace)
            is MhResult.Failure -> result
        }
    }

    fun getString(key: String): String = _snapshot.value[key] ?: DefaultRemoteConfig.defaults[key].orEmpty()
    fun getInt(key: String): Int = getString(key).toIntOrNull() ?: (DefaultRemoteConfig.defaults[key]?.toIntOrNull() ?: 0)
    fun getLong(key: String): Long = getString(key).toLongOrNull() ?: (DefaultRemoteConfig.defaults[key]?.toLongOrNull() ?: 0L)
    fun getBoolean(key: String): Boolean = when (getString(key).lowercase()) {
        "true", "1", "yes" -> true
        else -> false
    }
}

/** 兜底默认值：仅用于接口不可达时避免空指针。正式阈值以服务端为准（HC-05）。 */
object DefaultRemoteConfig {
    const val KEY_TASK_SUSTAINED_DAYS = "task_sustained_days"
    const val KEY_MIN_CLIENT_VERSION = "min_client_version_android"
    const val KEY_NOTIFICATION_THROTTLE_SECONDS = "notification.throttle.interval_seconds"
    const val KEY_SESSION_TTL_SECONDS = "session_ttl_seconds"
    const val KEY_PASSWORD_RESET_LINK_TTL_MINUTES = "password_reset_link_ttl_minutes"
    const val KEY_AI_QUOTA_USER_MONTHLY = "ai.quota.user.monthly_limit"
    const val KEY_AI_QUOTA_PATIENT_MONTHLY = "ai.quota.patient.monthly_limit"
    const val KEY_TAG_BINDING_REQUIRES_OTP = "tag_binding_requires_otp"
    const val KEY_FENCE_RADIUS_MAX_M = "fence.radius.max_m"
    const val KEY_FENCE_RADIUS_MIN_M = "fence.radius.min_m"
    /** M4: 速度阈值（km/h），由服务端下发，HC-05 不得硬编码。 */
    const val KEY_SPEED_THRESHOLD_KMH = "speed_threshold_kmh"
    /** M6: 通知轮询间隔（秒），WorkManager 用此值设定 PeriodicWorkRequest 周期，HC-05。 */
    const val KEY_NOTIFICATION_POLL_INTERVAL_SEC = "notification.poll.interval_seconds"

    val defaults: Map<String, String> = mapOf(
        KEY_TASK_SUSTAINED_DAYS to "7",
        KEY_NOTIFICATION_THROTTLE_SECONDS to "30",
        KEY_SESSION_TTL_SECONDS to "3600",
        KEY_PASSWORD_RESET_LINK_TTL_MINUTES to "30",
        KEY_AI_QUOTA_USER_MONTHLY to "100000",
        KEY_AI_QUOTA_PATIENT_MONTHLY to "50000",
        KEY_TAG_BINDING_REQUIRES_OTP to "false",
        KEY_FENCE_RADIUS_MAX_M to "50000",
        KEY_FENCE_RADIUS_MIN_M to "100",
        KEY_SPEED_THRESHOLD_KMH to "5",
        KEY_NOTIFICATION_POLL_INTERVAL_SEC to "60",
    )
}

interface RemoteConfigApi {
    @GET("/api/v1/admin/configs")
    suspend fun configs(@Query("scope") scope: String? = null): Response<ApiEnvelope<ConfigListDto>>
}

@Serializable
data class ConfigListDto(val items: List<ConfigItemDto> = emptyList())

@Serializable
data class ConfigItemDto(
    @SerialName("config_key") val configKey: String,
    @SerialName("config_value") val configValue: String,
    val scope: String? = null,
    val description: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)
