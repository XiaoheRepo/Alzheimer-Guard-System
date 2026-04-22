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

    /**
     * 审计日志游标分页查询（§3.6.10）。
     * 支持 module / action / action_source / operator_user_id / date_from / date_to / risk_level + cursor。
     */
    @Query("select l from SysLogEntity l " +
            "where (:module is null or l.module = :module) " +
            "  and (:action is null or l.action = :action) " +
            "  and (:actionSource is null or l.actionSource = :actionSource) " +
            "  and (:operatorId is null or l.operatorUserId = :operatorId) " +
            "  and (:dateFrom is null or l.createdAt >= :dateFrom) " +
            "  and (:dateTo is null or l.createdAt <= :dateTo) " +
            "  and (:riskLevel is null or l.riskLevel = :riskLevel) " +
            "  and (:cursor is null or l.id < :cursor) " +
            "order by l.id desc")
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
     */
    @Query("select l from SysLogEntity l " +
            "where l.createdAt between :from and :to " +
            "  and (:operatorId is null or l.operatorUserId = :operatorId) " +
            "  and (:action is null or l.action = :action) " +
            "  and (:module is null or l.module = :module) " +
            "order by l.createdAt asc")
    List<SysLogEntity> findForExport(@Param("from") OffsetDateTime from,
                                      @Param("to") OffsetDateTime to,
                                      @Param("operatorId") Long operatorId,
                                      @Param("action") String action,
                                      @Param("module") String module,
                                      Pageable pageable);
}
