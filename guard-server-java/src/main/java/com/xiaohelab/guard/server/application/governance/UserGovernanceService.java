package com.xiaohelab.guard.server.application.governance;

import com.xiaohelab.guard.server.common.exception.BizException;
import com.xiaohelab.guard.server.domain.governance.entity.SysUserEntity;
import com.xiaohelab.guard.server.domain.governance.repository.SysUserRepository;
import com.xiaohelab.guard.server.infrastructure.persistence.do_.SysLogDO;
import com.xiaohelab.guard.server.infrastructure.persistence.mapper.SysLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * 用户治理应用服务（管理端）。
 * 封装用户列表查询、封禁/解封、密码强制重置等操作。
 * application 层可直接使用 SysLogMapper 写审计日志。
 */
@Service
@RequiredArgsConstructor
public class UserGovernanceService {

    private final SysUserRepository sysUserRepository;
    private final SysLogMapper sysLogMapper;

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

        Instant now = Instant.now();
        SysLogDO log = new SysLogDO();
        log.setModule("USER");
        log.setAction("RESET_PASSWORD");
        log.setActionId("reset_pwd_" + targetUserId);
        log.setObjectId(String.valueOf(targetUserId));
        log.setResultCode("OK");
        log.setResult("密码强制重置成功");
        log.setRiskLevel("HIGH");
        log.setOperatorUserId(operatorUserId);
        log.setOperatorUsername(operatorUsername);
        log.setExecutedAt(now);
        log.setCreatedAt(now);
        if (reason != null) log.setDetail(reason);
        log.setTraceId(traceId);
        sysLogMapper.insert(log);
    }
}
