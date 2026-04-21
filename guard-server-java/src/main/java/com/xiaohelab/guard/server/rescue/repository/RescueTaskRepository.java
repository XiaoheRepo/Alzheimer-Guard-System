package com.xiaohelab.guard.server.rescue.repository;

import com.xiaohelab.guard.server.rescue.entity.RescueTaskEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RescueTaskRepository extends JpaRepository<RescueTaskEntity, Long> {

    Optional<RescueTaskEntity> findByTaskNo(String taskNo);

    @Query("select t from RescueTaskEntity t where t.patientId = :pid and t.status not in ('CLOSED_FOUND','CLOSED_FALSE_ALARM')")
    Optional<RescueTaskEntity> findActiveByPatient(@Param("pid") Long patientId);

    @Query("select t from RescueTaskEntity t where t.patientId in :pids and t.status not in ('CLOSED_FOUND','CLOSED_FALSE_ALARM')")
    List<RescueTaskEntity> findActiveByPatients(@Param("pids") List<Long> patientIds);

    Page<RescueTaskEntity> findByPatientIdInOrderByCreatedAtDesc(List<Long> patientIds, Pageable pageable);

    Page<RescueTaskEntity> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
}
