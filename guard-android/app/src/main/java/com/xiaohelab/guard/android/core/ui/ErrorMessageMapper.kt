package com.xiaohelab.guard.android.core.ui

import android.content.Context
import com.xiaohelab.guard.android.R
import com.xiaohelab.guard.android.core.common.DomainException

/**
 * §24 错误码 → i18n 文案映射。Android 侧只映射 FAMILY 相关码。
 * 未知码回退到 `error_unknown`，并附码点便于客服定位（HC-04 trace prefix 由 UI 层附加）。
 */
object ErrorMessageMapper {

    fun message(context: Context, e: DomainException): String {
        val resId = resId(e.code)
        return if (resId != 0) {
            when (e.code) {
                "E_GOV_4291" -> context.getString(resId, (e.retryAfterSeconds ?: 0))
                else -> context.getString(resId)
            }
        } else {
            context.getString(R.string.error_unknown, e.code)
        }
    }

    private fun resId(code: String): Int = when (code) {
        "E_AUTH_4001" -> R.string.error_E_AUTH_4001
        "E_AUTH_4002" -> R.string.error_E_AUTH_4002
        "E_AUTH_4011" -> R.string.error_E_AUTH_4011
        "E_AUTH_4012" -> R.string.error_E_AUTH_4012
        "E_AUTH_4013" -> R.string.error_E_AUTH_4013
        "E_AUTH_4091" -> R.string.error_E_AUTH_4091
        "E_AUTH_4092" -> R.string.error_E_AUTH_4092
        "E_GOV_4011" -> R.string.error_E_GOV_4011
        "E_GOV_4030" -> R.string.error_E_GOV_4030
        "E_GOV_4031" -> R.string.error_E_GOV_4031
        "E_GOV_4032" -> R.string.error_E_GOV_4032
        "E_GOV_4291" -> R.string.error_E_GOV_4291
        "E_GOV_4292" -> R.string.error_E_GOV_4292
        "E_GOV_5001" -> R.string.error_E_GOV_5001
        "E_REQ_4001" -> R.string.error_E_REQ_4001
        "E_REQ_4003" -> R.string.error_E_REQ_4003
        "E_PRO_4030" -> R.string.error_E_PRO_4030
        "E_PRO_4031" -> R.string.error_E_PRO_4031
        "E_PRO_4032" -> R.string.error_E_PRO_4032
        "E_PRO_4033" -> R.string.error_E_PRO_4033
        "E_PRO_4091" -> R.string.error_E_PRO_4091
        DomainException.CODE_NETWORK -> R.string.common_network_error
        else -> 0
    }
}
