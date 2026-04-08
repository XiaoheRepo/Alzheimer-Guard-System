package com.xiaohelab.guard.android.core.network

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.xiaohelab.guard.android.BuildConfig
import com.xiaohelab.guard.android.core.common.ApiResult
import com.xiaohelab.guard.android.core.common.ErrorCode
import com.xiaohelab.guard.android.core.datastore.TokenManager
import com.xiaohelab.guard.android.core.network.dto.ApiResponseDto
import com.xiaohelab.guard.android.core.network.interceptor.GuardHeaderInterceptor
import com.xiaohelab.guard.android.core.network.interceptor.RetryInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

val json = Json {
    ignoreUnknownKeys = true   // 后端新增字段不崩溃
    isLenient = true
    coerceInputValues = true   // 枚举兜底
}

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(tokenManager: TokenManager): OkHttpClient {
        val logging = HttpLoggingInterceptor { message -> Timber.tag("HTTP").d(message) }.apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
                    else HttpLoggingInterceptor.Level.NONE
        }

        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(
                GuardHeaderInterceptor(
                    tokenProvider = { tokenManager.getAccessToken() },
                    anonymousTokenProvider = { tokenManager.getAnonymousToken() }
                )
            )
            .addInterceptor(RetryInterceptor())
            .addInterceptor(logging)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }
}

/**
 * 统一 API 调用封装：将 Retrofit suspend Response<ApiResponseDto<T>> 转换为 ApiResult<T>
 * - 非 2xx -> Failure（携带 HTTP 状态码）
 * - 2xx 但 code != OK -> Failure（携带业务码）
 * - 2xx 且 code == OK -> Success
 */
suspend fun <T> safeApiCall(
    call: suspend () -> Response<ApiResponseDto<T>>
): ApiResult<T> {
    return try {
        val response = call()
        val body = response.body()

        if (!response.isSuccessful || body == null) {
            // 尝试解析错误体
            val errorBody = response.errorBody()?.string()
            val retryAfter = response.headers()["Retry-After"]?.toIntOrNull()
            val traceId = response.headers()["X-Trace-Id"]

            // 简单尝试从错误体提取 code
            val errorCode = if (errorBody != null) {
                try {
                    json.decodeFromString<ApiResponseDto<Unit>>(errorBody).code
                } catch (e: Exception) {
                    "HTTP_${response.code()}"
                }
            } else {
                "HTTP_${response.code()}"
            }
            Timber.e("API error: code=$errorCode traceId=$traceId")
            ApiResult.Failure(
                code = errorCode,
                message = errorBody ?: "请求失败",
                traceId = traceId,
                retryAfterSeconds = retryAfter
            )
        } else if (!body.isSuccess) {
            Timber.e("Business error: code=${body.code} traceId=${body.trace_id}")
            val retryAfter = response.headers()["Retry-After"]?.toIntOrNull()
            ApiResult.Failure(
                code = body.code,
                message = body.message,
                traceId = body.trace_id,
                retryAfterSeconds = retryAfter
            )
        } else {
            @Suppress("UNCHECKED_CAST")
            ApiResult.Success(body.data as T)
        }
    } catch (e: Exception) {
        Timber.e(e, "Network exception")
        ApiResult.Failure(
            code = "NETWORK_ERROR",
            message = e.message ?: "网络连接失败，请检查网络设置"
        )
    }
}
