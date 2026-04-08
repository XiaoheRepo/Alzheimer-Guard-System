package com.xiaohelab.guard.server.infrastructure.persistence.repository;

import com.xiaohelab.guard.server.domain.guardian.entity.GuardianRelationEntity;
import com.xiaohelab.guard.server.domain.guardian.repository.GuardianRepository;
import com.xiaohelab.guard.server.infrastructure.persistence.mapper.SysUserPatientMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * GuardianRepository 基础设施实现：封装 SysUserPatientMapper。
 */
@Repository
@RequiredArgsConstructor
public class GuardianRepositoryImpl implements GuardianRepository {

    private final SysUserPatientMapper sysUserPatientMapper;

    @Override
    public Optional<GuardianRelationEntity> findById(Long id) {
        var d = sysUserPatientMapper.findById(id);
        return Optional.ofNullable(d).map(GuardianRelationEntity::fromDO);
    }

    @Override
    public Optional<GuardianRelationEntity> findByUserIdAndPatientId(Long userId, Long patientId) {
        var d = sysUserPatientMapper.findByUserIdAndPatientId(userId, patientId);
        return Optional.ofNullable(d).map(GuardianRelationEntity::fromDO);
    }

    @Override
    public Optional<GuardianRelationEntity> findPrimaryByPatientId(Long patientId) {
        var d = sysUserPatientMapper.findPrimaryByPatientId(patientId);
        return Optional.ofNullable(d).map(GuardianRelationEntity::fromDO);
    }

    @Override
    public Optional<GuardianRelationEntity> findByTransferRequestId(String transferRequestId) {
        var d = sysUserPatientMapper.findByTransferRequestId(transferRequestId);
        return Optional.ofNullable(d).map(GuardianRelationEntity::fromDO);
    }

    @Override
    public List<GuardianRelationEntity> listActiveByPatientId(Long patientId) {
        return sysUserPatientMapper.listActiveByPatientId(patientId)
                .stream().map(GuardianRelationEntity::fromDO).toList();
    }

    @Override
    public List<GuardianRelationEntity> listByUserId(Long userId, int limit, int offset) {
        return sysUserPatientMapper.listByUserId(userId, limit, offset)
                .stream().map(GuardianRelationEntity::fromDO).toList();
    }

    @Override
    public long countByUserId(Long userId) {
        return sysUserPatientMapper.countByUserId(userId);
    }

    @Override
    public long countActiveRelation(Long userId, Long patientId) {
        return sysUserPatientMapper.countActiveRelation(userId, patientId);
    }

    @Override
    public void insert(GuardianRelationEntity entity) {
        var d = entity.toDO();
        sysUserPatientMapper.insert(d);
    }

    @Override
    public int updateRelationStatus(Long id, String relationStatus) {
        return sysUserPatientMapper.updateRelationStatus(id, relationStatus);
    }

    @Override
    public int initiateTransfer(GuardianRelationEntity entity) {
        return sysUserPatientMapper.initiateTransfer(entity.toDO());
    }

    @Override
    public int updateTransferState(GuardianRelationEntity entity) {
        return sysUserPatientMapper.updateTransferState(entity.toDO());
    }

    @Override
    public int updateRole(Long id, String relationRole) {
        return sysUserPatientMapper.updateRole(id, relationRole);
    }

    @Override
    public List<GuardianRelationEntity> listTransfersByPatientId(Long patientId, String state, int limit, int offset) {
        return sysUserPatientMapper.listTransfersByPatientId(patientId, state, limit, offset)
                .stream().map(GuardianRelationEntity::fromDO).toList();
    }

    @Override
    public long countTransfersByPatientId(Long patientId, String state) {
        return sysUserPatientMapper.countTransfersByPatientId(patientId, state);
    }
}
