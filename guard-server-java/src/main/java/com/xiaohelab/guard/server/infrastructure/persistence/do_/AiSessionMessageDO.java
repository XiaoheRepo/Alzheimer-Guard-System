package com.xiaohelab.guard.server.infrastructure.persistence.do_;

import lombok.Data;

import java.time.Instant;

/**
 * ai_session_message 持久化对象。
 * role: user / assistant / system
 * UNIQUE 约束 (session_id, sequence_no)
 */
@Data
public class AiSessionMessageDO {

    private Long id;
    /** 关联 ai_session.session_id（VARCHAR64 FK） */
    private String sessionId;
    private Integer sequenceNo;
    /** user / assistant / system */
    private String role;
    private String content;
    /** token 详情 JSONB 序列化文本（可空） */
    private String tokenUsage;
    private Instant createdAt;
}
