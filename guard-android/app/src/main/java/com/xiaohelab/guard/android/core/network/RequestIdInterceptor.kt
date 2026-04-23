package com.xiaohelab.guard.android.core.network

import com.xiaohelab.guard.android.core.common.Ids
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * HC-03: `X-Request-Id` for every write method. Retries MUST re-use the same id;
 * if the caller has already set the header (e.g. offline replay) we respect it.
 */
@Singleton
class RequestIdInterceptor @Inject constructor() : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (request.method !in WRITE_METHODS) return chain.proceed(request)

        val existing = request.header(HEADER)
        val rid = when {
            existing != null -> {
                require(Ids.isValidHeaderId(existing)) { "Illegal X-Request-Id: $existing" }
                existing
            }
            else -> Ids.newRequestId()
        }
        return chain.proceed(request.newBuilder().header(HEADER, rid).build())
    }

    companion object {
        const val HEADER = "X-Request-Id"
        val WRITE_METHODS = setOf("POST", "PUT", "PATCH", "DELETE")
    }
}
