package com.xiaohelab.guard.server.infrastructure.persistence.do_;

import lombok.Data;

import java.time.Instant;

/**
 * ai_session 持久化对象。
 * status: ACTIVE / ARCHIVED
 * messages 与 tokenUsage 为 JSONB 字段，以 text 形式映射后业务层自行解析。
 */
@Data
public class AiSessionDO {

    private Long id;
    /** 业务会话 ID，格式 ais_ + 时间戳，全局唯一 */
    private String sessionId;
    private Long userId;
    private Long patientId;
    /** 关联任务 ID（可空） */
    private Long taskId;
    /** 消息列表 JSONB 序列化文本 */
    private String messages;
    private Integer requestTokens;
    private Integer responseTokens;
    /** token 详情 JSONB 序列化文本（可空） */
    private String tokenUsage;
    /** 累计消耗 token 数 */
    private Integer tokenUsed;
    private String modelName;
    /** ACTIVE / ARCHIVED */
    private String status;
    private Instant archivedAt;
    /** 乐观锁版本，CAS 更新使用 */
    private Long version;
    private Instant createdAt;
    private Instant updatedAt;
}
