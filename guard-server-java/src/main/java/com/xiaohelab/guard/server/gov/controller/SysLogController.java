package com.xiaohelab.guard.server.gov.controller;

import com.xiaohelab.guard.server.common.dto.Result;
import com.xiaohelab.guard.server.common.error.ErrorCode;
import com.xiaohelab.guard.server.common.exception.BizException;
import com.xiaohelab.guard.server.common.security.AuthUser;
import com.xiaohelab.guard.server.common.security.SecurityUtil;
import com.xiaohelab.guard.server.gov.entity.SysLogEntity;
import com.xiaohelab.guard.server.gov.repository.SysLogRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Admin.SysLog", description = "系统操作审计日志（Admin）")
@RestController
@RequestMapping("/api/v1/admin/sys-logs")
public class SysLogController {

    private final SysLogRepository sysLogRepository;

    public SysLogController(SysLogRepository sysLogRepository) {
        this.sysLogRepository = sysLogRepository;
    }

    @GetMapping
    @Operation(summary = "按 cursor 游标翻页列出操作日志")
    public Result<List<SysLogEntity>> list(
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "50") int limit) {
        assertAdmin();
        return Result.ok(sysLogRepository.findCursor(module, action, cursor,
                PageRequest.of(0, Math.min(limit, 200), Sort.by(Sort.Direction.DESC, "id"))));
    }

    private void assertAdmin() {
        AuthUser u = SecurityUtil.current();
        if (!u.isAdmin()) throw BizException.of(ErrorCode.E_GOV_4030);
    }
}
