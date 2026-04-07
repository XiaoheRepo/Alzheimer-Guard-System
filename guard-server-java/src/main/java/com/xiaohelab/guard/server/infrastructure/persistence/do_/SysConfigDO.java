package com.xiaohelab.guard.server.infrastructure.persistence.do_;

import lombok.Data;

import java.time.Instant;

/**
 * sys_config 持久化对象（治理配置）。
 * scope: public / ops / security / ai_policy
 */
@Data
public class SysConfigDO {

    private String configKey;
    private String configValue;
    /** public / ops / security / ai_policy */
    private String scope;
    private Long updatedBy;
    private String updatedReason;
    private Instant createdAt;
    private Instant updatedAt;
}
