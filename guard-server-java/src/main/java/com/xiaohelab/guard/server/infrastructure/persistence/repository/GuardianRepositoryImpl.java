package com.xiaohelab.guard.server.infrastructure.persistence.repository;

import com.xiaohelab.guard.server.domain.guardian.entity.GuardianRelationEntity;
import com.xiaohelab.guard.server.domain.guardian.repository.GuardianRepository;
import com.xiaohelab.guard.server.infrastructure.persistence.do_.SysUserPatientDO;
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
        return Optional.ofNullable(d).map(this::toEntity);
    }

    @Override
    public Optional<GuardianRelationEntity> findByUserIdAndPatientId(Long userId, Long patientId) {
        var d = sysUserPatientMapper.findByUserIdAndPatientId(userId, patientId);
        return Optional.ofNullable(d).map(this::toEntity);
    }

    @Override
    public Optional<GuardianRelationEntity> findPrimaryByPatientId(Long patientId) {
        var d = sysUserPatientMapper.findPrimaryByPatientId(patientId);
        return Optional.ofNullable(d).map(this::toEntity);
    }

    @Override
    public Optional<GuardianRelationEntity> findByTransferRequestId(String transferRequestId) {
        var d = sysUserPatientMapper.findByTransferRequestId(transferRequestId);
        return Optional.ofNullable(d).map(this::toEntity);
    }

    @Override
    public List<GuardianRelationEntity> listActiveByPatientId(Long patientId) {
        return sysUserPatientMapper.listActiveByPatientId(patientId)
                .stream().map(this::toEntity).toList();
    }

    @Override
    public List<GuardianRelationEntity> listByUserId(Long userId, int limit, int offset) {
        return sysUserPatientMapper.listByUserId(userId, limit, offset)
                .stream().map(this::toEntity).toList();
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
        sysUserPatientMapper.insert(toDO(entity));
    }

    @Override
    public int updateRelationStatus(Long id, String relationStatus) {
        return sysUserPatientMapper.updateRelationStatus(id, relationStatus);
    }

    @Override
    public int initiateTransfer(GuardianRelationEntity entity) {
        return sysUserPatientMapper.initiateTransfer(toDO(entity));
    }

    @Override
    public int updateTransferState(GuardianRelationEntity entity) {
        return sysUserPatientMapper.updateTransferState(toDO(entity));
    }

    @Override
    public int updateRole(Long id, String relationRole) {
        return sysUserPatientMapper.updateRole(id, relationRole);
    }

    @Override
    public List<GuardianRelationEntity> listTransfersByPatientId(Long patientId, String state, int limit, int offset) {
        return sysUserPatientMapper.listTransfersByPatientId(patientId, state, limit, offset)
                .stream().map(this::toEntity).toList();
    }

    @Override
    public long countTransfersByPatientId(Long patientId, String state) {
        return sysUserPatientMapper.countTransfersByPatientId(patientId, state);
    }

    /** DO → Entity 转换 */
    private GuardianRelationEntity toEntity(SysUserPatientDO d) {
        return GuardianRelationEntity.reconstitute(
                d.getId(), d.getUserId(), d.getPatientId(), d.getRelationRole(), d.getRelationStatus(),
                d.getTransferState(), d.getTransferRequestId(), d.getTransferTargetUserId(),
                d.getTransferRequestedBy(), d.getTransferRequestedAt(), d.getTransferReason(),
                d.getTransferCancelledBy(), d.getTransferCancelledAt(), d.getTransferCancelReason(),
                d.getTransferExpireAt(), d.getTransferConfirmedAt(),
                d.getTransferRejectedAt(), d.getTransferRejectReason(),
                d.getCreatedAt(), d.getUpdatedAt());
    }

    /** Entity → DO 转换 */
    private SysUserPatientDO toDO(GuardianRelationEntity e) {
        SysUserPatientDO d = new SysUserPatientDO();
        d.setId(e.getId());
        d.setUserId(e.getUserId());
        d.setPatientId(e.getPatientId());
        d.setRelationRole(e.getRelationRole());
        d.setRelationStatus(e.getRelationStatus());
        d.setTransferState(e.getTransferState());
        d.setTransferRequestId(e.getTransferRequestId());
        d.setTransferTargetUserId(e.getTransferTargetUserId());
        d.setTransferRequestedBy(e.getTransferRequestedBy());
        d.setTransferRequestedAt(e.getTransferRequestedAt());
        d.setTransferReason(e.getTransferReason());
        d.setTransferCancelledBy(e.getTransferCancelledBy());
        d.setTransferCancelledAt(e.getTransferCancelledAt());
        d.setTransferCancelReason(e.getTransferCancelReason());
        d.setTransferExpireAt(e.getTransferExpireAt());
        d.setTransferConfirmedAt(e.getTransferConfirmedAt());
        d.setTransferRejectedAt(e.getTransferRejectedAt());
        d.setTransferRejectReason(e.getTransferRejectReason());
        return d;
    }
}
