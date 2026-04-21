package com.xiaohelab.guard.server.gov.repository;

import com.xiaohelab.guard.server.gov.entity.SysLogEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;

public interface SysLogRepository extends JpaRepository<SysLogEntity, Long> {

    Page<SysLogEntity> findByModuleOrderByCreatedAtDesc(String module, Pageable pageable);

    Page<SysLogEntity> findByOperatorUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    @Query("select l from SysLogEntity l where (:module is null or l.module = :module) " +
            "and (:action is null or l.action = :action) " +
            "and (:cursor is null or l.id < :cursor) order by l.id desc")
    List<SysLogEntity> findCursor(@Param("module") String module,
                                   @Param("action") String action,
                                   @Param("cursor") Long cursor,
                                   Pageable pageable);

    List<SysLogEntity> findByCreatedAtBetween(OffsetDateTime from, OffsetDateTime to);
}
