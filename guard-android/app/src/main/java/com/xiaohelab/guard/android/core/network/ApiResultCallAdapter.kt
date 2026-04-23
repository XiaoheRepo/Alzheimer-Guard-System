package com.xiaohelab.guard.android.core.network

import com.xiaohelab.guard.android.core.common.DomainException
import com.xiaohelab.guard.android.core.common.MhResult
import kotlinx.serialization.json.JsonElement
import retrofit2.Response
import java.io.IOException

/**
 * Applies handbook §2.3 六条规则 on a Retrofit [Response] that wraps [ApiEnvelope].
 *
 * API interfaces in this project declare `suspend fun foo(...): Response<ApiEnvelope<Foo>>`
 * so that this helper can inspect HTTP status + headers (needed for 429 `Retry-After`).
 *
 * Rule map:
 *   1. 2xx + code=="ok"         → Success
 *   2. 2xx + code startsWith E_ → Failure(DomainException)
 *   3. 401 + E_AUTH_4011/E_GOV_4011 → Failure — caller (AuthRepository) will trigger refresh or logout.
 *   4. 403                      → Failure; caller must NOT auto-retry.
 *   5. 429                      → Failure with retryAfterSeconds (Retry-After header).
 *   6. Network/IO               → Failure(E_NET_LOCAL) via runCatching.
 */
inline fun <T> handleEnvelope(block: () -> Response<ApiEnvelope<T>>): MhResult<T> = try {
    val raw = block()
    val resp: okhttp3.Response = raw.raw()
    val envelope = raw.body() ?: run {
        // Business error with non-2xx could still carry JSON error body.
        raw.errorBody()?.string()?.let { /* left to caller; keep simple here */ }
        return MhResult.Failure(DomainException(
            code = DomainException.CODE_PROTOCOL,
            message = "Empty envelope (http=${resp.code})",
            traceId = resp.header(TraceIdInterceptor.HEADER),
            requestId = resp.request.header(RequestIdInterceptor.HEADER),
            httpStatus = resp.code,
        ))
    }
    val traceId = envelope.traceId ?: resp.header(TraceIdInterceptor.HEADER)
    val requestId = resp.request.header(RequestIdInterceptor.HEADER)

    when {
        envelope.isOk -> {
            @Suppress("UNCHECKED_CAST")
            val payload = envelope.data ?: (Unit as T)
            MhResult.Success(payload, traceId)
        }
        resp.code == 429 -> {
            val retry = BackoffPolicy.parseRetryAfterSeconds(resp.header("Retry-After"))
            MhResult.Failure(DomainException(
                code = if (envelope.code.startsWith("E_")) envelope.code else DomainException.CODE_RATE_LIMITED,
                message = envelope.message, traceId = traceId, requestId = requestId,
                httpStatus = 429, retryAfterSeconds = retry,
            ))
        }
        else -> MhResult.Failure(DomainException(
            code = envelope.code.ifBlank { DomainException.CODE_PROTOCOL },
            message = envelope.message, traceId = traceId, requestId = requestId,
            httpStatus = resp.code,
        ))
    }
} catch (io: IOException) {
    MhResult.Failure(DomainException(
        code = DomainException.CODE_NETWORK, message = io.message, cause = io,
    ))
} catch (t: Throwable) {
    MhResult.Failure(DomainException(
        code = DomainException.CODE_PROTOCOL, message = t.message, cause = t,
    ))
}

/**
 * Convenience for endpoints whose `data` is `null` on success (e.g. password-reset/request).
 * Discards the payload even if server returns something.
 */
inline fun handleEnvelopeUnit(block: () -> Response<ApiEnvelope<JsonElement>>): MhResult<Unit> =
    when (val r = handleEnvelope(block)) {
        is MhResult.Success -> MhResult.Success(Unit, r.trace)
        is MhResult.Failure -> r
    }

