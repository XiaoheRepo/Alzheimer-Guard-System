package com.xiaohelab.guard.server.common.event;

/**
 * 领域事件 Topic 定义（Kafka / Redis Streams 通道名）。
 */
public final class OutboxTopics {

    private OutboxTopics() {}

    // TASK
    public static final String TASK_CREATED            = "task.created";
    public static final String TASK_STATE_CHANGED      = "task.state.changed";
    public static final String TASK_CLOSED_FOUND       = "task.closed.found";
    public static final String TASK_CLOSED_FALSE_ALARM = "task.closed.false_alarm";
    public static final String TASK_SUSTAINED          = "task.sustained";

    // CLUE
    public static final String CLUE_REPORTED_RAW       = "clue.reported.raw";
    public static final String CLUE_VALIDATED          = "clue.validated";
    public static final String CLUE_SUSPECTED          = "clue.suspected";
    public static final String CLUE_OVERRIDDEN         = "clue.overridden";
    public static final String CLUE_REJECTED           = "clue.rejected";
    public static final String TRACK_UPDATED           = "track.updated";
    public static final String FENCE_BREACHED          = "fence.breached";

    // PROFILE
    public static final String PROFILE_CREATED         = "profile.created";
    public static final String PROFILE_UPDATED         = "profile.updated";
    public static final String PROFILE_DELETED_LOGICAL = "profile.deleted.logical";
    public static final String PATIENT_MISSING_PENDING = "patient.missing_pending";
    public static final String PATIENT_CONFIRMED_SAFE  = "patient.confirmed_safe";
    public static final String GUARDIAN_INVITED        = "guardian.invited";
    public static final String GUARDIAN_JOINED         = "guardian.joined";
    public static final String GUARDIAN_TRANSFER_REQ   = "guardian.transfer.requested";
    public static final String GUARDIAN_TRANSFER_DONE  = "guardian.transfer.completed";

    // MAT
    public static final String MAT_ORDER_CREATED       = "material.order.created";
    public static final String MAT_ORDER_APPROVED      = "material.order.approved";
    public static final String MAT_ORDER_REJECTED      = "material.order.rejected";
    public static final String MAT_ORDER_SHIPPED       = "material.order.shipped";
    public static final String MAT_ORDER_RECEIVED      = "material.order.received";
    public static final String TAG_ALLOCATED           = "tag.allocated";
    public static final String TAG_BOUND               = "tag.bound";
    public static final String TAG_SUSPECTED_LOST      = "tag.suspected_lost";
    public static final String TAG_LOST                = "tag.lost";

    // AI
    public static final String AI_STRATEGY_GENERATED   = "ai.strategy.generated";
    public static final String AI_POSTER_GENERATED     = "ai.poster.generated";
    public static final String MEMORY_APPENDED         = "memory.appended";

    // GOV
    public static final String NOTIFICATION_SENT       = "notification.sent";

    // GOV / 管理员治理（V2.1 增量：对应 SRS FR-GOV-011 ~ FR-GOV-014、FR-PRO-012）
    public static final String USER_ROLE_CHANGED       = "user.role.changed";
    public static final String USER_DISABLED           = "user.disabled";
    public static final String USER_ENABLED            = "user.enabled";
    public static final String USER_DEACTIVATED        = "user.deactivated";
    public static final String PATIENT_PRIMARY_GUARDIAN_FORCE_TRANSFERRED
            = "patient.primary_guardian.force_transferred";
}
