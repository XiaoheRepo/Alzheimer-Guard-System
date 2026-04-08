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
        return Optional.ofNullable(d).map(this::toEntity);
    }

    @Override
    public Optional<TagAssetEntity> findByTagCode(String tagCode) {
        TagAssetDO d = mapper.findByTagCode(tagCode);
        return Optional.ofNullable(d).map(this::toEntity);
    }

    @Override
    public Optional<TagAssetEntity> findBoundByPatientId(Long patientId) {
        TagAssetDO d = mapper.findBoundByPatientId(patientId);
        return Optional.ofNullable(d).map(this::toEntity);
    }

    @Override
    public List<TagAssetEntity> listUnbound(int limit, int offset) {
        return mapper.listUnbound(limit, offset).stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
    }

    @Override
    public long countUnbound() {
        return mapper.countUnbound();
    }

    @Override
    public List<TagAssetEntity> listByFilter(String status, Long patientId, int limit, int offset) {
        return mapper.listByFilter(status, patientId, limit, offset).stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
    }

    @Override
    public long countByFilter(String status, Long patientId) {
        return mapper.countByFilter(status, patientId);
    }

    @Override
    public void insert(TagAssetEntity entity) {
        mapper.insert(toDO(entity));
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

    /** DO → Entity 转换 */
    private TagAssetEntity toEntity(TagAssetDO d) {
        return TagAssetEntity.reconstitute(
                d.getId(), d.getTagCode(), d.getTagType(), d.getStatus(),
                d.getPatientId(), d.getApplyRecordId(), d.getImportBatchNo(), d.getVoidReason(),
                d.getLostAt(), d.getVoidAt(), d.getResetAt(), d.getRecoveredAt(),
                d.getCreatedAt(), d.getUpdatedAt());
    }

    /** Entity → DO 转换 */
    private TagAssetDO toDO(TagAssetEntity e) {
        TagAssetDO d = new TagAssetDO();
        d.setId(e.getId());
        d.setTagCode(e.getTagCode());
        d.setTagType(e.getTagType());
        d.setStatus(e.getStatus());
        d.setPatientId(e.getPatientId());
        d.setApplyRecordId(e.getApplyRecordId());
        d.setImportBatchNo(e.getImportBatchNo());
        d.setVoidReason(e.getVoidReason());
        d.setLostAt(e.getLostAt());
        d.setVoidAt(e.getVoidAt());
        d.setResetAt(e.getResetAt());
        d.setRecoveredAt(e.getRecoveredAt());
        return d;
    }
}
