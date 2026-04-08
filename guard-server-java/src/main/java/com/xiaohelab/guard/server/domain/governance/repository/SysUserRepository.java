package com.xiaohelab.guard.server.domain.governance.repository;

import com.xiaohelab.guard.server.domain.governance.entity.SysUserEntity;

import java.util.List;
import java.util.Optional;

/**
 * 用户仓储接口（治理域）——管理侧操作。
 * 认证侧查询（findByUsername 等）维持在 security 包直接使用 SysUserMapper，不在此接口中重复。
 */
public interface SysUserRepository {

    Optional<SysUserEntity> findById(Long id);

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
