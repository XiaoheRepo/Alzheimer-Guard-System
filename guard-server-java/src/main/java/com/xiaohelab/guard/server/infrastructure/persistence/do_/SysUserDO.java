package com.xiaohelab.guard.server.infrastructure.persistence.do_;

import lombok.Data;

import java.time.Instant;

/**
 * sys_user 持久化对象。
 * 命名规范：DO 后缀，禁止出现在 Controller 入参/出参中（见 backend_handbook §5.1）。
 */
@Data
public class SysUserDO {

    private Long id;
    private String username;
    /** BCrypt 哈希，禁止明文存储 */
    private String passwordHash;
    private String displayName;
    private String phone;
    /** 角色：FAMILY / ADMIN / SUPERADMIN */
    private String role;
    /** 状态：NORMAL / BANNED */
    private String status;
    private Instant lastLoginAt;
    private String lastLoginIp;
    private Instant createdAt;
    private Instant updatedAt;
}
