package com.xiaohelab.guard.server.infrastructure.persistence.repository;

import com.xiaohelab.guard.server.domain.governance.entity.SysUserEntity;
import com.xiaohelab.guard.server.domain.governance.repository.SysUserRepository;
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
        return d == null ? Optional.empty() : Optional.of(SysUserEntity.fromDO(d));
    }

    @Override
    public List<SysUserEntity> listByFilter(String role, String status, String keyword,
                                            int limit, int offset) {
        return sysUserMapper.listByFilter(role, status, keyword, limit, offset)
                .stream()
                .map(SysUserEntity::fromDO)
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
}
