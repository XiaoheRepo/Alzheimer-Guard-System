package com.xiaohelab.guard.android.di

import com.xiaohelab.guard.android.BuildConfig
import com.xiaohelab.guard.android.core.network.AuthInterceptor
import com.xiaohelab.guard.android.core.network.AuthRefreshAuthenticator
import com.xiaohelab.guard.android.core.network.RequestIdInterceptor
import com.xiaohelab.guard.android.core.network.ReservedHeaderGuardInterceptor
import com.xiaohelab.guard.android.core.network.TraceIdInterceptor
import com.xiaohelab.guard.android.core.config.RemoteConfigApi
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
    }

    @Provides
    @Singleton
    fun provideOkHttp(
        reservedGuard: ReservedHeaderGuardInterceptor,
        requestId: RequestIdInterceptor,
        traceId: TraceIdInterceptor,
        auth: AuthInterceptor,
        authenticator: AuthRefreshAuthenticator,
    ): OkHttpClient {
        // Order matters:
        // 1. ReservedHeaderGuard runs first to fail-fast on HC-08 violations.
        // 2. Auth / Trace / RequestId follow.
        // 3. Logging only in DEBUG_TOOLS_ENABLED flavors (dev / staging).
        val builder = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(reservedGuard)
            .addInterceptor(auth)
            .addInterceptor(traceId)
            .addInterceptor(requestId)
            .authenticator(authenticator)
        if (BuildConfig.DEBUG_TOOLS_ENABLED) {
            builder.addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BASIC
                    // Sensitive headers are redacted; body logging deliberately kept off (HC-07).
                    redactHeader("Authorization")
                    redactHeader("X-Anonymous-Token")
                }
            )
        }
        return builder.build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient, json: Json): Retrofit {
        val contentType = "application/json; charset=utf-8".toMediaType()
        return Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    @Provides
    @Singleton
    fun provideRemoteConfigApi(retrofit: Retrofit): RemoteConfigApi =
        retrofit.create(RemoteConfigApi::class.java)

    /** M7: SSE 流式需要 API base URL。提供 named String 供 AiChatViewModel 使用。 */
    @Provides
    @Singleton
    @javax.inject.Named("api_base_url")
    fun provideApiBaseUrl(): String = BuildConfig.API_BASE_URL
}
