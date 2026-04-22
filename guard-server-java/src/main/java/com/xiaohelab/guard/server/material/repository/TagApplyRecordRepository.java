package com.xiaohelab.guard.server.material.repository;

import com.xiaohelab.guard.server.material.entity.TagApplyRecordEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TagApplyRecordRepository extends JpaRepository<TagApplyRecordEntity, Long> {

    Optional<TagApplyRecordEntity> findByOrderNo(String orderNo);

    Page<TagApplyRecordEntity> findByApplicantUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<TagApplyRecordEntity> findByStatusOrderByCreatedAtAsc(String status, Pageable pageable);

    List<TagApplyRecordEntity> findByPatientIdAndStatusIn(Long patientId, List<String> statuses);

    /** 管理员治理用：申请人在给定状态集合下的工单数（含 PENDING_AUDIT / PENDING_SHIP 等未终态）。 */
    long countByApplicantUserIdAndStatusIn(Long applicantUserId, Collection<String> statuses);

    /** 物流单号全局唯一性校验（补发时防糊错，LLD §6.3.8）。 */
    Optional<TagApplyRecordEntity> findByLogisticsNo(String logisticsNo);
}
