package com.xiaohelab.guard.server.infrastructure.persistence.mapper;

import com.xiaohelab.guard.server.infrastructure.persistence.do_.SysLogDO;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * sys_log 审计日志数据访问层。
 * 只写不改，审计记录不允许删除或更新。
 * operator_username 为写入时快照，账号改名后历史记录不受影响。
 */
@Mapper
public interface SysLogMapper {

    /** 写入审计日志，列名与 DB schema 保持一致 */
    @Insert("INSERT INTO sys_log(module, action, action_id, result_code, executed_at, " +
            "operator_user_id, operator_username, object_id, result, risk_level, detail, " +
            "action_source, agent_profile, execution_mode, confirm_level, blocked_reason, " +
            "request_id, trace_id, created_at) " +
            "VALUES(#{module}, #{action}, #{actionId}, #{resultCode}, #{executedAt}, " +
            "#{operatorUserId}, #{operatorUsername}, #{objectId}, #{result}, #{riskLevel}, " +
            "#{detail}::jsonb, #{actionSource}, #{agentProfile}, #{executionMode}, " +
            "#{confirmLevel}, #{blockedReason}, #{requestId}, #{traceId}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(SysLogDO log);

    /** 分页查询审计日志（管理员使用），支持按 module / operatorUserId 过滤 */
    @Select("SELECT id, module, action, action_id, result_code, executed_at, operator_user_id, " +
            "operator_username, object_id, result, risk_level, detail::text, action_source, " +
            "request_id, trace_id, created_at " +
            "FROM sys_log " +
            "ORDER BY created_at DESC LIMIT #{limit} OFFSET #{offset}")
    List<SysLogDO> listByFilter(@Param("limit") int limit, @Param("offset") int offset);

    /** 按 module 和 operatorUserId 过滤（分开写避免 XML 动态SQL） */
    @Select("SELECT id, module, action, action_id, result_code, executed_at, operator_user_id, " +
            "operator_username, object_id, result, risk_level, detail::text, action_source, " +
            "request_id, trace_id, created_at " +
            "FROM sys_log WHERE module = #{module} " +
            "ORDER BY created_at DESC LIMIT #{limit} OFFSET #{offset}")
    List<SysLogDO> listByModule(@Param("module") String module,
                                @Param("limit") int limit,
                                @Param("offset") int offset);

    @Select("SELECT COUNT(*) FROM sys_log")
    long count();

    /** 按 objectId 查询审计记录（用于任务/线索审计轨迹） */
    @Select("SELECT id, module, action, action_id, result_code, executed_at, operator_user_id, " +
            "operator_username, object_id, result, risk_level, detail::text, action_source, " +
            "request_id, trace_id, created_at " +
            "FROM sys_log WHERE object_id = #{objectId} " +
            "ORDER BY created_at DESC, id DESC LIMIT #{limit} OFFSET #{offset}")
    List<SysLogDO> listByObjectId(@Param("objectId") String objectId,
                                   @Param("limit") int limit,
                                   @Param("offset") int offset);

    @Select("SELECT COUNT(*) FROM sys_log WHERE object_id = #{objectId}")
    long countByObjectId(@Param("objectId") String objectId);
}
