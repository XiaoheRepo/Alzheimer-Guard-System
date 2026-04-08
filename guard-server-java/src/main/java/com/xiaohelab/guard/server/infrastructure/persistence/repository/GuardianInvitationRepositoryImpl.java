package com.xiaohelab.guard.server.infrastructure.persistence.repository;

import com.xiaohelab.guard.server.domain.guardian.entity.GuardianInvitationEntity;
import com.xiaohelab.guard.server.domain.guardian.repository.GuardianInvitationRepository;
import com.xiaohelab.guard.server.infrastructure.persistence.do_.GuardianInvitationDO;
import com.xiaohelab.guard.server.infrastructure.persistence.mapper.GuardianInvitationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * GuardianInvitationRepository 基础设施实现：封装 GuardianInvitationMapper。
 */
@Repository
@RequiredArgsConstructor
public class GuardianInvitationRepositoryImpl implements GuardianInvitationRepository {

    private final GuardianInvitationMapper guardianInvitationMapper;

    @Override
    public Optional<GuardianInvitationEntity> findById(Long id) {
        var d = guardianInvitationMapper.findById(id);
        return Optional.ofNullable(d).map(this::toEntity);
    }

    @Override
    public Optional<GuardianInvitationEntity> findByInviteId(String inviteId) {
        var d = guardianInvitationMapper.findByInviteId(inviteId);
        return Optional.ofNullable(d).map(this::toEntity);
    }

    @Override
    public Optional<GuardianInvitationEntity> findPendingByPatientAndInvitee(Long patientId, Long inviteeUserId) {
        var d = guardianInvitationMapper.findPendingByPatientAndInvitee(patientId, inviteeUserId);
        return Optional.ofNullable(d).map(this::toEntity);
    }

    @Override
    public List<GuardianInvitationEntity> listByPatient(Long patientId, int limit, int offset) {
        return guardianInvitationMapper.listByPatient(patientId, limit, offset)
                .stream().map(this::toEntity).toList();
    }

    @Override
    public long countByPatient(Long patientId) {
        return guardianInvitationMapper.countByPatient(patientId);
    }

    @Override
    public List<GuardianInvitationEntity> listPendingByInvitee(Long inviteeUserId, int limit, int offset) {
        return guardianInvitationMapper.listPendingByInvitee(inviteeUserId, limit, offset)
                .stream().map(this::toEntity).toList();
    }

    @Override
    public void insert(GuardianInvitationEntity entity) {
        var d = toDO(entity);
        guardianInvitationMapper.insert(d);
    }

    @Override
    public int respond(GuardianInvitationEntity entity) {
        return guardianInvitationMapper.respond(toDO(entity));
    }

    @Override
    public int revoke(Long id) {
        return guardianInvitationMapper.revoke(id);
    }

    @Override
    public int expirePending() {
        return guardianInvitationMapper.expirePending();
    }

    /** DO → Entity 转换 */
    private GuardianInvitationEntity toEntity(GuardianInvitationDO d) {
        return GuardianInvitationEntity.reconstitute(
                d.getId(), d.getInviteId(), d.getPatientId(), d.getInviterUserId(), d.getInviteeUserId(),
                d.getRelationRole(), d.getStatus(), d.getReason(), d.getRejectReason(),
                d.getExpireAt(), d.getAcceptedAt(), d.getRejectedAt(), d.getRevokedAt(),
                d.getCreatedAt(), d.getUpdatedAt());
    }

    /** Entity → DO 转换 */
    private GuardianInvitationDO toDO(GuardianInvitationEntity e) {
        GuardianInvitationDO d = new GuardianInvitationDO();
        d.setId(e.getId());
        d.setInviteId(e.getInviteId());
        d.setPatientId(e.getPatientId());
        d.setInviterUserId(e.getInviterUserId());
        d.setInviteeUserId(e.getInviteeUserId());
        d.setRelationRole(e.getRelationRole());
        d.setStatus(e.getStatus());
        d.setReason(e.getReason());
        d.setRejectReason(e.getRejectReason());
        d.setExpireAt(e.getExpireAt());
        d.setAcceptedAt(e.getAcceptedAt());
        d.setRejectedAt(e.getRejectedAt());
        d.setRevokedAt(e.getRevokedAt());
        return d;
    }
}
