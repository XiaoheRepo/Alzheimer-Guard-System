package com.xiaohelab.guard.android.core.common

import java.util.UUID

/** 生成符合契约的 Trace-Id（16-64位，字母数字与-） */
fun generateTraceId(): String = "trc_${UUID.randomUUID().toString().replace("-", "").take(24)}"

/** 生成符合契约的 Request-Id（16-64位，字母数字与-），写接口重试时保持同值 */
fun generateRequestId(): String = "req_${UUID.randomUUID().toString().replace("-", "").take(24)}"

/** 格式化 ISO-8601 时间戳为本地展示字符串（简单格式化，UI 层调用） */
fun String.toDisplayTime(): String {
    return try {
        val instant = java.time.Instant.parse(this)
        val local = instant.atZone(java.time.ZoneId.systemDefault())
        val formatter = java.time.format.DateTimeFormatter.ofPattern("MM-dd HH:mm")
        local.format(formatter)
    } catch (e: Exception) {
        this
    }
}

fun String.toDisplayDate(): String {
    return try {
        val instant = java.time.Instant.parse(this)
        val local = instant.atZone(java.time.ZoneId.systemDefault())
        val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")
        local.format(formatter)
    } catch (e: Exception) {
        this
    }
}

fun String.toDisplayDateTime(): String {
    return try {
        val instant = java.time.Instant.parse(this)
        val local = instant.atZone(java.time.ZoneId.systemDefault())
        val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        local.format(formatter)
    } catch (e: Exception) {
        this
    }
}
