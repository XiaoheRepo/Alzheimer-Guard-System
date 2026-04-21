package com.xiaohelab.guard.server.outbox.repository;

import com.xiaohelab.guard.server.outbox.entity.OutboxLogEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface OutboxLogRepository extends JpaRepository<OutboxLogEntity, Long> {

    Optional<OutboxLogEntity> findByEventId(String eventId);

    @Query(value = "SELECT * FROM sys_outbox_log " +
            " WHERE phase IN ('PENDING','RETRY') " +
            "   AND (next_retry_at IS NULL OR next_retry_at <= now()) " +
            " ORDER BY created_at ASC LIMIT :limit FOR UPDATE SKIP LOCKED",
            nativeQuery = true)
    List<OutboxLogEntity> claimPending(@Param("limit") int limit);

    @Modifying
    @Query("update OutboxLogEntity o set o.phase = :phase, o.sentAt = :at where o.id = :id")
    int markSent(@Param("id") Long id, @Param("phase") String phase, @Param("at") OffsetDateTime at);

    Page<OutboxLogEntity> findByPhaseOrderByUpdatedAtDesc(String phase, Pageable pageable);
}
