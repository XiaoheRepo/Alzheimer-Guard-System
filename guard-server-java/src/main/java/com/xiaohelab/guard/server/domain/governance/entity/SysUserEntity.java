package com.xiaohelab.guard.server.domain.governance.entity;

import com.xiaohelab.guard.server.common.exception.BizException;
import com.xiaohelab.guard.server.infrastructure.persistence.do_.SysUserDO;
import lombok.Getter;

import java.time.Instant;

/**
 * 用户聚合根（治理域）。
 * 状态机：NORMAL ⇄ BANNED（管理员操作）。
 * passwordHash 仅在密码重置时由调用方更新，领域方法不负责 BCrypt 哈希。
 */
@Getter
public class SysUserEntity {

    public enum UserStatus { NORMAL, BANNED }

    public enum UserRole { FAMILY, ADMIN, SUPERADMIN }

    private Long id;
    private String username;
    private String passwordHash;
    private String displayName;
    private String phone;
    private UserRole role;
    private UserStatus status;
    private Instant lastLoginAt;
    private String lastLoginIp;
    private Instant createdAt;
    private Instant updatedAt;

    private SysUserEntity() {}

    public static SysUserEntity fromDO(SysUserDO d) {
        SysUserEntity e = new SysUserEntity();
        e.id = d.getId();
        e.username = d.getUsername();
        e.passwordHash = d.getPasswordHash();
        e.displayName = d.getDisplayName();
        e.phone = d.getPhone();
        e.role = d.getRole() != null ? UserRole.valueOf(d.getRole()) : UserRole.FAMILY;
        e.status = d.getStatus() != null ? UserStatus.valueOf(d.getStatus()) : UserStatus.NORMAL;
        e.lastLoginAt = d.getLastLoginAt();
        e.lastLoginIp = d.getLastLoginIp();
        e.createdAt = d.getCreatedAt();
        e.updatedAt = d.getUpdatedAt();
        return e;
    }

    /**
     * 管理员封禁账号（NORMAL → BANNED）。
     * SUPERADMIN 账号由调用方在 application 层判断是否允许封禁。
     */
    public void ban() {
        if (this.status == UserStatus.BANNED) {
            throw BizException.of("E_GOV_4003", "用户已处于封禁状态");
        }
        this.status = UserStatus.BANNED;
    }

    /** 管理员解封账号（BANNED → NORMAL）。 */
    public void unban() {
        if (this.status == UserStatus.NORMAL) {
            throw BizException.of("E_GOV_4004", "用户已处于正常状态");
        }
        this.status = UserStatus.NORMAL;
    }

    /** 返回当前状态字符串，供 Repository 持久化使用。 */
    public String getStatusValue() {
        return status.name();
    }
}
