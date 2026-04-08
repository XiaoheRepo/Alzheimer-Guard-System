package com.xiaohelab.guard.server.infrastructure.persistence.repository;

import com.xiaohelab.guard.server.domain.tag.entity.TagApplyRecordEntity;
import com.xiaohelab.guard.server.domain.tag.repository.TagApplyRecordRepository;
import com.xiaohelab.guard.server.infrastructure.persistence.do_.TagApplyRecordDO;
import com.xiaohelab.guard.server.infrastructure.persistence.mapper.TagApplyRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class TagApplyRecordRepositoryImpl implements TagApplyRecordRepository {

    private final TagApplyRecordMapper mapper;

    @Override
    public Optional<TagApplyRecordEntity> findById(Long id) {
        TagApplyRecordDO d = mapper.findById(id);
        return Optional.ofNullable(d).map(TagApplyRecordEntity::fromDO);
    }

    @Override
    public Optional<TagApplyRecordEntity> findByOrderNo(String orderNo) {
        TagApplyRecordDO d = mapper.findByOrderNo(orderNo);
        return Optional.ofNullable(d).map(TagApplyRecordEntity::fromDO);
    }

    @Override
    public Optional<TagApplyRecordEntity> findOpenByPatientId(Long patientId) {
        TagApplyRecordDO d = mapper.findOpenByPatientId(patientId);
        return Optional.ofNullable(d).map(TagApplyRecordEntity::fromDO);
    }

    @Override
    public Optional<TagApplyRecordEntity> findByResourceToken(String token) {
        TagApplyRecordDO d = mapper.findByResourceToken(token);
        return Optional.ofNullable(d).map(TagApplyRecordEntity::fromDO);
    }

    @Override
    public List<TagApplyRecordEntity> listByStatus(String status, int limit, int offset) {
        return mapper.listByStatus(status, limit, offset).stream()
                .map(TagApplyRecordEntity::fromDO)
                .collect(Collectors.toList());
    }

    @Override
    public long countByStatus(String status) {
        return mapper.countByStatus(status);
    }

    @Override
    public List<TagApplyRecordEntity> listByApplicant(Long applicantUserId, int limit, int offset) {
        return mapper.listByApplicant(applicantUserId, limit, offset).stream()
                .map(TagApplyRecordEntity::fromDO)
                .collect(Collectors.toList());
    }

    @Override
    public long countByApplicant(Long applicantUserId) {
        return mapper.countByApplicant(applicantUserId);
    }

    @Override
    public void insert(TagApplyRecordEntity entity) {
        mapper.insert(entity.toDO());
    }

    @Override
    public void update(TagApplyRecordEntity entity) {
        mapper.update(entity.toDO());
    }
}
