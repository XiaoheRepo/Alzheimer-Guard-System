package com.xiaohelab.guard.server.infrastructure.persistence.repository;

import com.xiaohelab.guard.server.domain.material.repository.MaterialOrderRepository;
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
public class TagApplyRecordRepositoryImpl implements TagApplyRecordRepository, MaterialOrderRepository {

    private final TagApplyRecordMapper mapper;

    @Override
    public Optional<TagApplyRecordEntity> findById(Long id) {
        TagApplyRecordDO d = mapper.findById(id);
        return Optional.ofNullable(d).map(this::toEntity);
    }

    @Override
    public Optional<TagApplyRecordEntity> findByOrderNo(String orderNo) {
        TagApplyRecordDO d = mapper.findByOrderNo(orderNo);
        return Optional.ofNullable(d).map(this::toEntity);
    }

    @Override
    public Optional<TagApplyRecordEntity> findOpenByPatientId(Long patientId) {
        TagApplyRecordDO d = mapper.findOpenByPatientId(patientId);
        return Optional.ofNullable(d).map(this::toEntity);
    }

    @Override
    public Optional<TagApplyRecordEntity> findByResourceToken(String token) {
        TagApplyRecordDO d = mapper.findByResourceToken(token);
        return Optional.ofNullable(d).map(this::toEntity);
    }

    @Override
    public List<TagApplyRecordEntity> listByStatus(String status, int limit, int offset) {
        return mapper.listByStatus(status, limit, offset).stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
    }

    @Override
    public long countByStatus(String status) {
        return mapper.countByStatus(status);
    }

    @Override
    public List<TagApplyRecordEntity> listByApplicant(Long applicantUserId, int limit, int offset) {
        return mapper.listByApplicant(applicantUserId, limit, offset).stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
    }

    @Override
    public long countByApplicant(Long applicantUserId) {
        return mapper.countByApplicant(applicantUserId);
    }

    @Override
    public void insert(TagApplyRecordEntity entity) {
        mapper.insert(toDO(entity));
    }

    @Override
    public void update(TagApplyRecordEntity entity) {
        mapper.update(toDO(entity));
    }

    /** DO → Entity 转换 */
    private TagApplyRecordEntity toEntity(TagApplyRecordDO d) {
        return TagApplyRecordEntity.reconstitute(
                d.getId(), d.getOrderNo(), d.getPatientId(), d.getApplicantUserId(),
                d.getQuantity(), d.getApplyNote(), d.getTagCode(), d.getStatus(),
                d.getDeliveryAddress(), d.getTrackingNumber(), d.getCourierName(), d.getResourceLink(),
                d.getCancelReason(), d.getApprovedAt(), d.getRejectReason(), d.getRejectedAt(),
                d.getExceptionDesc(), d.getClosedAt(), d.getCreatedAt(), d.getUpdatedAt());
    }

    /** Entity → DO 转换 */
    private TagApplyRecordDO toDO(TagApplyRecordEntity e) {
        TagApplyRecordDO d = new TagApplyRecordDO();
        d.setId(e.getId());
        d.setOrderNo(e.getOrderNo());
        d.setPatientId(e.getPatientId());
        d.setApplicantUserId(e.getApplicantUserId());
        d.setQuantity(e.getQuantity());
        d.setApplyNote(e.getApplyNote());
        d.setTagCode(e.getTagCode());
        d.setStatus(e.getStatus());
        d.setDeliveryAddress(e.getDeliveryAddress());
        d.setTrackingNumber(e.getTrackingNumber());
        d.setCourierName(e.getCourierName());
        d.setResourceLink(e.getResourceLink());
        d.setCancelReason(e.getCancelReason());
        d.setApprovedAt(e.getApprovedAt());
        d.setRejectReason(e.getRejectReason());
        d.setRejectedAt(e.getRejectedAt());
        d.setExceptionDesc(e.getExceptionDesc());
        d.setClosedAt(e.getClosedAt());
        return d;
    }
}
