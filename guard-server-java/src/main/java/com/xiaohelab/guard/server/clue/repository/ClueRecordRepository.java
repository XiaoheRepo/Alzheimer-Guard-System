package com.xiaohelab.guard.server.clue.repository;

import com.xiaohelab.guard.server.clue.entity.ClueRecordEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClueRecordRepository extends JpaRepository<ClueRecordEntity, Long> {

    Page<ClueRecordEntity> findByTaskIdOrderByCreatedAtDesc(Long taskId, Pageable pageable);

    Page<ClueRecordEntity> findByPatientIdOrderByCreatedAtDesc(Long patientId, Pageable pageable);

    Page<ClueRecordEntity> findByReviewStatusOrderByCreatedAtAsc(String reviewStatus, Pageable pageable);

    List<ClueRecordEntity> findTop200ByTaskIdOrderByCreatedAtDesc(Long taskId);

    List<ClueRecordEntity> findTop200ByPatientIdOrderByCreatedAtDesc(Long patientId);
}
