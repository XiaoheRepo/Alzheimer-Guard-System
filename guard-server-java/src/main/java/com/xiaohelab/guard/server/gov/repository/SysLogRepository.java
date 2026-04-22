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

    /**
     * 审计日志游标分页（旧 API 兼容）。
     * <p>改为 nativeQuery + CAST 以规避 Hibernate 6 + PostgreSQL 对可空参数的类型推断 Bug。</p>
     */
    @Query(nativeQuery = true,
            value = "SELECT * FROM sys_log l " +
                    "WHERE (CAST(:module AS text)   IS NULL OR l.module = CAST(:module AS text)) " +
                    "  AND (CAST(:action AS text)   IS NULL OR l.action = CAST(:action AS text)) " +
                    "  AND (CAST(:cursor AS bigint) IS NULL OR l.id     < CAST(:cursor AS bigint)) " +
                    "ORDER BY l.id DESC")
    List<SysLogEntity> findCursor(@Param("module") String module,
                                   @Param("action") String action,
                                   @Param("cursor") Long cursor,
                                   Pageable pageable);

    /**
     * 审计日志游标分页查询（§3.6.10）。
     * 支持 module / action / action_source / operator_user_id / date_from / date_to / risk_level + cursor。
     * <p>改为 nativeQuery + CAST 以规避 Hibernate 6 + PostgreSQL 对可空参数的类型推断 Bug。</p>
     */
    @Query(nativeQuery = true,
            value = "SELECT * FROM sys_log l " +
                    "WHERE (CAST(:module       AS text)        IS NULL OR l.module          = CAST(:module       AS text)) " +
                    "  AND (CAST(:action       AS text)        IS NULL OR l.action          = CAST(:action       AS text)) " +
                    "  AND (CAST(:actionSource AS text)        IS NULL OR l.action_source   = CAST(:actionSource AS text)) " +
                    "  AND (CAST(:operatorId   AS bigint)      IS NULL OR l.operator_user_id = CAST(:operatorId  AS bigint)) " +
                    "  AND (CAST(:dateFrom     AS timestamptz) IS NULL OR l.created_at     >= CAST(:dateFrom     AS timestamptz)) " +
                    "  AND (CAST(:dateTo       AS timestamptz) IS NULL OR l.created_at     <= CAST(:dateTo       AS timestamptz)) " +
                    "  AND (CAST(:riskLevel    AS text)        IS NULL OR l.risk_level      = CAST(:riskLevel    AS text)) " +
                    "  AND (CAST(:cursor       AS bigint)      IS NULL OR l.id              < CAST(:cursor       AS bigint)) " +
                    "ORDER BY l.id DESC")
    List<SysLogEntity> findForQuery(@Param("module") String module,
                                    @Param("action") String action,
                                    @Param("actionSource") String actionSource,
                                    @Param("operatorId") Long operatorId,
                                    @Param("dateFrom") OffsetDateTime dateFrom,
                                    @Param("dateTo") OffsetDateTime dateTo,
                                    @Param("riskLevel") String riskLevel,
                                    @Param("cursor") Long cursor,
                                    Pageable pageable);

    List<SysLogEntity> findByCreatedAtBetween(OffsetDateTime from, OffsetDateTime to);

    /**
     * 审计导出查询（FR-GOV-007，API §3.6.21）。
     * <p>按时间区间 + 可选操作人 / 动作 / 资源类型(module) 过滤；按 createdAt 升序以便阅读。
     * 查询调用方必须传 Pageable(size=10001) 以侦测是否超限。</p>
     * <p>{@code from}/{@code to} 不可为 null；其余参数可空，已加 CAST 规避类型推断。</p>
     */
    @Query(nativeQuery = true,
            value = "SELECT * FROM sys_log l " +
                    "WHERE l.created_at BETWEEN :from AND :to " +
                    "  AND (CAST(:operatorId AS bigint) IS NULL OR l.operator_user_id = CAST(:operatorId AS bigint)) " +
                    "  AND (CAST(:action     AS text)   IS NULL OR l.action           = CAST(:action     AS text)) " +
                    "  AND (CAST(:module     AS text)   IS NULL OR l.module           = CAST(:module     AS text)) " +
                    "ORDER BY l.created_at ASC")
    List<SysLogEntity> findForExport(@Param("from") OffsetDateTime from,
                                      @Param("to") OffsetDateTime to,
                                      @Param("operatorId") Long operatorId,
                                      @Param("action") String action,
                                      @Param("module") String module,
                                      Pageable pageable);
}
