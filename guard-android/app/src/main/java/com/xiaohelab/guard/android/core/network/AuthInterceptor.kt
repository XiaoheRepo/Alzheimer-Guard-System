package com.xiaohelab.guard.android.core.network

import com.xiaohelab.guard.android.core.auth.AuthTokenStore
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Injects `Authorization: Bearer <access_token>` for protected endpoints.
 *
 * 401 refresh flow is handled by [ApiResultCallAdapterFactory] so we keep the
 * interceptor side-effect free (HC-Check #5).
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenStore: AuthTokenStore,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val path = request.url.encodedPath
        // Public / auth endpoints skip the injection (see handbook §10.3.3).
        if (path.startsWith("/api/v1/public") ||
            path.startsWith("/r/") ||
            path.startsWith("/api/v1/auth/")
        ) return chain.proceed(request)

        val token = tokenStore.accessToken()
        val out = if (token != null) {
            request.newBuilder().header("Authorization", "Bearer $token").build()
        } else request
        return chain.proceed(out)
    }
}
