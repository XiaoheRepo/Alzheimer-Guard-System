package com.xiaohelab.guard.server.domain.governance.entity;

import com.xiaohelab.guard.server.infrastructure.persistence.do_.SysConfigDO;
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

    public static SysConfigEntity fromDO(SysConfigDO d) {
        SysConfigEntity e = new SysConfigEntity();
        e.configKey = d.getConfigKey();
        e.configValue = d.getConfigValue();
        e.scope = d.getScope();
        e.updatedBy = d.getUpdatedBy();
        e.updatedReason = d.getUpdatedReason();
        e.createdAt = d.getCreatedAt();
        e.updatedAt = d.getUpdatedAt();
        return e;
    }

    public SysConfigDO toDO() {
        SysConfigDO d = new SysConfigDO();
        d.setConfigKey(this.configKey);
        d.setConfigValue(this.configValue);
        d.setScope(this.scope);
        d.setUpdatedBy(this.updatedBy);
        d.setUpdatedReason(this.updatedReason);
        d.setCreatedAt(this.createdAt);
        d.setUpdatedAt(this.updatedAt);
        return d;
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
