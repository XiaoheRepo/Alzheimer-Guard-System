package com.xiaohelab.guard.android.core.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Unified response envelope (API §1.4).
 *
 *  - [code]     = "ok" for success or "E_*" for failure.
 *  - [traceId]  = X-Trace-Id echoed back (HC-04). MUST be present.
 *  - [data]     = business payload, JSON null on failure.
 *  - [state]    = optional WS/BFF state snapshot (HC-02 服务端权威).
 *  - [notification] = optional push/toast payload.
 *  - [version]  = event anti-reordering anchor.
 */
@Serializable
data class ApiEnvelope<T>(
    val code: String,
    val message: String? = null,
    @SerialName("trace_id") val traceId: String? = null,
    val data: T? = null,
    val state: JsonElement? = null,
    val notification: JsonElement? = null,
    val version: Long? = null,
) {
    val isOk: Boolean get() = code == "ok"
}
