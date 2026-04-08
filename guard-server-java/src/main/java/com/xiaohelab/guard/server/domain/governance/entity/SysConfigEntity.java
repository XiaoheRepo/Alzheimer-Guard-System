package com.xiaohelab.guard.server.domain.governance.entity;

import lombok.Getter;

import java.time.Instant;

/**
 * 系统配置实体（治理域）。
 * scope: public / ops / security / ai_policy
 */
@Getter
public class SysConfigEntity {

    private String configKey;
    private String configValue;
    private String scope;
    private Long updatedBy;
    private String updatedReason;
    private Instant createdAt;
    private Instant updatedAt;

    private SysConfigEntity() {}

    /** 从持久化数据重建（仅 Infrastructure 层 RepositoryImpl 调用）。 */
    public static SysConfigEntity reconstitute(String configKey, String configValue, String scope,
                                               Long updatedBy, String updatedReason,
                                               Instant createdAt, Instant updatedAt) {
        SysConfigEntity e = new SysConfigEntity();
        e.configKey = configKey;
        e.configValue = configValue;
        e.scope = scope;
        e.updatedBy = updatedBy;
        e.updatedReason = updatedReason;
        e.createdAt = createdAt;
        e.updatedAt = updatedAt;
        return e;
    }

    /** 工厂方法：构造待 upsert 的配置项 */
    public static SysConfigEntity of(String configKey, String configValue, String scope,
                                     Long updatedBy, String updatedReason) {
        SysConfigEntity e = new SysConfigEntity();
        e.configKey = configKey;
        e.configValue = configValue;
        e.scope = scope;
        e.updatedBy = updatedBy;
        e.updatedReason = updatedReason;
        return e;
    }
}
