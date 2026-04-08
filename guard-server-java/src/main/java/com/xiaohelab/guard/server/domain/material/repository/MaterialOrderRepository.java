package com.xiaohelab.guard.server.domain.material.repository;

import com.xiaohelab.guard.server.domain.tag.entity.TagApplyRecordEntity;

import java.util.List;
import java.util.Optional;

/**
 * 物资工单 Repository 接口（物资领域视角）。
 * 对应 tag_apply_record 表，由 TagApplyRecordRepositoryImpl 同时实现此接口。
 * 物资领域通过此接口隔离对 tag 持久化实现的直接依赖。
 */
public interface MaterialOrderRepository {

    Optional<TagApplyRecordEntity> findById(Long id);

    Optional<TagApplyRecordEntity> findOpenByPatientId(Long patientId);

    Optional<TagApplyRecordEntity> findByResourceToken(String token);

    List<TagApplyRecordEntity> listByStatus(String status, int limit, int offset);

    long countByStatus(String status);

    List<TagApplyRecordEntity> listByApplicant(Long applicantUserId, int limit, int offset);

    long countByApplicant(Long applicantUserId);

    void insert(TagApplyRecordEntity entity);

    void update(TagApplyRecordEntity entity);
}
