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

    /**
     * 管理员治理用：统计由目标用户发起、且仍为 ACTIVE / SUSTAINED 态的寻回任务数。
     * <p>注销前置校验（E_USR_4093）。</p>
     */
    @Query("select count(t) from RescueTaskEntity t " +
            "where t.createdBy = :userId and t.status in ('ACTIVE','SUSTAINED')")
    long countActiveByCreator(@Param("userId") Long userId);
}
