package com.xiaohelab.guard.server.infrastructure.persistence.repository;

import com.xiaohelab.guard.server.domain.governance.entity.SysUserEntity;
import com.xiaohelab.guard.server.domain.governance.repository.SysUserRepository;
import com.xiaohelab.guard.server.infrastructure.persistence.do_.SysUserDO;
import com.xiaohelab.guard.server.infrastructure.persistence.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 用户仓储实现（基础设施层）。
 * 代理 SysUserMapper，返回 domain 层 Entity。
 */
@Repository
@RequiredArgsConstructor
public class SysUserRepositoryImpl implements SysUserRepository {

    private final SysUserMapper sysUserMapper;

    @Override
    public Optional<SysUserEntity> findById(Long id) {
        var d = sysUserMapper.findById(id);
        return d == null ? Optional.empty() : Optional.of(toEntity(d));
    }

    @Override
    public Optional<SysUserEntity> findByUsername(String username) {
        var d = sysUserMapper.findByUsername(username);
        return d == null ? Optional.empty() : Optional.of(toEntity(d));
    }

    @Override
    public int countByUsername(String username) {
        return sysUserMapper.countByUsername(username);
    }

    @Override
    public SysUserEntity insert(String username, String passwordHash, String phone,
                                String displayName, String role, String status) {
        SysUserDO user = new SysUserDO();
        user.setUsername(username);
        user.setPasswordHash(passwordHash);
        user.setPhone(phone);
        user.setDisplayName(displayName);
        user.setRole(role);
        user.setStatus(status);
        sysUserMapper.insert(user);
        return toEntity(user);
    }

    @Override
    public void updateLoginInfo(Long id, String ip) {
        sysUserMapper.updateLoginInfo(id, ip);
    }

    @Override
    public List<SysUserEntity> listByFilter(String role, String status, String keyword,
                                            int limit, int offset) {
        return sysUserMapper.listByFilter(role, status, keyword, limit, offset)
                .stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
    }

    @Override
    public long countByFilter(String role, String status, String keyword) {
        return sysUserMapper.countByFilter(role, status, keyword);
    }

    @Override
    public void updateStatus(Long id, String status) {
        sysUserMapper.updateStatus(id, status);
    }

    @Override
    public void updatePassword(Long id, String passwordHash) {
        sysUserMapper.updatePassword(id, passwordHash);
    }

    @Override
    public Optional<SysUserEntity> findByPhone(String phone) {
        var d = sysUserMapper.findByPhone(phone);
        return d == null ? Optional.empty() : Optional.of(toEntity(d));
    }

    /** DO → Entity 转换（基础设施层职责） */
    private SysUserEntity toEntity(SysUserDO d) {
        return SysUserEntity.reconstitute(
                d.getId(), d.getUsername(), d.getPasswordHash(),
                d.getDisplayName(), d.getPhone(), d.getRole(), d.getStatus(),
                d.getLastLoginAt(), d.getLastLoginIp(),
                d.getCreatedAt(), d.getUpdatedAt());
    }
}
