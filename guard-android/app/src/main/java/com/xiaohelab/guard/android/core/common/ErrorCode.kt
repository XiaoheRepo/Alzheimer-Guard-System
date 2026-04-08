package com.xiaohelab.guard.android.core.common

/** 业务错误码（对齐 API 文档 §2 错误码字典） */
object ErrorCode {
    // 通用与网关
    const val E_REQ_4001 = "E_REQ_4001"
    const val E_REQ_4002 = "E_REQ_4002"
    const val E_REQ_4003 = "E_REQ_4003"
    const val E_REQ_4005 = "E_REQ_4005"
    const val E_REQ_4221 = "E_REQ_4221"
    const val E_GOV_4011 = "E_GOV_4011"  // 会话失效 -> 跳登录
    const val E_GOV_4012 = "E_GOV_4012"
    const val E_GOV_4030 = "E_GOV_4030"
    const val E_GOV_4031 = "E_GOV_4031"
    const val E_GOV_4038 = "E_GOV_4038"
    const val E_GOV_4291 = "E_GOV_4291"  // 限流
    const val E_GOV_4292 = "E_GOV_4292"  // 冷却期
    // 任务域
    const val E_TASK_4001 = "E_TASK_4001"
    const val E_TASK_4002 = "E_TASK_4002"
    const val E_TASK_4004 = "E_TASK_4004"
    const val E_TASK_4005 = "E_TASK_4005"
    const val E_TASK_4041 = "E_TASK_4041"
    const val E_TASK_4091 = "E_TASK_4091"  // 已存在 ACTIVE 任务
    const val E_TASK_4093 = "E_TASK_4093"  // 任务终态，不可重复流转
    const val E_TASK_4222 = "E_TASK_4222"
    // 线索域
    const val E_CLUE_4001 = "E_CLUE_4001"
    const val E_CLUE_4002 = "E_CLUE_4002"
    const val E_CLUE_4003 = "E_CLUE_4003"
    const val E_CLUE_4004 = "E_CLUE_4004"
    const val E_CLUE_4007 = "E_CLUE_4007"
    const val E_CLUE_4012 = "E_CLUE_4012"  // 匿名令牌失效 -> 清除并提示重扫
    const val E_CLUE_4041 = "E_CLUE_4041"
    const val E_CLUE_4042 = "E_CLUE_4042"
    // 档案与监护
    const val E_PRO_4030 = "E_PRO_4030"
    const val E_PRO_4032 = "E_PRO_4032"
    const val E_PRO_4033 = "E_PRO_4033"
    const val E_PRO_4034 = "E_PRO_4034"
    const val E_PRO_4041 = "E_PRO_4041"
    const val E_PRO_4091 = "E_PRO_4091"
    const val E_PRO_4094 = "E_PRO_4094"
    const val E_PRO_4095 = "E_PRO_4095"
    const val E_PRO_4221 = "E_PRO_4221"  // 围栏参数缺失
    // AI 域
    const val E_AI_4001 = "E_AI_4001"
    const val E_AI_4002 = "E_AI_4002"
    const val E_AI_4041 = "E_AI_4041"
    const val E_AI_4292 = "E_AI_4292"  // 频率限制
    const val E_AI_4293 = "E_AI_4293"  // 配额不足
    // 认证
    const val E_AUTH_4001 = "E_AUTH_4001"
    const val E_AUTH_4002 = "E_AUTH_4002"
    const val E_AUTH_4091 = "E_AUTH_4091"
    const val E_USR_4011 = "E_USR_4011"
    // 通知
    const val E_NOTI_4041 = "E_NOTI_4041"
}

/** 将业务错误码映射为用户可读提示文案 */
fun ErrorCode.toUserMessage(code: String): String = when (code) {
    ErrorCode.E_GOV_4011 -> "登录已过期，请重新登录"
    ErrorCode.E_GOV_4030 -> "权限不足，无法执行此操作"
    ErrorCode.E_GOV_4031 -> "账号已被封禁"
    ErrorCode.E_GOV_4291, ErrorCode.E_GOV_4292 -> "操作过于频繁，请稍后重试"
    ErrorCode.E_REQ_4003 -> "客户端版本异常，请更新应用"
    ErrorCode.E_TASK_4091 -> "该患者已有进行中的任务"
    ErrorCode.E_TASK_4093 -> "任务已是终态，无法重复操作"
    ErrorCode.E_TASK_4041 -> "任务不存在"
    ErrorCode.E_CLUE_4012 -> "匿名凭据已失效，请重新扫码"
    ErrorCode.E_PRO_4221 -> "围栏参数不完整，请填写中心点和半径"
    ErrorCode.E_PRO_4091 -> "患者档案已存在"
    ErrorCode.E_PRO_4094 -> "该用户已是监护成员"
    ErrorCode.E_PRO_4095 -> "当前已有待确认的主监护转移请求"
    ErrorCode.E_AI_4292 -> "AI 使用频率过高，请稍后重试"
    ErrorCode.E_AI_4293 -> "AI 配额已用尽"
    ErrorCode.E_AUTH_4001 -> "用户名格式不正确（4-64位，字母数字下划线）"
    ErrorCode.E_AUTH_4002 -> "密码格式不正确（8-64位，需包含数字和字母）"
    ErrorCode.E_AUTH_4091 -> "用户名已存在"
    ErrorCode.E_USR_4011 -> "原密码错误"
    else -> "操作失败（$code）"
}
