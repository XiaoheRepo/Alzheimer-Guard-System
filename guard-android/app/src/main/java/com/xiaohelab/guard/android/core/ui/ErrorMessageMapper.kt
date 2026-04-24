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
                "E_GOV_4291", "E_REQ_4291" -> context.getString(resId, (e.retryAfterSeconds ?: 0))
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
        "E_USR_4001"  -> R.string.error_E_USR_4001
        "E_USR_4002"  -> R.string.error_E_USR_4002
        "E_USR_4005"  -> R.string.error_E_USR_4005
        "E_USR_4011"  -> R.string.error_E_AUTH_4013  // old_password wrong → reuse string
        "E_USR_4032"  -> R.string.error_E_USR_4032
        "E_USR_4033"  -> R.string.error_E_USR_4033
        "E_USR_4034"  -> R.string.error_E_USR_4034
        "E_USR_4035"  -> R.string.error_E_USR_4035
        "E_USR_4041"  -> R.string.error_E_USR_4041
        "E_USR_4091"  -> R.string.error_E_USR_4091
        "E_USR_4092"  -> R.string.error_E_USR_4092
        "E_USR_4093"  -> R.string.error_E_USR_4093
        "E_USR_4094"  -> R.string.error_E_USR_4094
        "E_USR_4095"  -> R.string.error_E_USR_4095
        // Request validation (§2.1)
        "E_REQ_4001"  -> R.string.error_E_REQ_4001
        "E_REQ_4002"  -> R.string.error_E_REQ_4002
        "E_REQ_4003"  -> R.string.error_E_REQ_4003
        "E_REQ_4150"  -> R.string.error_E_REQ_4150
        "E_REQ_4220"  -> R.string.error_E_REQ_4220
        "E_REQ_4221"  -> R.string.error_E_REQ_4221
        "E_REQ_4291"  -> R.string.error_E_REQ_4291
        // Gov extras
        "E_GOV_4004"  -> R.string.error_E_GOV_4004
        "E_GOV_4012"  -> R.string.error_E_GOV_4012
        "E_GOV_4038"  -> R.string.error_E_GOV_4038
        "E_GOV_4101"  -> R.string.error_E_GOV_4101
        "E_GOV_4131"  -> R.string.error_E_GOV_4131
        // Profile domain (§2.4)
        "E_PRO_4030"  -> R.string.error_E_PRO_4030
        "E_PRO_4031"  -> R.string.error_E_PRO_4031
        "E_PRO_4032"  -> R.string.error_E_PRO_4032
        "E_PRO_4033"  -> R.string.error_E_PRO_4033
        "E_PRO_4034"  -> R.string.error_E_PRO_4034
        "E_PRO_4035"  -> R.string.error_E_PRO_4035
        "E_PRO_4041"  -> R.string.error_E_PRO_4041
        "E_PRO_4042"  -> R.string.error_E_PRO_4042
        "E_PRO_4043"  -> R.string.error_E_PRO_4043
        "E_PRO_4044"  -> R.string.error_E_PRO_4044
        "E_PRO_4045"  -> R.string.error_E_PRO_4045
        "E_PRO_4046"  -> R.string.error_E_PRO_4046
        "E_PRO_4091"  -> R.string.error_E_PRO_4091
        "E_PRO_4092"  -> R.string.error_E_PRO_4092
        "E_PRO_4094"  -> R.string.error_E_PRO_4094
        "E_PRO_4095"  -> R.string.error_E_PRO_4095
        "E_PRO_4096"  -> R.string.error_E_PRO_4096
        "E_PRO_4097"  -> R.string.error_E_PRO_4097
        "E_PRO_4098"  -> R.string.error_E_PRO_4098
        "E_PRO_4099"  -> R.string.error_E_PRO_4099
        "E_PRO_4221"  -> R.string.error_E_PRO_4221
        // Tag domain (M3-A)
        "E_TAG_4041"  -> R.string.error_E_TAG_4041
        "E_TAG_4091"  -> R.string.error_E_TAG_4091
        "E_TAG_4031"  -> R.string.error_E_TAG_4031
        // Material domain (M3-B)
        "E_MAT_4041"  -> R.string.error_E_MAT_4041
        "E_MAT_4291"  -> R.string.error_E_MAT_4291
        // Task domain (M5-A)
        "E_TASK_4041" -> R.string.error_E_TASK_4041
        "E_TASK_4091" -> R.string.error_E_TASK_4091
        "E_TASK_4031" -> R.string.error_E_TASK_4031
        "E_TASK_4221" -> R.string.error_E_TASK_4221
        // Clue domain (M5-B)
        "E_CLUE_4041" -> R.string.error_E_CLUE_4041
        "E_CLUE_4031" -> R.string.error_E_CLUE_4031
        // Notification domain (M6)
        "E_NOTIF_4041" -> R.string.error_E_NOTIF_4041
        // AI domain (M7)
        "E_AI_4041"   -> R.string.error_E_AI_4041
        "E_AI_4291"   -> R.string.error_E_AI_4291
        "E_AI_5001"   -> R.string.error_E_AI_5001
        DomainException.CODE_NETWORK -> R.string.common_network_error
        else -> 0
    }
}
