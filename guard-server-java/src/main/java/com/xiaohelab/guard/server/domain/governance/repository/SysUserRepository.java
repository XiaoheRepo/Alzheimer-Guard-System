package com.xiaohelab.guard.server.domain.governance.repository;

import com.xiaohelab.guard.server.domain.governance.entity.SysUserEntity;

import java.util.List;
import java.util.Optional;

/**
 * 用户仓储接口（治理域）。
 */
public interface SysUserRepository {

    Optional<SysUserEntity> findById(Long id);

    /** 按用户名查询（登录认证使用） */
    Optional<SysUserEntity> findByUsername(String username);

    /** 检查用户名是否已存在 */
    int countByUsername(String username);

    /** 新增用户（注册） */
    SysUserEntity insert(String username, String passwordHash, String phone,
                         String displayName, String role, String status);

    /** 更新最近登录信息 */
    void updateLoginInfo(Long id, String ip);

    List<SysUserEntity> listByFilter(String role, String status, String keyword,
                                     int limit, int offset);

    long countByFilter(String role, String status, String keyword);

    /** 更新账号状态（NORMAL / BANNED） */
    void updateStatus(Long id, String status);

    /** 更新密码哈希（调用方需提前完成 BCrypt 哈希） */
    void updatePassword(Long id, String passwordHash);

    /** 按手机号查询（邀请前置查询使用） */
    Optional<SysUserEntity> findByPhone(String phone);
}
