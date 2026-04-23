package com.xiaohelab.guard.android.core.network

import kotlin.math.min
import kotlin.math.pow
import kotlin.random.Random

/**
 * §10.4 限流与退避：
 *  - 429 + `Retry-After` → 固定秒数；
 *  - 429 无 Retry-After → `min(base * 2^n + random(0..jitterMs), capMs)`；
 *  - 5xx（幂等读）→ 最多自动重试 2 次，首次 1s，次次 2s；写不自动重试。
 */
object BackoffPolicy {
    const val BASE_MS = 500L
    const val CAP_MS = 30_000L
    const val JITTER_MS = 300L

    /** Returns delay millis for the [attempt]-th retry (0-based). */
    fun exponentialWithJitter(
        attempt: Int,
        baseMs: Long = BASE_MS,
        capMs: Long = CAP_MS,
        jitterMs: Long = JITTER_MS,
        random: Random = Random.Default,
    ): Long {
        require(attempt >= 0)
        val exp = (baseMs.toDouble() * 2.0.pow(attempt.toDouble())).toLong()
        val jitter = if (jitterMs > 0) random.nextLong(0, jitterMs + 1) else 0L
        return min(exp + jitter, capMs)
    }

    /**
     * Parse `Retry-After`. Supports seconds (integer) per §10.4; HTTP-date variant is
     * intentionally not supported server-side.
     */
    fun parseRetryAfterSeconds(header: String?): Int? =
        header?.trim()?.toIntOrNull()?.takeIf { it >= 0 }
}
