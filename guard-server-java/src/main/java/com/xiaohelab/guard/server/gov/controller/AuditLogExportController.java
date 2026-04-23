package com.xiaohelab.guard.server.gov.controller;

import com.xiaohelab.guard.server.common.dto.CursorResponse;
import com.xiaohelab.guard.server.common.dto.Result;
import com.xiaohelab.guard.server.common.error.ErrorCode;
import com.xiaohelab.guard.server.common.exception.BizException;
import com.xiaohelab.guard.server.common.security.SecurityUtil;
import com.xiaohelab.guard.server.common.util.CursorUtil;
import com.xiaohelab.guard.server.gov.entity.SysLogEntity;
import com.xiaohelab.guard.server.gov.repository.SysLogRepository;
import com.xiaohelab.guard.server.gov.service.AuditLogExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * 审计日志查询与导出（§3.6.10 / §3.6.21，FR-GOV-006 / FR-GOV-007）。
 */
@Tag(name = "Admin.AuditLog", description = "审计日志查询与导出")
@RestController
@RequestMapping("/api/v1/admin/logs")
public class AuditLogExportController {

    private final AuditLogExportService exportService;
    private final SysLogRepository sysLogRepository;

    public AuditLogExportController(AuditLogExportService exportService,
                                    SysLogRepository sysLogRepository) {
        this.exportService = exportService;
        this.sysLogRepository = sysLogRepository;
    }

    /**
     * 审计日志游标分页查询（§3.6.10，ADMIN / SUPER_ADMIN）。
     */
    @GetMapping
    @Operation(summary = "3.6.10 审计日志查询")
    public Result<CursorResponse<SysLogEntity>> query(
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String action,
            @RequestParam(name = "action_source", required = false) String actionSource,
            @RequestParam(name = "operator_user_id", required = false) Long operatorUserId,
            @RequestParam(name = "date_from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime dateFrom,
            @RequestParam(name = "date_to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime dateTo,
            @RequestParam(name = "risk_level", required = false) String riskLevel,
            @RequestParam(required = false) String cursor,
            @RequestParam(name = "page_size", defaultValue = "50") int pageSize) {

        if (!SecurityUtil.current().isAdmin()) throw BizException.of(ErrorCode.E_AUTH_4031);

        if (pageSize < 1 || pageSize > 200) pageSize = 50;
        Long cursorId = (cursor == null || cursor.isBlank()) ? null : CursorUtil.decodeId(cursor);

        List<SysLogEntity> rows = sysLogRepository.findForQuery(
                module, action, actionSource, operatorUserId,
                dateFrom, dateTo, riskLevel, cursorId,
                PageRequest.of(0, pageSize + 1));

        boolean hasNext = rows.size() > pageSize;
        if (hasNext) rows = rows.subList(0, pageSize);

        String nextCursor = hasNext ? CursorUtil.encode(rows.get(rows.size() - 1).getId()) : null;
        return Result.ok(new CursorResponse<>(rows, pageSize, nextCursor, hasNext));
    }

    @GetMapping("/export")
    @Operation(summary = "3.6.21 审计日志导出（JSON/CSV）")
    public Object export(
            @RequestParam("start_at")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startAt,
            @RequestParam("end_at")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endAt,
            @RequestParam(name = "operator_id", required = false) Long operatorId,
            @RequestParam(required = false) String action,
            @RequestParam(name = "resource_type", required = false) String resourceType,
            @RequestParam(required = false, defaultValue = "json") String format,
            HttpServletResponse response) {

        List<SysLogEntity> rows = exportService.query(
                startAt, endAt, operatorId, action, resourceType, format);

        if ("csv".equalsIgnoreCase(format)) {
            String filename = String.format("audit_logs_%s_%s.csv",
                    startAt.toLocalDate(), endAt.toLocalDate());
            response.setContentType("text/csv; charset=utf-8");
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setHeader("Content-Disposition",
                    "attachment; filename=\"" + filename + "\"");
            try {
                exportService.writeCsv(rows, response.getOutputStream());
            } catch (IOException e) {
                throw BizException.of(ErrorCode.E_SYS_5000, "CSV 输出失败: " + e.getMessage());
            }
            return null;
        }
        // 默认 json
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        return Result.ok(rows);
    }
}
