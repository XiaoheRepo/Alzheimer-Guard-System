package com.xiaohelab.guard.android.core.network

import com.xiaohelab.guard.android.core.auth.AuthTokenStore
import com.xiaohelab.guard.android.core.eventbus.AppEvent
import com.xiaohelab.guard.android.core.eventbus.AppEventBus
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

/**
 * HC-Check #5 / §2.3 rule 3: 401 → single-shot `/auth/token/refresh`.
 *   - Success: re-sign the original request and resume.
 *   - Failure: clear session, emit [AppEvent.SessionInvalidated], return null so the call fails as-is.
 *
 * Uses a [Mutex] so concurrent 401s trigger only ONE refresh call.
 * The refresher is a [Provider] to break the DI cycle between OkHttp and the retrofit service.
 */
@Singleton
class AuthRefreshAuthenticator @Inject constructor(
    private val tokenStore: AuthTokenStore,
    private val refresher: Provider<TokenRefresher>,
    private val eventBus: AppEventBus,
) : Authenticator {

    private val mutex = Mutex()

    override fun authenticate(route: Route?, response: Response): Request? {
        // Don't retry forever.
        if (responseCount(response) >= 2) return null
        val original = response.request

        // /auth/** endpoints MUST NOT be re-challenged here (avoid recursion).
        val path = original.url.encodedPath
        if (path.startsWith("/api/v1/auth/")) return null

        val newAccess = runBlocking {
            mutex.withLock {
                // Double-check: another thread may have refreshed already.
                val current = tokenStore.accessToken()
                val originalAuth = original.header("Authorization")
                if (current != null && originalAuth != "Bearer $current") {
                    return@withLock current
                }
                val refreshed = refresher.get().refreshOnce()
                if (refreshed == null) {
                    tokenStore.clear()
                    eventBus.tryEmit(AppEvent.SessionInvalidated)
                }
                refreshed
            }
        } ?: return null

        return original.newBuilder()
            .header("Authorization", "Bearer $newAccess")
            .build()
    }

    private fun responseCount(response: Response): Int {
        var r: Response? = response
        var count = 1
        while (r?.priorResponse != null) { count++; r = r.priorResponse }
        return count
    }
}

/** Abstract hook implemented by AuthRepository (data/auth). */
interface TokenRefresher {
    /** Returns a fresh access token, or null if refresh failed (-> logout). */
    suspend fun refreshOnce(): String?
}
