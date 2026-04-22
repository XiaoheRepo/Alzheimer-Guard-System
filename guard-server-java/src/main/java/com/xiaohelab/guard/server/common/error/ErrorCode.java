package com.xiaohelab.guard.server.common.error;

import org.springframework.http.HttpStatus;

/**
 * 全局错误码字典（按 API 契约 + LLD 详设整合）。
 */
public enum ErrorCode {

    // ===== 请求网关 =====
    E_REQ_4001(HttpStatus.BAD_REQUEST,  "E_REQ_4001",  "幂等键格式不合法"),
    E_REQ_4002(HttpStatus.BAD_REQUEST,  "E_REQ_4002",  "Trace-Id 不合法"),
    E_REQ_4003(HttpStatus.BAD_REQUEST,  "E_REQ_4003",  "伪造保留 Header"),
    E_REQ_4005(HttpStatus.BAD_REQUEST,  "E_REQ_4005",  "ID 参数格式不合法"),
    E_REQ_4150(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "E_REQ_4150", "Content-Type 必须为 application/json"),
    E_REQ_4221(HttpStatus.UNPROCESSABLE_ENTITY, "E_REQ_4221", "请求时间偏差超限"),
    E_REQ_4220(HttpStatus.UNPROCESSABLE_ENTITY, "E_REQ_4220", "请求参数校验失败"),

    // ===== 治理 / 安全 =====
    E_GOV_4004(HttpStatus.BAD_REQUEST, "E_GOV_4004", "设备指纹格式非法"),
    E_GOV_4011(HttpStatus.UNAUTHORIZED, "E_GOV_4011", "鉴权失败或缺失凭据"),
    E_GOV_4012(HttpStatus.UNAUTHORIZED, "E_GOV_4012", "ws_ticket 无效/过期/已使用"),
    E_GOV_4030(HttpStatus.FORBIDDEN, "E_GOV_4030", "角色或授权不足"),
    E_GOV_4031(HttpStatus.FORBIDDEN, "E_GOV_4031", "账号已被封禁"),
    E_GOV_4032(HttpStatus.FORBIDDEN, "E_GOV_4032", "高危操作仅限 SUPER_ADMIN"),
    E_GOV_4038(HttpStatus.FORBIDDEN, "E_GOV_4038", "CAPTCHA 校验失败"),
    E_GOV_4039(HttpStatus.FORBIDDEN, "E_GOV_4039", "策略守卫拒绝 Agent 执行"),
    E_GOV_4041(HttpStatus.NOT_FOUND, "E_GOV_4041", "配置键不存在"),
    E_GOV_4046(HttpStatus.NOT_FOUND, "E_GOV_4046", "Outbox DEAD 事件不存在"),
    E_GOV_4091(HttpStatus.CONFLICT, "E_GOV_4091", "用户名已存在"),
    E_GOV_4092(HttpStatus.CONFLICT, "E_GOV_4092", "邮箱已存在"),
    E_GOV_4096(HttpStatus.CONFLICT, "E_GOV_4096", "Outbox 事件状态不允许重放"),
    E_GOV_4097(HttpStatus.CONFLICT, "E_GOV_4097", "Agent 确认等级不足"),
    E_GOV_4098(HttpStatus.CONFLICT, "E_GOV_4098", "同分区有更早未修复 DEAD 事件"),
    E_GOV_4101(HttpStatus.GONE, "E_GOV_4101", "邮箱验证链接已过期"),
    E_GOV_4102(HttpStatus.GONE, "E_GOV_4102", "密码重置链接已过期"),
    E_GOV_4226(HttpStatus.UNPROCESSABLE_ENTITY, "E_GOV_4226", "预检查失败"),
    E_GOV_4231(HttpStatus.LOCKED, "E_GOV_4231", "接口仅允许人工执行"),
    E_GOV_4291(HttpStatus.TOO_MANY_REQUESTS, "E_GOV_4291", "网关限流"),
    E_GOV_4292(HttpStatus.TOO_MANY_REQUESTS, "E_GOV_4292", "冷却期未结束"),

    // ===== AUTH / USER =====
    E_AUTH_4001(HttpStatus.BAD_REQUEST, "E_AUTH_4001", "username 格式非法"),
    E_AUTH_4002(HttpStatus.BAD_REQUEST, "E_AUTH_4002", "password 格式非法"),
    E_AUTH_4011(HttpStatus.UNAUTHORIZED, "E_AUTH_4011", "登录凭据错误"),
    E_AUTH_4031(HttpStatus.FORBIDDEN, "E_AUTH_4031", "角色或确认等级不足"),
    E_USR_4001(HttpStatus.BAD_REQUEST, "E_USR_4001", "新旧密码相同"),
    E_USR_4002(HttpStatus.BAD_REQUEST, "E_USR_4002", "新密码不满足强度"),
    E_USR_4004(HttpStatus.BAD_REQUEST, "E_USR_4004", "role 枚举非法"),
    E_USR_4005(HttpStatus.BAD_REQUEST, "E_USR_4005", "nickname / email / phone 格式非法"),
    E_USR_4011(HttpStatus.UNAUTHORIZED, "E_USR_4011", "原密码校验失败"),
    E_USR_4032(HttpStatus.FORBIDDEN, "E_USR_4032", "ADMIN 只能操作 FAMILY 账号"),
    E_USR_4033(HttpStatus.FORBIDDEN, "E_USR_4033", "SUPER_ADMIN 不可被禁用/注销/降级"),
    E_USR_4034(HttpStatus.FORBIDDEN, "E_USR_4034", "不可对自身执行该高危操作"),
    E_USR_4035(HttpStatus.FORBIDDEN, "E_USR_4035", "role 字段仅 SUPER_ADMIN 可修改"),
    E_USR_4041(HttpStatus.NOT_FOUND, "E_USR_4041", "目标用户不存在"),
    E_USR_4091(HttpStatus.CONFLICT, "E_USR_4091", "用户状态不允许该操作"),
    E_USR_4092(HttpStatus.CONFLICT, "E_USR_4092", "目标仍为患者主监护,禁止注销"),
    E_USR_4093(HttpStatus.CONFLICT, "E_USR_4093", "目标仍有未终态任务/工单,禁止注销"),

    // ===== TASK =====
    E_TASK_4001(HttpStatus.BAD_REQUEST, "E_TASK_4001", "source 枚举不合法"),
    E_TASK_4002(HttpStatus.BAD_REQUEST, "E_TASK_4002", "remark 长度非法"),
    E_TASK_4003(HttpStatus.BAD_REQUEST, "E_TASK_4003", "禁止客户端传入 reported_by"),
    E_TASK_4004(HttpStatus.BAD_REQUEST, "E_TASK_4004", "close_type 非法"),
    E_TASK_4005(HttpStatus.BAD_REQUEST, "E_TASK_4005", "FALSE_ALARM 缺少 reason"),
    E_TASK_4030(HttpStatus.FORBIDDEN, "E_TASK_4030", "任务操作无授权"),
    E_TASK_4031(HttpStatus.FORBIDDEN, "E_TASK_4031", "无关闭权限"),
    E_TASK_4032(HttpStatus.FORBIDDEN, "E_TASK_4032", "无患者监护授权"),
    E_TASK_4041(HttpStatus.NOT_FOUND, "E_TASK_4041", "任务不存在"),
    E_TASK_4091(HttpStatus.CONFLICT, "E_TASK_4091", "同患者已存在非终态任务"),
    E_TASK_4092(HttpStatus.CONFLICT, "E_TASK_4092", "任务状态不可迁移"),
    E_TASK_4093(HttpStatus.CONFLICT, "E_TASK_4093", "SUSTAINED 态禁止误报关闭"),
    E_TASK_4094(HttpStatus.CONFLICT, "E_TASK_4094", "并发冲突,请重试"),
    E_TASK_4221(HttpStatus.UNPROCESSABLE_ENTITY, "E_TASK_4221", "误报关闭条件不满足"),

    // ===== CLUE =====
    E_CLUE_4001(HttpStatus.BAD_REQUEST, "E_CLUE_4001", "纬度非法"),
    E_CLUE_4002(HttpStatus.BAD_REQUEST, "E_CLUE_4002", "经度非法"),
    E_CLUE_4003(HttpStatus.BAD_REQUEST, "E_CLUE_4003", "description 非法"),
    E_CLUE_4004(HttpStatus.BAD_REQUEST, "E_CLUE_4004", "photo_url 非白名单"),
    E_CLUE_4005(HttpStatus.BAD_REQUEST, "E_CLUE_4005", "short_code 非法"),
    E_CLUE_4007(HttpStatus.BAD_REQUEST, "E_CLUE_4007", "坐标系非法或转换失败"),
    E_CLUE_4008(HttpStatus.BAD_REQUEST, "E_CLUE_4008", "override 参数非法"),
    E_CLUE_4009(HttpStatus.BAD_REQUEST, "E_CLUE_4009", "override_reason 非法"),
    E_CLUE_4010(HttpStatus.BAD_REQUEST, "E_CLUE_4010", "reject_reason 非法"),
    E_CLUE_4012(HttpStatus.CONFLICT, "E_CLUE_4012", "entry_token 无效/重放/已消费"),
    E_CLUE_4013(HttpStatus.FORBIDDEN, "E_CLUE_4013", "凭据绑定校验失败"),
    E_CLUE_4041(HttpStatus.NOT_FOUND, "E_CLUE_4041", "标签不可用"),
    E_CLUE_4042(HttpStatus.NOT_FOUND, "E_CLUE_4042", "short_code 无效"),
    E_CLUE_4043(HttpStatus.NOT_FOUND, "E_CLUE_4043", "clue_id 不存在"),
    E_CLUE_4091(HttpStatus.CONFLICT, "E_CLUE_4091", "复核状态不可变"),
    E_CLUE_4221(HttpStatus.UNPROCESSABLE_ENTITY, "E_CLUE_4221", "非可疑线索不可复核"),

    // ===== PROFILE =====
    E_PRO_4001(HttpStatus.BAD_REQUEST, "E_PRO_4001", "patient_name 非法"),
    E_PRO_4002(HttpStatus.BAD_REQUEST, "E_PRO_4002", "birthday 非法"),
    E_PRO_4003(HttpStatus.BAD_REQUEST, "E_PRO_4003", "gender 非法"),
    E_PRO_4005(HttpStatus.BAD_REQUEST, "E_PRO_4005", "围栏参数非法"),
    E_PRO_4006(HttpStatus.BAD_REQUEST, "E_PRO_4006", "relation_role 非法"),
    E_PRO_4008(HttpStatus.BAD_REQUEST, "E_PRO_4008", "invitation action 非法"),
    E_PRO_4009(HttpStatus.BAD_REQUEST, "E_PRO_4009", "transfer reason 非法"),
    E_PRO_4010(HttpStatus.BAD_REQUEST, "E_PRO_4010", "transfer expire_in_seconds 非法"),
    E_PRO_4011(HttpStatus.BAD_REQUEST, "E_PRO_4011", "transfer confirm action 非法"),
    E_PRO_4012(HttpStatus.BAD_REQUEST, "E_PRO_4012", "transfer cancel_reason 非法"),
    E_PRO_4013(HttpStatus.BAD_REQUEST, "E_PRO_4013", "reject_reason 非法"),
    E_PRO_4014(HttpStatus.BAD_REQUEST, "E_PRO_4014", "avatar_url 缺失/非法/尝试清空"),
    E_PRO_4015(HttpStatus.BAD_REQUEST, "E_PRO_4015", "confirm action 非法"),
    E_PRO_4030(HttpStatus.FORBIDDEN, "E_PRO_4030", "患者授权不足"),
    E_PRO_4032(HttpStatus.FORBIDDEN, "E_PRO_4032", "无主监护管理权限"),
    E_PRO_4033(HttpStatus.FORBIDDEN, "E_PRO_4033", "非目标受方确认"),
    E_PRO_4034(HttpStatus.FORBIDDEN, "E_PRO_4034", "非原发起方撤销"),
    E_PRO_4035(HttpStatus.FORBIDDEN, "E_PRO_4035", "非主监护人"),
    E_PRO_4041(HttpStatus.NOT_FOUND, "E_PRO_4041", "patient_id 不存在"),
    E_PRO_4042(HttpStatus.NOT_FOUND, "E_PRO_4042", "invitee_user_id 不存在"),
    E_PRO_4043(HttpStatus.NOT_FOUND, "E_PRO_4043", "invite_id 不存在或状态非法"),
    E_PRO_4044(HttpStatus.UNPROCESSABLE_ENTITY, "E_PRO_4044", "target_user_id 非活跃成员"),
    E_PRO_4045(HttpStatus.NOT_FOUND, "E_PRO_4045", "transfer_request_id 不存在"),
    E_PRO_4046(HttpStatus.NOT_FOUND, "E_PRO_4046", "监护关系不存在"),
    E_PRO_4091(HttpStatus.CONFLICT, "E_PRO_4091", "患者档案冲突"),
    E_PRO_4092(HttpStatus.CONFLICT, "E_PRO_4092", "状态机流转不合法"),
    E_PRO_4094(HttpStatus.CONFLICT, "E_PRO_4094", "重复邀请或已激活成员"),
    E_PRO_4095(HttpStatus.CONFLICT, "E_PRO_4095", "已存在 PENDING_CONFIRM 转移"),
    E_PRO_4096(HttpStatus.CONFLICT, "E_PRO_4096", "邀请状态冲突"),
    E_PRO_4097(HttpStatus.CONFLICT, "E_PRO_4097", "转移请求状态冲突"),
    E_PRO_4098(HttpStatus.CONFLICT, "E_PRO_4098", "转移请求不可撤销"),
    E_PRO_4099(HttpStatus.CONFLICT, "E_PRO_4099", "不可移除自身或受方非 ACTIVE"),
    E_PRO_4221(HttpStatus.UNPROCESSABLE_ENTITY, "E_PRO_4221", "围栏启用缺中心点/半径"),

    // ===== MAT =====
    E_MAT_4001(HttpStatus.BAD_REQUEST, "E_MAT_4001", "quantity 非法"),
    E_MAT_4002(HttpStatus.BAD_REQUEST, "E_MAT_4002", "resource_token 格式非法"),
    E_MAT_4003(HttpStatus.BAD_REQUEST, "E_MAT_4003", "cancel_reason 非法"),
    E_MAT_4004(HttpStatus.BAD_REQUEST, "E_MAT_4004", "lost_reason 非法"),
    E_MAT_4030(HttpStatus.FORBIDDEN, "E_MAT_4030", "物资域权限不足"),
    E_MAT_4032(HttpStatus.FORBIDDEN, "E_MAT_4032", "resource_token 审计载荷非法"),
    E_MAT_4041(HttpStatus.NOT_FOUND, "E_MAT_4041", "order_id 不存在"),
    E_MAT_4042(HttpStatus.NOT_FOUND, "E_MAT_4042", "标签不存在"),
    E_MAT_4044(HttpStatus.NOT_FOUND, "E_MAT_4044", "tag_code 不存在"),
    E_MAT_4091(HttpStatus.CONFLICT, "E_MAT_4091", "工单状态不允许流转"),
    E_MAT_4094(HttpStatus.CONFLICT, "E_MAT_4094", "工单状态冲突,无法取消"),
    E_MAT_4096(HttpStatus.CONFLICT, "E_MAT_4096", "标签三方一致性校验失败"),
    E_MAT_4098(HttpStatus.CONFLICT, "E_MAT_4098", "标签状态不满足 LOST 前置条件"),
    E_MAT_4221(HttpStatus.UNPROCESSABLE_ENTITY, "E_MAT_4221", "标签数量与工单不匹配"),
    E_MAT_4222(HttpStatus.UNPROCESSABLE_ENTITY, "E_MAT_4222", "发货前置条件不满足"),
    E_MAT_4223(HttpStatus.UNPROCESSABLE_ENTITY, "E_MAT_4223", "resource_token 验签/解密失败"),
    E_MAT_4225(HttpStatus.UNPROCESSABLE_ENTITY, "E_MAT_4225", "批量发号超限"),

    // ===== AI =====
    E_AI_4001(HttpStatus.BAD_REQUEST, "E_AI_4001", "session_id 或 prompt 非法"),
    E_AI_4002(HttpStatus.BAD_REQUEST, "E_AI_4002", "AI 会话创建参数非法"),
    E_AI_4031(HttpStatus.FORBIDDEN, "E_AI_4031", "内容安全限制"),
    E_AI_4033(HttpStatus.FORBIDDEN, "E_AI_4033", "无 patient/task 归属权限"),
    E_AI_4041(HttpStatus.NOT_FOUND, "E_AI_4041", "ai_session 不存在"),
    E_AI_4091(HttpStatus.CONFLICT, "E_AI_4091", "会话并发冲突或意图过期/已处理"),
    E_AI_4292(HttpStatus.TOO_MANY_REQUESTS, "E_AI_4292", "用户 AI 配额耗尽"),
    E_AI_4293(HttpStatus.TOO_MANY_REQUESTS, "E_AI_4293", "患者 AI 配额耗尽"),
    E_AI_5021(HttpStatus.INTERNAL_SERVER_ERROR, "E_AI_5021", "AI 上下文溢出"),
    E_AI_5031(HttpStatus.SERVICE_UNAVAILABLE, "E_AI_5031", "模型服务超时"),

    // ===== NOTIFICATION =====
    E_NOTI_4030(HttpStatus.FORBIDDEN, "E_NOTI_4030", "无通知访问权限"),
    E_NOTI_4041(HttpStatus.NOT_FOUND, "E_NOTI_4041", "notification_id 不存在"),

    // ===== SYS =====
    E_SYS_5000(HttpStatus.INTERNAL_SERVER_ERROR, "E_SYS_5000", "服务器内部错误"),
    E_SYS_5001(HttpStatus.INTERNAL_SERVER_ERROR, "E_SYS_5001", "Outbox 写入失败"),
    E_SYS_5002(HttpStatus.INTERNAL_SERVER_ERROR, "E_SYS_5002", "Redis 不可用"),
    E_SYS_5031(HttpStatus.SERVICE_UNAVAILABLE, "E_SYS_5031", "依赖服务超时");

    private final HttpStatus httpStatus;
    private final String code;
    private final String defaultMessage;

    ErrorCode(HttpStatus httpStatus, String code, String defaultMessage) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus httpStatus() { return httpStatus; }
    public String code() { return code; }
    public String defaultMessage() { return defaultMessage; }
}
