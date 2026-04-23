package com.xiaohelab.guard.android.core.common

/**
 * Sealed result wrapper for all repository / use-case returns.
 * Aligns handbook §2.3 六条规则: success carries [trace_id], failure carries [DomainException].
 */
sealed interface MhResult<out T> {
    data class Success<T>(val data: T, val trace: String? = null) : MhResult<T>
    data class Failure(val error: DomainException) : MhResult<Nothing>
}

inline fun <T> MhResult<T>.onSuccess(block: (T) -> Unit): MhResult<T> {
    if (this is MhResult.Success) block(data)
    return this
}

inline fun <T> MhResult<T>.onFailure(block: (DomainException) -> Unit): MhResult<T> {
    if (this is MhResult.Failure) block(error)
    return this
}

inline fun <T, R> MhResult<T>.map(transform: (T) -> R): MhResult<R> = when (this) {
    is MhResult.Success -> MhResult.Success(transform(data), trace)
    is MhResult.Failure -> this
}
