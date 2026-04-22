package com.xiaohelab.guard.server.gov.service;

import com.xiaohelab.guard.server.common.error.ErrorCode;
import com.xiaohelab.guard.server.common.exception.BizException;
import com.xiaohelab.guard.server.common.security.AuthUser;
import com.xiaohelab.guard.server.common.security.SecurityUtil;
import com.xiaohelab.guard.server.gov.entity.SysLogEntity;
import com.xiaohelab.guard.server.gov.repository.SysLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 审计日志导出服务（API V2.0 §3.6.21，FR-GOV-007；handbook §12.1）。
 *
 * <p>合规约束：</p>
 * <ul>
 *   <li>仅 SUPER_ADMIN 可调用（E_AUTH_4031）</li>
 *   <li>时间范围强约束：start_at &lt; end_at，区间 ≤ 31 天，起点不早于 180 天前（E_GOV_4095）</li>
 *   <li>单次最多 10,000 条，超限拒绝（E_GOV_4094）</li>
 *   <li>每次导出必写一条 CRITICAL 级"admin_export_audit_logs"审计，形成导出追溯闭环</li>
 * </ul>
 */
@Service
public class AuditLogExportService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogExportService.class);

    /** 合规硬上限 — 单次查询至多返回 10000 行。 */
    public static final int MAX_ROWS = 10_000;
    /** 查询窗口硬上限 — 31 天。 */
    public static final Duration MAX_RANGE = Duration.ofDays(31);
    /** 归档窗口 — 180 天，更早的审计日志不再在热库内。 */
    public static final Duration ARCHIVE_WINDOW = Duration.ofDays(180);

    private final SysLogRepository sysLogRepository;
    private final AuditLogger auditLogger;

    public AuditLogExportService(SysLogRepository sysLogRepository, AuditLogger auditLogger) {
        this.sysLogRepository = sysLogRepository;
        this.auditLogger = auditLogger;
    }

    /**
     * 执行查询并返回结果集；所有合规校验与审计在此处完成。
     */
    @Transactional(rollbackFor = Exception.class)
    public List<SysLogEntity> query(OffsetDateTime startAt,
                                    OffsetDateTime endAt,
                                    Long operatorId,
                                    String action,
                                    String resourceType,
                                    String format) {
        AuthUser me = SecurityUtil.current();
        // 规则 1：SUPER_ADMIN only
        if (!me.isSuperAdmin()) throw BizException.of(ErrorCode.E_AUTH_4031);

        validateRange(startAt, endAt);

        // 规则 3：预取 MAX_ROWS + 1 以判定超限
        List<SysLogEntity> rows = sysLogRepository.findForExport(
                startAt, endAt, operatorId,
                normalize(action), normalize(resourceType),
                PageRequest.of(0, MAX_ROWS + 1));
        if (rows.size() > MAX_ROWS) {
            throw BizException.of(ErrorCode.E_GOV_4094);
        }

        // 规则 4：导出行为本身写一条 CRITICAL 审计（admin_export_audit_logs）
        Map<String, Object> detail = new HashMap<>();
        detail.put("start_at", startAt.toString());
        detail.put("end_at", endAt.toString());
        detail.put("operator_filter", operatorId == null ? "" : String.valueOf(operatorId));
        detail.put("action_filter", action == null ? "" : action);
        detail.put("resource_type_filter", resourceType == null ? "" : resourceType);
        detail.put("format", format == null ? "json" : format);
        detail.put("row_count", rows.size());
        auditLogger.logSuccess("GOV", "admin_export_audit_logs", null,
                "CRITICAL", "CONFIRM_2", detail);
        log.warn("[Audit.Export] operator={} range=[{}..{}] rows={} format={}",
                me.getUserId(), startAt, endAt, rows.size(), format);
        return rows;
    }

    /**
     * 向 {@link OutputStream} 直接写 CSV（不落盘、不走内存聚合）。
     */
    public void writeCsv(List<SysLogEntity> rows, OutputStream out) throws IOException {
        try (Writer w = new OutputStreamWriter(out, StandardCharsets.UTF_8)) {
            // BOM 便于 Excel 正确识别 UTF-8
            w.write('\uFEFF');
            w.write("id,created_at,module,action,action_source,operator_user_id,operator_username,"
                    + "object_id,result,result_code,risk_level,confirm_level,client_ip,request_id,trace_id,detail\n");
            for (SysLogEntity r : rows) {
                w.write(csv(r.getId()));            w.write(',');
                w.write(csv(r.getCreatedAt()));     w.write(',');
                w.write(csv(r.getModule()));        w.write(',');
                w.write(csv(r.getAction()));        w.write(',');
                w.write(csv(r.getActionSource())); w.write(',');
                w.write(csv(r.getOperatorUserId())); w.write(',');
                w.write(csv(r.getOperatorUsername())); w.write(',');
                w.write(csv(r.getObjectId()));      w.write(',');
                w.write(csv(r.getResult()));        w.write(',');
                w.write(csv(r.getResultCode()));    w.write(',');
                w.write(csv(r.getRiskLevel()));     w.write(',');
                w.write(csv(r.getConfirmLevel()));  w.write(',');
                w.write(csv(r.getClientIp()));      w.write(',');
                w.write(csv(r.getRequestId()));     w.write(',');
                w.write(csv(r.getTraceId()));       w.write(',');
                w.write(csv(r.getDetail()));        w.write('\n');
            }
            w.flush();
        }
    }

    private void validateRange(OffsetDateTime start, OffsetDateTime end) {
        if (start == null || end == null) {
            throw BizException.of(ErrorCode.E_GOV_4095, "start_at / end_at 必填");
        }
        if (!start.isBefore(end)) {
            throw BizException.of(ErrorCode.E_GOV_4095, "start_at 必须早于 end_at");
        }
        Duration span = Duration.between(start, end);
        if (span.compareTo(MAX_RANGE) > 0) {
            throw BizException.of(ErrorCode.E_GOV_4095, "时间窗口超过 31 天");
        }
        OffsetDateTime earliest = OffsetDateTime.now().minus(ARCHIVE_WINDOW);
        if (start.isBefore(earliest)) {
            throw BizException.of(ErrorCode.E_GOV_4095, "start_at 超出 180 天归档窗口");
        }
    }

    private static String normalize(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    private static String csv(Object v) {
        if (v == null) return "";
        String s = v.toString();
        boolean needsQuote = s.indexOf(',') >= 0 || s.indexOf('"') >= 0
                || s.indexOf('\n') >= 0 || s.indexOf('\r') >= 0;
        if (!needsQuote) return s;
        return '"' + s.replace("\"", "\"\"") + '"';
    }
}
