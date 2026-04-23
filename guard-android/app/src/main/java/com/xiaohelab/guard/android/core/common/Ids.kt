package com.xiaohelab.guard.android.core.common

import java.time.Instant
import java.util.UUID

/**
 * ID helpers. HC-ID-String: every ID stays as [String] from DTO to UI.
 */
object Ids {
    /** Generate an X-Request-Id (§2.2; length 16-64, charset [A-Za-z0-9-]). */
    fun newRequestId(): String = "req-" + UUID.randomUUID().toString().replace("-", "")

    /** Generate an X-Trace-Id. Keep format symmetrical with backend (§2.2). */
    fun newTraceId(): String = "trc-" + UUID.randomUUID().toString().replace("-", "")

    private val HEADER_ID = Regex("^[A-Za-z0-9-]{16,64}$")
    fun isValidHeaderId(value: String): Boolean = HEADER_ID.matches(value)
}

/** Injectable clock seam for deterministic tests. */
fun interface Clock {
    fun now(): Instant

    companion object {
        val System: Clock = Clock { Instant.now() }
    }
}
