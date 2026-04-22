package com.xiaohelab.guard.server.gov.controller;

import com.xiaohelab.guard.server.common.dto.Result;
import com.xiaohelab.guard.server.common.error.ErrorCode;
import com.xiaohelab.guard.server.common.exception.BizException;
import com.xiaohelab.guard.server.gov.entity.SysLogEntity;
import com.xiaohelab.guard.server.gov.service.AuditLogExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
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
 * 审计日志导出（API V2.0 §3.6.21，FR-GOV-007）。
 * <p>仅 SUPER_ADMIN；格式支持 JSON / CSV。</p>
 */
@Tag(name = "Admin.AuditLogExport", description = "审计日志导出（SUPER_ADMIN）")
@RestController
@RequestMapping("/api/v1/admin/logs")
public class AuditLogExportController {

    private final AuditLogExportService exportService;

    public AuditLogExportController(AuditLogExportService exportService) {
        this.exportService = exportService;
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
