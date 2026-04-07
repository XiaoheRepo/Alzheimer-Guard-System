package com.xiaohelab.guard.server.infrastructure.persistence.mapper;

import com.xiaohelab.guard.server.infrastructure.persistence.do_.SysConfigDO;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * sys_config 数据访问层（治理配置）。
 * scope: public / ops / security / ai_policy
 */
@Mapper
public interface SysConfigMapper {

    String COLS = "config_key, config_value, scope, updated_by, updated_reason, created_at, updated_at";

    @Select("SELECT " + COLS + " FROM sys_config")
    List<SysConfigDO> listAll();

    @Select("SELECT " + COLS + " FROM sys_config WHERE scope = #{scope}")
    List<SysConfigDO> listByScope(@Param("scope") String scope);

    @Select("SELECT " + COLS + " FROM sys_config WHERE config_key = #{configKey}")
    SysConfigDO findByKey(@Param("configKey") String configKey);

    /** 新增或更新配置项（ON CONFLICT upsert） */
    @Insert("INSERT INTO sys_config(config_key, config_value, scope, updated_by, updated_reason, created_at, updated_at) " +
            "VALUES(#{configKey}, #{configValue}, #{scope}, #{updatedBy}, #{updatedReason}, NOW(), NOW()) " +
            "ON CONFLICT(config_key) DO UPDATE SET config_value=EXCLUDED.config_value, " +
            "updated_by=EXCLUDED.updated_by, updated_reason=EXCLUDED.updated_reason, updated_at=NOW()")
    void upsert(SysConfigDO config);
}
