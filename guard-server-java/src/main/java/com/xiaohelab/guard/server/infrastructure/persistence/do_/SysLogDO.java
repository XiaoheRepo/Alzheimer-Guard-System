package com.xiaohelab.guard.server.infrastructure.persistence.do_;

import lombok.Data;
import java.time.Instant;

/** sys_log 审计日志 DO */
@Data
public class SysLogDO {
    private Long id;
    private String module;
    private String action;
    private String actionId;
    private String resultCode;
    private Instant executedAt;
    private Long operatorUserId;
    /** 操作账号快照，操作发生时固化 */
    private String operatorUsername;
    private String objectId;
    /** SUCCESS / FAIL */
    private String result;
    /** LOW / MEDIUM / HIGH / CRITICAL */
    private String riskLevel;
    /** 扩展信息 JSON */
    private String detail;
    /** USER / AI_AGENT */
    private String actionSource;
    private String agentProfile;
    private String executionMode;
    private String confirmLevel;
    private String blockedReason;
    private String requestId;
    private String traceId;
    private Instant createdAt;
}
