package com.xiaohelab.guard.server.infrastructure.persistence.mapper;

import com.xiaohelab.guard.server.infrastructure.persistence.do_.SysLogDO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * sys_log 审计日志数据访问层。
 * 只写不改，审计记录不允许删除或更新。
 * operator_username 为写入时快照，账号改名后历史记录不受影响。
 */
@Mapper
public interface SysLogMapper {

    /** 写入审计日志（读操作可选，写操作必记） */
    @Insert("INSERT INTO sys_log(operator_id, operator_username, operator_role, action_source, " +
            "biz_domain, action_type, target_id, target_type, detail, trace_id, ip_address, " +
            "created_at) " +
            "VALUES(#{operatorId}, #{operatorUsername}, #{operatorRole}, #{actionSource}, " +
            "#{bizDomain}, #{actionType}, #{targetId}, #{targetType}, #{detail}::jsonb, " +
            "#{traceId}, #{ipAddress}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(SysLogDO log);

    /** 分页查询审计日志（管理员使用） */
    @Select("SELECT id, operator_id, operator_username, operator_role, action_source, " +
            "biz_domain, action_type, target_id, target_type, detail::text, trace_id, " +
            "ip_address, created_at " +
            "FROM sys_log WHERE 1=1 " +
            "<if test='bizDomain != null'>AND biz_domain = #{bizDomain}</if> " +
            "<if test='operatorId != null'>AND operator_id = #{operatorId}</if> " +
            "ORDER BY created_at DESC LIMIT #{limit} OFFSET #{offset}")
    List<SysLogDO> listByFilter(@org.apache.ibatis.annotations.Param("bizDomain") String bizDomain,
                                @org.apache.ibatis.annotations.Param("operatorId") Long operatorId,
                                @org.apache.ibatis.annotations.Param("limit") int limit,
                                @org.apache.ibatis.annotations.Param("offset") int offset);

    @Select("SELECT COUNT(*) FROM sys_log")
    long count();
}
