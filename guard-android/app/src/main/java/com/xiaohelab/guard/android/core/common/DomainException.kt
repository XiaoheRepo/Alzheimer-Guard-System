package com.xiaohelab.guard.android.core.common

/**
 * Exception raised when the server returns `code` starting with `E_` or when transport fails.
 *
 * HC-Check references:
 *  - HC-03: [requestId] is the X-Request-Id used for the failed call (may be null for GET).
 *  - HC-04: [traceId] is the X-Trace-Id echoed by the server; first 8 chars are surfaced to the user.
 *  - §2.3: [code] drives the UI mapping in `core/ui/ErrorMessageMapper`.
 */
class DomainException(
    val code: String,
    override val message: String?,
    val traceId: String? = null,
    val requestId: String? = null,
    val httpStatus: Int = 0,
    val retryAfterSeconds: Int? = null,
    cause: Throwable? = null,
) : RuntimeException(message, cause) {

    val shortTrace: String? get() = traceId?.take(8)

    companion object {
        /** Transport or I/O failure before we ever parsed an envelope. */
        const val CODE_NETWORK = "E_NET_LOCAL"

        /** Envelope malformed / unknown response shape. */
        const val CODE_PROTOCOL = "E_PROTO_LOCAL"

        /** HC-08: client tried to send a reserved header. Should never reach the wire. */
        const val CODE_RESERVED_HEADER = "E_REQ_4003"

        const val CODE_UNAUTHORIZED = "E_GOV_4011"
        const val CODE_AUTH_BAD_CREDENTIAL = "E_AUTH_4011"
        const val CODE_RATE_LIMITED = "E_GOV_4291"
        const val CODE_COOLDOWN = "E_GOV_4292"
        const val CODE_FORBIDDEN = "E_GOV_4030"
        const val CODE_BANNED = "E_GOV_4031"
        const val CODE_VERSION_CONFLICT = "E_PRO_4091"
    }
}
