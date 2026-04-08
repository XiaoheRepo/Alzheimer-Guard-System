package com.xiaohelab.guard.android.core.network.interceptor

import okhttp3.Interceptor
import okhttp3.Response
import timber.log.Timber
import kotlin.math.min
import kotlin.math.pow
import kotlin.random.Random

/**
 * 限流与退避拦截器（HandBook §6.4 + API §1.8）：
 * - 收到 429 时优先读 Retry-After 头
 * - 无 Retry-After 则指数退避 + 抖动：delay = min(base * 2^attempt + jitter, maxDelay)
 * - E_REQ_4003 不重试（客户端伪造保留 Header）
 */
class RetryInterceptor(
    private val maxRetries: Int = 3,
    private val baseDelayMs: Long = 1000L,
    private val maxDelayMs: Long = 30_000L
) : Interceptor {

    companion object {
        private const val TAG = "RetryInterceptor"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        var response = chain.proceed(request)
        var attempt = 0

        while (response.code == 429 && attempt < maxRetries) {
            val retryAfterSec = response.header("Retry-After")?.toLongOrNull()
            val delayMs = if (retryAfterSec != null) {
                retryAfterSec * 1000L
            } else {
                val jitter = Random.nextLong(0, 500)
                min(baseDelayMs * 2.0.pow(attempt.toDouble()).toLong() + jitter, maxDelayMs)
            }

            Timber.tag(TAG).w("429 received, retrying after ${delayMs}ms (attempt ${attempt + 1}/$maxRetries)")
            response.close()
            Thread.sleep(delayMs)
            response = chain.proceed(request)
            attempt++
        }

        return response
    }
}
