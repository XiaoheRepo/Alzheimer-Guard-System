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
     * <p>改为 nativeQuery + CAST 以规避 Hibernate 6 + PostgreSQL 在 {@code (:x is null or ...)} 模式下
     * 对 null 参数无法推断类型的 Bug。</p>
     */
    @Query(nativeQuery = true,
            value = "SELECT * FROM ai_session s WHERE s.user_id = :uid " +
                    "  AND (CAST(:patientId AS bigint) IS NULL OR s.patient_id = CAST(:patientId AS bigint)) " +
                    "  AND (CAST(:taskId    AS bigint) IS NULL OR s.task_id    = CAST(:taskId    AS bigint)) " +
                    "  AND (CAST(:status    AS text)   IS NULL OR s.status     = CAST(:status    AS text)) " +
                    "  AND (CAST(:cursorId  AS bigint) IS NULL OR s.id         < CAST(:cursorId  AS bigint))",
            countQuery = "SELECT count(*) FROM ai_session s WHERE s.user_id = :uid " +
                    "  AND (CAST(:patientId AS bigint) IS NULL OR s.patient_id = CAST(:patientId AS bigint)) " +
                    "  AND (CAST(:taskId    AS bigint) IS NULL OR s.task_id    = CAST(:taskId    AS bigint)) " +
                    "  AND (CAST(:status    AS text)   IS NULL OR s.status     = CAST(:status    AS text)) " +
                    "  AND (CAST(:cursorId  AS bigint) IS NULL OR s.id         < CAST(:cursorId  AS bigint))")
    Page<AiSessionEntity> findForList(@Param("uid") Long userId,
                                      @Param("patientId") Long patientId,
                                      @Param("taskId") Long taskId,
                                      @Param("status") String status,
                                      @Param("cursorId") Long cursorId,
                                      Pageable pageable);
}

