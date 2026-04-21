package com.xiaohelab.guard.server.material.repository;

import com.xiaohelab.guard.server.material.entity.TagApplyRecordEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TagApplyRecordRepository extends JpaRepository<TagApplyRecordEntity, Long> {

    Optional<TagApplyRecordEntity> findByOrderNo(String orderNo);

    Page<TagApplyRecordEntity> findByApplicantUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<TagApplyRecordEntity> findByStatusOrderByCreatedAtAsc(String status, Pageable pageable);

    List<TagApplyRecordEntity> findByPatientIdAndStatusIn(Long patientId, List<String> statuses);
}
