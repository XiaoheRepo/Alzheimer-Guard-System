package com.xiaohelab.guard.server.application.governance;

import com.xiaohelab.guard.server.common.exception.BizException;
import com.xiaohelab.guard.server.domain.governance.entity.SysLogEntity;
import com.xiaohelab.guard.server.domain.governance.entity.SysUserEntity;
import com.xiaohelab.guard.server.domain.governance.repository.SysLogRepository;
import com.xiaohelab.guard.server.domain.governance.repository.SysUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 用户治理应用服务（管理端）。
 * 封装用户列表查询、封禁/解封、密码强制重置等操作。
 */
@Service
@RequiredArgsConstructor
public class UserGovernanceService {

    private final SysUserRepository sysUserRepository;
    private final SysLogRepository sysLogRepository;

    public List<SysUserEntity> listUsers(String role, String status, String keyword,
                                         int pageSize, int offset) {
        return sysUserRepository.listByFilter(role, status, keyword, pageSize, offset);
    }

    public long countUsers(String role, String status, String keyword) {
        return sysUserRepository.countByFilter(role, status, keyword);
    }

    public SysUserEntity getUser(Long userId) {
        return sysUserRepository.findById(userId)
                .orElseThrow(() -> BizException.of("E_USER_4041"));
    }

    /** 按手机号查询用户（邀请前置查询） */
    public SysUserEntity findByPhone(String phone) {
        return sysUserRepository.findByPhone(phone)
                .orElseThrow(() -> BizException.of("E_USER_4041"));
    }

    /**
     * 更新账号状态（NORMAL / BANNED）。
     * 调用方已通过聚合根 ban()/unban() 校验状态合法性，此处仅落库。
     */
    @Transactional
    public void updateUserStatus(Long userId, String newStatus) {
        sysUserRepository.updateStatus(userId, newStatus);
    }

    /**
     * 强制重置密码。
     *
     * @param targetUserId    目标用户 ID
     * @param encodedPassword 已由调用方完成 BCrypt 哈希的新密码
     * @param operatorUserId  操作员 ID（审计用）
     * @param operatorUsername 操作员用户名快照（审计用）
     * @param reason          重置原因
     * @param traceId         链路追踪 ID
     */
    @Transactional
    public void resetPassword(Long targetUserId, String encodedPassword,
                              Long operatorUserId, String operatorUsername,
                              String reason, String traceId) {
        sysUserRepository.updatePassword(targetUserId, encodedPassword);

        SysLogEntity log = SysLogEntity.create(
                "USER", "RESET_PASSWORD", "reset_pwd_" + targetUserId,
                String.valueOf(targetUserId), "OK", "密码强制重置成功",
                "HIGH", operatorUserId, operatorUsername,
                reason, null, null, null, traceId);
        sysLogRepository.insert(log);
    }
}
