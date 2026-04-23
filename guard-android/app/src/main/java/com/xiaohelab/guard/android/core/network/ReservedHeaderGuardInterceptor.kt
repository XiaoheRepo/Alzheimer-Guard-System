package com.xiaohelab.guard.android.core.network

import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * HC-08 / §10.3.5: 客户端**严禁**主动发送 X-User-Id / X-User-Role；出现即 fail-fast，
 * 避免误提交到网关。该拦截器放在拦截器链最上方，重试时同样生效。
 */
@Singleton
class ReservedHeaderGuardInterceptor @Inject constructor() : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        FORBIDDEN.forEach { h ->
            check(request.header(h) == null) { "Forbidden reserved header: $h (HC-08)" }
        }
        return chain.proceed(request)
    }

    companion object {
        val FORBIDDEN = setOf("X-User-Id", "X-User-Role")
    }
}
