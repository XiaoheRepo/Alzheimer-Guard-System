package com.xiaohelab.guard.server.clue.repository;

import com.xiaohelab.guard.server.clue.entity.ClueRecordEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface ClueRecordRepository extends JpaRepository<ClueRecordEntity, Long> {

    Page<ClueRecordEntity> findByTaskIdOrderByCreatedAtDesc(Long taskId, Pageable pageable);

    Page<ClueRecordEntity> findByPatientIdOrderByCreatedAtDesc(Long patientId, Pageable pageable);

    Page<ClueRecordEntity> findByReviewStatusOrderByCreatedAtAsc(String reviewStatus, Pageable pageable);

    List<ClueRecordEntity> findTop200ByTaskIdOrderByCreatedAtDesc(Long taskId);

    List<ClueRecordEntity> findTop200ByPatientIdOrderByCreatedAtDesc(Long patientId);

    /** 任务下线索总数（snapshot 聚合）。 */
    long countByTaskId(Long taskId);

    /** 任务下有效线索数（review_status = VALID）。 */
    long countByTaskIdAndReviewStatus(Long taskId, String reviewStatus);

    /** 任务下可疑线索数（suspect_flag = true）。 */
    long countByTaskIdAndSuspectFlag(Long taskId, Boolean suspectFlag);

    /** 最近一条线索创建时间（snapshot）。 */
    @Query("select max(c.createdAt) from ClueRecordEntity c where c.taskId = :taskId")
    Optional<OffsetDateTime> findLatestClueTimeByTask(@Param("taskId") Long taskId);
}
