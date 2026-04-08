package com.xiaohelab.guard.server.domain.governance.entity;

import com.xiaohelab.guard.server.common.exception.BizException;
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

    /** 从持久化数据重建聚合根（仅 Infrastructure 层 RepositoryImpl 调用）。 */
    public static SysUserEntity reconstitute(Long id, String username, String passwordHash,
                                             String displayName, String phone,
                                             String role, String status,
                                             Instant lastLoginAt, String lastLoginIp,
                                             Instant createdAt, Instant updatedAt) {
        SysUserEntity e = new SysUserEntity();
        e.id = id;
        e.username = username;
        e.passwordHash = passwordHash;
        e.displayName = displayName;
        e.phone = phone;
        e.role = role != null ? UserRole.valueOf(role) : UserRole.FAMILY;
        e.status = status != null ? UserStatus.valueOf(status) : UserStatus.NORMAL;
        e.lastLoginAt = lastLoginAt;
        e.lastLoginIp = lastLoginIp;
        e.createdAt = createdAt;
        e.updatedAt = updatedAt;
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
