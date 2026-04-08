package com.xiaohelab.guard.server.infrastructure.persistence.repository;

import com.xiaohelab.guard.server.common.exception.BizException;
import com.xiaohelab.guard.server.domain.clue.entity.ClueRecordEntity;
import com.xiaohelab.guard.server.domain.clue.repository.ClueRepository;
import com.xiaohelab.guard.server.infrastructure.persistence.do_.ClueRecordDO;
import com.xiaohelab.guard.server.infrastructure.persistence.mapper.ClueRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class ClueRepositoryImpl implements ClueRepository {

    private final ClueRecordMapper mapper;

    @Override
    public Optional<ClueRecordEntity> findById(Long id) {
        ClueRecordDO d = mapper.findByIdFull(id);
        return Optional.ofNullable(d).map(ClueRecordEntity::fromDO);
    }

    @Override
    public Optional<ClueRecordEntity> findByClueNo(String clueNo) {
        ClueRecordDO d = mapper.findByClueNo(clueNo);
        return Optional.ofNullable(d).map(ClueRecordEntity::fromDO);
    }

    @Override
    public List<ClueRecordEntity> listByTaskId(Long taskId, int limit, int offset) {
        return mapper.listByTaskId(taskId, limit, offset).stream()
                .map(ClueRecordEntity::fromDO)
                .collect(Collectors.toList());
    }

    @Override
    public long countByTaskId(Long taskId) {
        return mapper.countByTaskId(taskId);
    }

    @Override
    public List<ClueRecordEntity> listPendingByTaskId(Long taskId) {
        return mapper.listPendingByTaskId(taskId).stream()
                .map(ClueRecordEntity::fromDO)
                .collect(Collectors.toList());
    }

    @Override
    public List<ClueRecordEntity> listReviewQueue(int limit, int offset) {
        return mapper.listReviewQueue(limit, offset).stream()
                .map(ClueRecordEntity::fromDO)
                .collect(Collectors.toList());
    }

    @Override
    public long countReviewQueue() {
        return mapper.countReviewQueue();
    }

    @Override
    public Optional<ClueRecordEntity> findByIdFull(Long id) {
        ClueRecordDO d = mapper.findByIdFull(id);
        return Optional.ofNullable(d).map(ClueRecordEntity::fromDO);
    }

    @Override
    public ClueRecordEntity findByIdOrThrow(Long id) {
        return findByIdFull(id).orElseThrow(() -> BizException.of("E_CLUE_4043"));
    }

    @Override
    public void insert(ClueRecordEntity entity) {
        mapper.insert(entity.toDO());
    }

    @Override
    public int assign(Long clueId, Long assigneeUserId) {
        ClueRecordDO d = new ClueRecordDO();
        d.setId(clueId);
        d.setAssigneeUserId(assigneeUserId);
        return mapper.assign(d);
    }

    @Override
    public int override(Long clueId, Long overrideBy, String overrideReason) {
        ClueRecordDO d = new ClueRecordDO();
        d.setId(clueId);
        d.setOverrideBy(overrideBy);
        d.setOverrideReason(overrideReason);
        return mapper.override(d);
    }

    @Override
    public int reject(Long clueId, Long rejectedBy, String rejectReason) {
        ClueRecordDO d = new ClueRecordDO();
        d.setId(clueId);
        d.setRejectedBy(rejectedBy);
        d.setRejectReason(rejectReason);
        return mapper.reject(d);
    }

    @Override
    public List<ClueRecordEntity> listSuspected(String reviewStatus, Long taskId,
                                                 Long patientId, int limit, int offset) {
        return mapper.listSuspected(reviewStatus, taskId, patientId, limit, offset).stream()
                .map(ClueRecordEntity::fromDO)
                .collect(Collectors.toList());
    }

    @Override
    public long countSuspectedFiltered(String reviewStatus, Long taskId, Long patientId) {
        return mapper.countSuspectedFiltered(reviewStatus, taskId, patientId);
    }

    @Override
    public long countAll(String timeFrom, String timeTo) {
        return mapper.countAll(timeFrom, timeTo);
    }

    @Override
    public long countSuspected(String timeFrom, String timeTo) {
        return mapper.countSuspected(timeFrom, timeTo);
    }

    @Override
    public long countOverridden(String timeFrom, String timeTo) {
        return mapper.countOverridden(timeFrom, timeTo);
    }

    @Override
    public long countRejected(String timeFrom, String timeTo) {
        return mapper.countRejected(timeFrom, timeTo);
    }
}
