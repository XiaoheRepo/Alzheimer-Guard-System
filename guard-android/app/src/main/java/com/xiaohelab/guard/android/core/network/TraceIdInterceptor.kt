package com.xiaohelab.guard.android.core.network

import com.xiaohelab.guard.android.core.common.Ids
import okhttp3.Interceptor
import okhttp3.Response
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * HC-04: every request carries `X-Trace-Id`. Upstream trace id is preserved for
 * cross-link propagation (e.g. push notification → detail page).
 */
@Singleton
class TraceIdInterceptor @Inject constructor() : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val traceId = request.header(HEADER) ?: Ids.newTraceId()
        val out = request.newBuilder().header(HEADER, traceId).build()
        val response = chain.proceed(out)
        val echoed = response.header(HEADER)
        Timber.tag("NET").d("trace=%s %s %d echoed=%s", traceId, request.url.encodedPath, response.code, echoed)
        return response
    }

    companion object { const val HEADER = "X-Trace-Id" }
}
