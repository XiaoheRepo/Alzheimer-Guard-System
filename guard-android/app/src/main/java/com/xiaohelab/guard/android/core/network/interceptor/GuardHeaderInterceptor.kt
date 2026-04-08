package com.xiaohelab.guard.android.core.network.interceptor

import com.xiaohelab.guard.android.core.common.generateRequestId
import com.xiaohelab.guard.android.core.common.generateTraceId
import okhttp3.Interceptor
import okhttp3.Response

/**
 * 全局 Header 拦截器（HandBook §6.2）：
 * - 所有请求注入 X-Trace-Id
 * - 写请求（POST/PUT/PATCH/DELETE）注入 X-Request-Id（同一请求重试时保持原值）
 * - 受保护接口注入 Authorization: Bearer {token}
 * - 匿名链路注入 X-Anonymous-Token（当 anonymousTokenProvider 返回非 null 时）
 */
class GuardHeaderInterceptor(
    private val tokenProvider: () -> String?,
    private val anonymousTokenProvider: () -> String?
) : Interceptor {

    private val writeMethods = setOf("POST", "PUT", "PATCH", "DELETE")

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val method = original.method.uppercase()
        val traceId = generateTraceId()

        val builder = original.newBuilder()
            .header("X-Trace-Id", traceId)
            .header("Content-Type", "application/json")

        tokenProvider()?.let { builder.header("Authorization", "Bearer $it") }

        // 写接口幂等键——如果已有则保留（重试场景保持同值）
        if (method in writeMethods) {
            val existingRequestId = original.header("X-Request-Id")
            if (existingRequestId == null) {
                builder.header("X-Request-Id", generateRequestId())
            }
        }

        // 匿名链路凭据
        anonymousTokenProvider()?.let { builder.header("X-Anonymous-Token", it) }

        return chain.proceed(builder.build())
    }
}
