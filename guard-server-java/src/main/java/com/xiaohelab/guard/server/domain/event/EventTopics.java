package com.xiaohelab.guard.server.domain.event;

/**
 * 系统全量 Kafka Topic 与事件类型常量。
 * 消费者、生产者统一引用此类，避免字符串分散。
 */
public final class EventTopics {

    private EventTopics() {}

    // ===== 任务域事件 =====
    /** Topic：rescue.task */
    public static final String TOPIC_RESCUE_TASK = "rescue.task";

    /** 寻回任务创建（患者进入 LOST 状态触发） */
    public static final String TASK_CREATED = "task.created";
    /** 寻回任务关闭——找回 */
    public static final String TASK_RESOLVED = "task.resolved";
    /** 寻回任务关闭——误报 */
    public static final String TASK_FALSE_ALARM = "task.false_alarm";
    /** 任务状态变更（通用，携带版本号） */
    public static final String TASK_STATE_CHANGED = "task.state.changed";

    // ===== 线索域事件 =====
    /** Topic：clue.intake（削峰，不走 Outbox） */
    public static final String TOPIC_CLUE_INTAKE = "clue.intake";
    /** Topic：clue.analysis */
    public static final String TOPIC_CLUE_ANALYSIS = "clue.analysis";

    /** 线索原始上报（入站削峰，不走 Outbox，直写 Kafka） */
    public static final String CLUE_REPORTED_RAW = "clue.reported.raw";
    /** 线索经研判后验证通过 */
    public static final String CLUE_VALIDATED = "clue.validated";
    /** 疑似发现（高风险线索） */
    public static final String CLUE_SUSPECTED = "clue.suspected";
    /** 线索被拒绝/被管理员否决 */
    public static final String CLUE_REJECTED = "clue.rejected";
    /** 请求向量化 */
    public static final String CLUE_VECTORIZE_REQUESTED = "clue.vectorize.requested";

    // ===== 档案域事件 =====
    /** Topic：patient.profile */
    public static final String TOPIC_PATIENT_PROFILE = "patient.profile";

    public static final String PROFILE_UPDATED = "profile.updated";
    public static final String PROFILE_CORRECTED = "profile.corrected";
    public static final String PROFILE_DELETED_LOGICAL = "profile.deleted.logical";

    // ===== 标签域事件 =====
    /** Topic：tag.asset */
    public static final String TOPIC_TAG_ASSET = "tag.asset";

    public static final String TAG_BOUND = "tag.bound";

    // ===== 轨迹事件 =====
    /** Topic：patient.track */
    public static final String TOPIC_PATIENT_TRACK = "patient.track";

    public static final String TRACK_UPDATED = "track.updated";
    public static final String FENCE_BREACHED = "fence.breached";

    // ===== AI 域事件 =====
    /** Topic：ai.output */
    public static final String TOPIC_AI_OUTPUT = "ai.output";

    public static final String AI_STRATEGY_GENERATED = "ai.strategy.generated";
    public static final String AI_POSTER_GENERATED = "ai.poster.generated";
    public static final String MEMORY_APPENDED = "memory.appended";
    public static final String MEMORY_EXPIRED = "memory.expired";

    // ===== 物资域事件 =====
    /** Topic：material.order */
    public static final String TOPIC_MATERIAL_ORDER = "material.order";

    public static final String MATERIAL_ORDER_CREATED = "material.order.created";

    // ===== 通知 WebSocket 定向推送 =====
    /** 前缀：ws.push.{pod_id}（HC-05，禁止广播） */
    public static final String TOPIC_WS_PUSH_PREFIX = "ws.push.";
}
