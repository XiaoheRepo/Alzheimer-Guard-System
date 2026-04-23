package com.xiaohelab.guard.android.core.logging

import timber.log.Timber

/**
 * HC-07: PII 脱敏树。生产包禁止将姓名、手机号、精确定位、Token、Bearer 写入日志。
 * 本 Tree 会对常见敏感模式做正则兜底，真正严格的脱敏应在调用侧完成。
 */
class PiiRedactingTree(private val debug: Boolean) : Timber.DebugTree() {

    override fun isLoggable(tag: String?, priority: Int): Boolean {
        // Release 包只输出 INFO 以上。
        return debug || priority >= android.util.Log.INFO
    }

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        super.log(priority, tag, redact(message), t)
    }

    companion object {
        private val PHONE = Regex("(?<!\\d)(1[3-9]\\d{9})(?!\\d)")
        private val BEARER = Regex("Bearer\\s+[A-Za-z0-9._~+/=-]+", RegexOption.IGNORE_CASE)
        private val EMAIL = Regex("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}")
        private val GEO = Regex("(?:lat|lng|latitude|longitude)\\s*=\\s*-?\\d+\\.\\d+", RegexOption.IGNORE_CASE)

        fun redact(input: String): String = input
            .replace(PHONE) { m -> m.value.take(3) + "****" + m.value.takeLast(4) }
            .replace(BEARER, "Bearer ***")
            .replace(EMAIL) { m ->
                val at = m.value.indexOf('@')
                if (at <= 2) "***@***" else m.value.take(2) + "****" + m.value.substring(at)
            }
            .replace(GEO, "coord=***")
    }
}
