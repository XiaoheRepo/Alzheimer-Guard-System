package com.xiaohelab.guard.server.clue.repository;

import com.xiaohelab.guard.server.clue.entity.PatientTrajectoryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface PatientTrajectoryRepository extends JpaRepository<PatientTrajectoryEntity, Long> {

    List<PatientTrajectoryEntity> findByTaskIdOrderByWindowStartAsc(Long taskId);

    List<PatientTrajectoryEntity> findByPatientIdAndWindowStartBetweenOrderByWindowStartAsc(
            Long patientId, OffsetDateTime from, OffsetDateTime to);

    /**
     * 任务下最新轨迹点（cursor 游标：按 id 倒序；since / afterId 任一条件满足即可增量拉取）。
     * 由 {@code since} 或 {@code afterId} 至少一项过滤；不传则从最新开始。
     * <p>改为 nativeQuery + CAST 以规避 Hibernate 6 + PostgreSQL 对可空参数的类型推断 Bug。</p>
     */
    @Query(nativeQuery = true,
            value = "SELECT * FROM patient_trajectory t WHERE t.task_id = :taskId " +
                    "  AND (CAST(:since   AS timestamptz) IS NULL OR t.window_start > CAST(:since   AS timestamptz)) " +
                    "  AND (CAST(:afterId AS bigint)      IS NULL OR t.id           > CAST(:afterId AS bigint))",
            countQuery = "SELECT count(*) FROM patient_trajectory t WHERE t.task_id = :taskId " +
                    "  AND (CAST(:since   AS timestamptz) IS NULL OR t.window_start > CAST(:since   AS timestamptz)) " +
                    "  AND (CAST(:afterId AS bigint)      IS NULL OR t.id           > CAST(:afterId AS bigint))")
    Page<PatientTrajectoryEntity> findLatestForTask(@Param("taskId") Long taskId,
                                                    @Param("since") OffsetDateTime since,
                                                    @Param("afterId") Long afterId,
                                                    Pageable pageable);

    /** 取任务最新一条轨迹点用于快照聚合。 */
    Optional<PatientTrajectoryEntity> findTopByTaskIdOrderByIdDesc(Long taskId);

    /** 任务下轨迹点总数，用于快照聚合。 */
    long countByTaskId(Long taskId);

    /** 任务下经纬度边界聚合（用于 snapshot bounding_box）。 */
    @Query("select min(t.latitude), max(t.latitude), min(t.longitude), max(t.longitude) " +
            "from PatientTrajectoryEntity t where t.taskId = :taskId and t.latitude is not null")
    Object[] aggBoundingBoxByTask(@Param("taskId") Long taskId);
}

