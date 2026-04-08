package com.xiaohelab.guard.server.infrastructure.persistence.repository;

import com.xiaohelab.guard.server.domain.tag.entity.TagAssetEntity;
import com.xiaohelab.guard.server.domain.tag.repository.TagAssetRepository;
import com.xiaohelab.guard.server.infrastructure.persistence.do_.TagAssetDO;
import com.xiaohelab.guard.server.infrastructure.persistence.mapper.TagAssetMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class TagAssetRepositoryImpl implements TagAssetRepository {

    private final TagAssetMapper mapper;

    @Override
    public Optional<TagAssetEntity> findById(Long id) {
        TagAssetDO d = mapper.findById(id);
        return Optional.ofNullable(d).map(TagAssetEntity::fromDO);
    }

    @Override
    public Optional<TagAssetEntity> findByTagCode(String tagCode) {
        TagAssetDO d = mapper.findByTagCode(tagCode);
        return Optional.ofNullable(d).map(TagAssetEntity::fromDO);
    }

    @Override
    public Optional<TagAssetEntity> findBoundByPatientId(Long patientId) {
        TagAssetDO d = mapper.findBoundByPatientId(patientId);
        return Optional.ofNullable(d).map(TagAssetEntity::fromDO);
    }

    @Override
    public List<TagAssetEntity> listUnbound(int limit, int offset) {
        return mapper.listUnbound(limit, offset).stream()
                .map(TagAssetEntity::fromDO)
                .collect(Collectors.toList());
    }

    @Override
    public long countUnbound() {
        return mapper.countUnbound();
    }

    @Override
    public List<TagAssetEntity> listByFilter(String status, Long patientId, int limit, int offset) {
        return mapper.listByFilter(status, patientId, limit, offset).stream()
                .map(TagAssetEntity::fromDO)
                .collect(Collectors.toList());
    }

    @Override
    public long countByFilter(String status, Long patientId) {
        return mapper.countByFilter(status, patientId);
    }

    @Override
    public void insert(TagAssetEntity entity) {
        mapper.insert(entity.toDO());
    }

    @Override
    public int allocate(Long id, Long applyRecordId) {
        return mapper.allocate(id, applyRecordId);
    }

    @Override
    public int bindToPatient(Long id, Long patientId) {
        return mapper.bindToPatient(id, patientId);
    }

    @Override
    public int markLost(Long id) {
        return mapper.markLost(id);
    }

    @Override
    public int voidTag(Long id, String voidReason) {
        return mapper.voidTag(id, voidReason);
    }

    @Override
    public int resetTag(Long id) {
        return mapper.resetTag(id);
    }

    @Override
    public int recover(Long id) {
        return mapper.recover(id);
    }

    @Override
    public int releaseByTagCode(String tagCode) {
        return mapper.releaseByTagCode(tagCode);
    }
}
