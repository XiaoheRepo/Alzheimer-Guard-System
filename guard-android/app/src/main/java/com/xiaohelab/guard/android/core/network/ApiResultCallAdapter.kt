package com.xiaohelab.guard.android.core.network

import com.xiaohelab.guard.android.core.common.DomainException
import com.xiaohelab.guard.android.core.common.MhResult
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import retrofit2.Response
import timber.log.Timber
import java.io.IOException

/**
 * Shared lenient parser for error-body decoding only.
 * The main Retrofit Json instance is injected via Hilt; this one is used locally
 * to parse HTTP 4xx/5xx error bodies before the DTO layer can act on them.
 */
@PublishedApi
internal val errorBodyParser = Json { ignoreUnknownKeys = true; isLenient = true }

/**
 * Applies handbook §2.3 六条规则 on a Retrofit [Response] that wraps [ApiEnvelope].
 *
 * NOTE: servers that follow REST conventions return 4xx for business errors.
 * Retrofit places non-2xx response bodies in [Response.errorBody], NOT [Response.body].
 * This helper handles both paths.
 *
 * Rule map:
 *   1. 2xx + code=="ok"         → Success
 *   2. 2xx + code startsWith E_ → Failure(DomainException)
 *   3. 4xx business error       → Failure parsed from errorBody
 *   4. 403                      → Failure; caller must NOT auto-retry.
 *   5. 429                      → Failure with retryAfterSeconds (Retry-After header).
 *   6. Network/IO               → Failure(E_NET_LOCAL) via runCatching.
 */
inline fun <T> handleEnvelope(block: () -> Response<ApiEnvelope<T>>): MhResult<T> {
    return try {
        val raw = block()
        val resp: okhttp3.Response = raw.raw()
        val traceId = resp.header(TraceIdInterceptor.HEADER)
        val requestId = resp.request.header(RequestIdInterceptor.HEADER)

        val envelope = raw.body()
        if (envelope == null) {
            // Non-2xx: try to extract the business error code from the error body JSON.
            val errorJson = try { raw.errorBody()?.string() } catch (_: Exception) { null }
            val errEnvelope: ApiEnvelope<JsonElement>? = if (!errorJson.isNullOrBlank()) {
                try { errorBodyParser.decodeFromString(errorJson) } catch (_: Exception) { null }
            } else null

            val tid = errEnvelope?.traceId ?: traceId
            val code = errEnvelope?.code?.ifBlank { null } ?: DomainException.CODE_PROTOCOL
            val msg = errEnvelope?.message ?: "Empty envelope (http=${resp.code})"

            return if (resp.code == 429) {
                val retry = BackoffPolicy.parseRetryAfterSeconds(resp.header("Retry-After"))
                MhResult.Failure(DomainException(
                    code = if (code.startsWith("E_")) code else DomainException.CODE_RATE_LIMITED,
                    message = msg, traceId = tid, requestId = requestId,
                    httpStatus = resp.code, retryAfterSeconds = retry,
                ))
            } else {
                MhResult.Failure(DomainException(
                    code = code, message = msg, traceId = tid, requestId = requestId,
                    httpStatus = resp.code,
                ))
            }
        }

        val traceId2 = envelope.traceId ?: traceId
        Timber.tag("GUARD/NET").d("http=%d code=%s", resp.code, envelope.code)
        when {
            envelope.isOk -> {
                @Suppress("UNCHECKED_CAST")
                val payload = envelope.data ?: (Unit as T)
                MhResult.Success(payload, traceId2)
            }
            resp.code == 429 -> {
                val retry = BackoffPolicy.parseRetryAfterSeconds(resp.header("Retry-After"))
                MhResult.Failure(DomainException(
                    code = if (envelope.code.startsWith("E_")) envelope.code else DomainException.CODE_RATE_LIMITED,
                    message = envelope.message, traceId = traceId2, requestId = requestId,
                    httpStatus = 429, retryAfterSeconds = retry,
                ))
            }
            else -> MhResult.Failure(DomainException(
                code = envelope.code.ifBlank { DomainException.CODE_PROTOCOL },
                message = envelope.message, traceId = traceId2, requestId = requestId,
                httpStatus = resp.code,
            ))
        }
    } catch (io: IOException) {
        Timber.tag("GUARD/NET").e(io, "handleEnvelope IOException")
        MhResult.Failure(DomainException(
            code = DomainException.CODE_NETWORK, message = io.message, cause = io,
        ))
    } catch (t: Throwable) {
        Timber.tag("GUARD/NET").e(t, "handleEnvelope unexpected [${t.javaClass.simpleName}]: ${t.message}")
        MhResult.Failure(DomainException(
            code = DomainException.CODE_PROTOCOL, message = "${t.javaClass.simpleName}: ${t.message}", cause = t,
        ))
    }
}

/**
 * Convenience for endpoints whose `data` is `null` on success (e.g. password-reset/request).
 */
inline fun handleEnvelopeUnit(block: () -> Response<ApiEnvelope<JsonElement>>): MhResult<Unit> =
    when (val r = handleEnvelope(block)) {
        is MhResult.Success -> MhResult.Success(Unit, r.trace)
        is MhResult.Failure -> r
    }

