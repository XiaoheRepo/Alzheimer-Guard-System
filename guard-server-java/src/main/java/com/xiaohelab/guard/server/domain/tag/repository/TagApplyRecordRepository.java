package com.xiaohelab.guard.server.domain.tag.repository;

import com.xiaohelab.guard.server.domain.tag.entity.TagApplyRecordEntity;

import java.util.List;
import java.util.Optional;

/**
 * 申领工单 Repository 接口（领域层定义，基础设施层实现）。
 */
public interface TagApplyRecordRepository {

    Optional<TagApplyRecordEntity> findById(Long id);

    Optional<TagApplyRecordEntity> findByOrderNo(String orderNo);

    Optional<TagApplyRecordEntity> findOpenByPatientId(Long patientId);

    Optional<TagApplyRecordEntity> findByResourceToken(String token);

    List<TagApplyRecordEntity> listByStatus(String status, int limit, int offset);

    long countByStatus(String status);

    List<TagApplyRecordEntity> listByApplicant(Long applicantUserId, int limit, int offset);

    long countByApplicant(Long applicantUserId);

    void insert(TagApplyRecordEntity entity);

    void update(TagApplyRecordEntity entity);
}
