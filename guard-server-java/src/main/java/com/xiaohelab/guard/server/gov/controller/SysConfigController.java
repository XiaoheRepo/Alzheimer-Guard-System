package com.xiaohelab.guard.server.gov.controller;

import com.xiaohelab.guard.server.common.annotation.Idempotent;
import com.xiaohelab.guard.server.common.dto.Result;
import com.xiaohelab.guard.server.common.error.ErrorCode;
import com.xiaohelab.guard.server.common.exception.BizException;
import com.xiaohelab.guard.server.common.security.AuthUser;
import com.xiaohelab.guard.server.common.security.SecurityUtil;
import com.xiaohelab.guard.server.gov.entity.SysConfigEntity;
import com.xiaohelab.guard.server.gov.repository.SysConfigRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/configs")
public class SysConfigController {

    private final SysConfigRepository configRepository;

    public SysConfigController(SysConfigRepository configRepository) {
        this.configRepository = configRepository;
    }

    @GetMapping
    public Result<List<SysConfigEntity>> list() {
        assertAdmin();
        return Result.ok(configRepository.findAll());
    }

    @GetMapping("/{key}")
    public Result<SysConfigEntity> get(@PathVariable String key) {
        assertAdmin();
        return Result.ok(configRepository.findById(key)
                .orElseThrow(() -> BizException.of(ErrorCode.E_GOV_4041)));
    }

    @PutMapping("/{key}")
    @Idempotent
    @Transactional(rollbackFor = Exception.class)
    public Result<SysConfigEntity> update(@PathVariable String key,
                                          @RequestBody Map<String, Object> body) {
        AuthUser user = assertAdmin();
        SysConfigEntity c = configRepository.findById(key)
                .orElseThrow(() -> BizException.of(ErrorCode.E_GOV_4041));
        if (body.containsKey("config_value")) c.setConfigValue((String) body.get("config_value"));
        if (body.containsKey("description")) c.setDescription((String) body.get("description"));
        c.setUpdatedBy(user.getUserId());
        c.setUpdatedAt(OffsetDateTime.now());
        configRepository.save(c);
        return Result.ok(c);
    }

    private AuthUser assertAdmin() {
        AuthUser u = SecurityUtil.current();
        if (!u.isAdmin()) throw BizException.of(ErrorCode.E_GOV_4030);
        return u;
    }
}
