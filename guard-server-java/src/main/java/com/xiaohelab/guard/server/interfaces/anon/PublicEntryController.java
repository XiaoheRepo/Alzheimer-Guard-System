package com.xiaohelab.guard.server.interfaces.anon;

import com.xiaohelab.guard.server.common.exception.BizException;
import com.xiaohelab.guard.server.common.response.ApiResponse;
import com.xiaohelab.guard.server.infrastructure.persistence.do_.PatientProfileDO;
import com.xiaohelab.guard.server.infrastructure.persistence.do_.TagAssetDO;
import com.xiaohelab.guard.server.infrastructure.persistence.mapper.PatientProfileMapper;
import com.xiaohelab.guard.server.infrastructure.persistence.mapper.TagAssetMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 公共入口接口（匿名可访问）。
 * 路由 /r/** 、/p/** 、/api/v1/public/** 均在 SecurityConfig 中已配置为白名单。
 */
@RestController
@RequiredArgsConstructor
public class PublicEntryController {

    private final TagAssetMapper tagAssetMapper;
    private final PatientProfileMapper patientProfileMapper;
    private final StringRedisTemplate redisTemplate;

    /** 前端应用基础地址，从配置注入，本地默认 http://localhost:3000 */
    @Value("${guard.public-entry-base-url:http://localhost:3000}")
    private String frontendBaseUrl;

    /** 入场令牌 TTL（秒），默认 120 */
    @Value("${guard.entry-token.ttl-seconds:120}")
    private long entryTokenTtlSeconds;

    // ─────────────────────────────────────────────────────────────────────────
    // QR 码 / NFC 扫码入口
    // GET /r/{tagCode}
    // 根据标签编码查询绑定患者，重定向到线索上报页或紧急求助页
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/r/{tagCode}")
    public void scanEntry(@PathVariable String tagCode, HttpServletResponse response) throws IOException {
        TagAssetDO tag = tagAssetMapper.findByTagCode(tagCode);
        if (tag == null || tag.getPatientId() == null) {
            response.sendRedirect(frontendBaseUrl + "/404");
            return;
        }

        PatientProfileDO patient = patientProfileMapper.findById(tag.getPatientId());
        if (patient == null) {
            response.sendRedirect(frontendBaseUrl + "/404");
            return;
        }

        // 标签丢失状态：转到紧急求助页
        if ("LOST".equals(tag.getStatus()) || "MISSING".equals(patient.getLostStatus())) {
            response.sendRedirect(
                    frontendBaseUrl + "/p/" + patient.getShortCode() + "/emergency/report"
                            + "?entry_type=SCAN&tag=" + tagCode);
            return;
        }

        // 正常入口：转到线索上报页
        response.sendRedirect(
                frontendBaseUrl + "/p/" + patient.getShortCode() + "/clues/new"
                        + "?entry_type=SCAN&tag=" + tagCode);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 短码入口页（前端 SPA，后端仅负责重定向）
    // GET /p/{shortCode}/clues/new
    // GET /p/{shortCode}/emergency/report
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/p/{shortCode}/clues/new")
    public void clueEntryPage(@PathVariable String shortCode,
                               @RequestParam(required = false) String entry_type,
                               @RequestParam(required = false) String tag,
                               HttpServletResponse response) throws IOException {
        StringBuilder url = new StringBuilder(frontendBaseUrl + "/p/" + shortCode + "/clues/new");
        String sep = "?";
        if (entry_type != null) { url.append(sep).append("entry_type=").append(entry_type); sep = "&"; }
        if (tag != null)         { url.append(sep).append("tag=").append(tag); }
        response.sendRedirect(url.toString());
    }

    @GetMapping("/p/{shortCode}/emergency/report")
    public void emergencyPage(@PathVariable String shortCode,
                               @RequestParam(required = false) String entry_type,
                               @RequestParam(required = false) String tag,
                               HttpServletResponse response) throws IOException {
        StringBuilder url = new StringBuilder(frontendBaseUrl + "/p/" + shortCode + "/emergency/report");
        String sep = "?";
        if (entry_type != null) { url.append(sep).append("entry_type=").append(entry_type); sep = "&"; }
        if (tag != null)         { url.append(sep).append("tag=").append(tag); }
        response.sendRedirect(url.toString());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 手动输入短码验证（知情人不扫码时使用）
    // POST /api/v1/public/clues/manual-entry
    // 校验 short_code + pin_code，颁发 entry_token（Redis TTL）
    // ─────────────────────────────────────────────────────────────────────────

    @PostMapping("/api/v1/public/clues/manual-entry")
    public ApiResponse<Map<String, Object>> manualEntry(
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId,
            @Valid @RequestBody ManualEntryRequest req) {

        PatientProfileDO patient = patientProfileMapper.findByShortCode(req.getShortCode());
        if (patient == null) {
            throw BizException.of("E_PAT_4041");
        }

        // 验证 PIN 码
        boolean pinOk = BCrypt.checkpw(req.getPinCode(), patient.getPinCodeHash());
        if (!pinOk) {
            throw BizException.of("E_AUTH_4001");
        }

        // 颁发 entry_token，存入 Redis，TTL = guard.entry-token.ttl-seconds
        String entryToken = UUID.randomUUID().toString().replace("-", "");
        redisTemplate.opsForValue().set(
                "entry_token:" + entryToken,
                patient.getId().toString(),
                entryTokenTtlSeconds,
                TimeUnit.SECONDS);

        return ApiResponse.ok(Map.of(
                "entry_token", entryToken,
                "patient_id", String.valueOf(patient.getId()),
                "short_code", patient.getShortCode(),
                "expire_seconds", entryTokenTtlSeconds
        ), traceId);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 请求体 DTO
    // ─────────────────────────────────────────────────────────────────────────

    @Data
    public static class ManualEntryRequest {

        @NotBlank(message = "short_code 不能为空")
        @Size(min = 6, max = 6, message = "short_code 格式错误")
        private String shortCode;

        @NotBlank(message = "pin_code 不能为空")
        @Size(min = 4, max = 12, message = "pin_code 长度 4~12 位")
        private String pinCode;
    }
}
