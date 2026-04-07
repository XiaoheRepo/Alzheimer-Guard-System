package com.xiaohelab.guard.server.infrastructure.persistence.mapper;

import com.xiaohelab.guard.server.infrastructure.persistence.do_.SysUserDO;
import org.apache.ibatis.annotations.*;

/**
 * sys_user 数据访问层。
 * 只做数据映射，不包含业务规则。
 */
@Mapper
public interface SysUserMapper {

    /** 按主键查询 */
    @Select("SELECT id, username, password_hash, display_name, phone, role, status, " +
            "last_login_at, last_login_ip, created_at, updated_at FROM sys_user WHERE id = #{id}")
    SysUserDO findById(Long id);

    /** 按用户名查询（登录认证使用） */
    @Select("SELECT id, username, password_hash, display_name, phone, role, status, " +
            "last_login_at, last_login_ip, created_at, updated_at FROM sys_user WHERE username = #{username}")
    SysUserDO findByUsername(String username);

    /** 新增用户 */
    @Insert("INSERT INTO sys_user(username, password_hash, display_name, phone, role, status, created_at, updated_at) " +
            "VALUES(#{username}, #{passwordHash}, #{displayName}, #{phone}, #{role}, #{status}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(SysUserDO user);

    /** 更新最近登录信息 */
    @Update("UPDATE sys_user SET last_login_at=NOW(), last_login_ip=#{ip}, updated_at=NOW() WHERE id=#{id}")
    void updateLoginInfo(@Param("id") Long id, @Param("ip") String ip);

    /** 更新密码哈希 */
    @Update("UPDATE sys_user SET password_hash=#{hash}, updated_at=NOW() WHERE id=#{id}")
    void updatePassword(@Param("id") Long id, @Param("hash") String hash);

    /** 更新账号状态 */
    @Update("UPDATE sys_user SET status=#{status}, updated_at=NOW() WHERE id=#{id}")
    int updateStatus(@Param("id") Long id, @Param("status") String status);

    /** 检查用户名是否已存在 */
    @Select("SELECT COUNT(1) FROM sys_user WHERE username = #{username}")
    int countByUsername(String username);
}
