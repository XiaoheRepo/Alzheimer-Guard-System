package com.xiaohelab.guard.server.ai.repository;

import com.xiaohelab.guard.server.ai.entity.AiSessionEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AiSessionRepository extends JpaRepository<AiSessionEntity, Long> {

    Optional<AiSessionEntity> findBySessionId(String sessionId);

    Page<AiSessionEntity> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<AiSessionEntity> findByPatientIdOrderByCreatedAtDesc(Long patientId, Pageable pageable);

    Optional<AiSessionEntity> findByPatientIdAndTaskIdAndStatus(Long patientId, Long taskId, String status);

    /**
     * 列表查询（V2.1 §3.8.1.1）：按 user_id 过滤，可选 patient_id / task_id / status / cursor(id)。
     * 倒序 id 分页；cursor 为 null 表示首页。
     */
    @Query("select s from AiSessionEntity s where s.userId = :uid " +
            "and (:patientId is null or s.patientId = :patientId) " +
            "and (:taskId    is null or s.taskId    = :taskId) " +
            "and (:status    is null or s.status    = :status) " +
            "and (:cursorId  is null or s.id        < :cursorId) " +
            "order by s.id desc")
    Page<AiSessionEntity> findForList(@Param("uid") Long userId,
                                      @Param("patientId") Long patientId,
                                      @Param("taskId") Long taskId,
                                      @Param("status") String status,
                                      @Param("cursorId") Long cursorId,
                                      Pageable pageable);
}

