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
            if (e.code == DomainException.CODE_PROTOCOL && e.message != null) {
                context.getString(R.string.error_unknown, "${e.code}: ${e.message}")
            } else {
                context.getString(R.string.error_unknown, e.code)
            }
        }
    }

    private fun resId(code: String): Int = when (code) {
        // Auth / credential (§2.7)
        "E_AUTH_4001" -> R.string.error_E_AUTH_4001
        "E_AUTH_4002" -> R.string.error_E_AUTH_4002
        "E_AUTH_4011" -> R.string.error_E_AUTH_4011
        "E_AUTH_4012" -> R.string.error_E_AUTH_4012  // client-synthetic: non-FAMILY role
        "E_AUTH_4013" -> R.string.error_E_AUTH_4013
        // §3.6.1 register conflicts — server returns E_GOV_40x1/2; keep E_AUTH aliases as fallback
        "E_AUTH_4091" -> R.string.error_E_GOV_4091
        "E_AUTH_4092" -> R.string.error_E_GOV_4092
        "E_GOV_4091"  -> R.string.error_E_GOV_4091  // username already exists
        "E_GOV_4092"  -> R.string.error_E_GOV_4092  // email already exists
        // Gov domain (§2.1)
        "E_GOV_4011"  -> R.string.error_E_GOV_4011
        "E_GOV_4030"  -> R.string.error_E_GOV_4030
        "E_GOV_4031"  -> R.string.error_E_GOV_4031
        "E_GOV_4032"  -> R.string.error_E_GOV_4032
        "E_GOV_4102"  -> R.string.error_E_GOV_4102  // password reset link expired
        "E_GOV_4291"  -> R.string.error_E_GOV_4291
        "E_GOV_4292"  -> R.string.error_E_GOV_4292
        "E_GOV_5001",
        "E_GOV_5002",
        "E_SYS_5001",
        "E_SYS_5002"  -> R.string.error_E_GOV_5001  // all server-side 5xx → "服务繁忙"
        // User domain (§2.7)
        "E_USR_4011"  -> R.string.error_E_AUTH_4013  // old_password wrong → reuse string
        // Request validation (§2.1)
        "E_REQ_4001"  -> R.string.error_E_REQ_4001
        "E_REQ_4003"  -> R.string.error_E_REQ_4003
        // Profile domain (§2.4)
        "E_PRO_4030"  -> R.string.error_E_PRO_4030
        "E_PRO_4031"  -> R.string.error_E_PRO_4031
        "E_PRO_4032"  -> R.string.error_E_PRO_4032
        "E_PRO_4033"  -> R.string.error_E_PRO_4033
        "E_PRO_4091"  -> R.string.error_E_PRO_4091
        DomainException.CODE_NETWORK -> R.string.common_network_error
        else -> 0
    }
}
