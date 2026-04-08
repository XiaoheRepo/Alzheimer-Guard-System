package com.xiaohelab.guard.server.infrastructure.persistence.repository;

import com.xiaohelab.guard.server.domain.guardian.entity.GuardianInvitationEntity;
import com.xiaohelab.guard.server.domain.guardian.repository.GuardianInvitationRepository;
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
        return Optional.ofNullable(d).map(GuardianInvitationEntity::fromDO);
    }

    @Override
    public Optional<GuardianInvitationEntity> findByInviteId(String inviteId) {
        var d = guardianInvitationMapper.findByInviteId(inviteId);
        return Optional.ofNullable(d).map(GuardianInvitationEntity::fromDO);
    }

    @Override
    public Optional<GuardianInvitationEntity> findPendingByPatientAndInvitee(Long patientId, Long inviteeUserId) {
        var d = guardianInvitationMapper.findPendingByPatientAndInvitee(patientId, inviteeUserId);
        return Optional.ofNullable(d).map(GuardianInvitationEntity::fromDO);
    }

    @Override
    public List<GuardianInvitationEntity> listByPatient(Long patientId, int limit, int offset) {
        return guardianInvitationMapper.listByPatient(patientId, limit, offset)
                .stream().map(GuardianInvitationEntity::fromDO).toList();
    }

    @Override
    public long countByPatient(Long patientId) {
        return guardianInvitationMapper.countByPatient(patientId);
    }

    @Override
    public List<GuardianInvitationEntity> listPendingByInvitee(Long inviteeUserId, int limit, int offset) {
        return guardianInvitationMapper.listPendingByInvitee(inviteeUserId, limit, offset)
                .stream().map(GuardianInvitationEntity::fromDO).toList();
    }

    @Override
    public void insert(GuardianInvitationEntity entity) {
        var d = entity.toDO();
        guardianInvitationMapper.insert(d);
        // MyBatis useGeneratedKeys 回填 id — entity 此后不缓存 id（无需回写，调用方若需要 id 重新查询）
    }

    @Override
    public int respond(GuardianInvitationEntity entity) {
        return guardianInvitationMapper.respond(entity.toDO());
    }

    @Override
    public int revoke(Long id) {
        return guardianInvitationMapper.revoke(id);
    }

    @Override
    public int expirePending() {
        return guardianInvitationMapper.expirePending();
    }
}
