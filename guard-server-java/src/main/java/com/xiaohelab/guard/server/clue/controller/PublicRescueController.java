package com.xiaohelab.guard.server.clue.controller;

import com.xiaohelab.guard.server.common.dto.Result;
import com.xiaohelab.guard.server.common.error.ErrorCode;
import com.xiaohelab.guard.server.common.exception.BizException;
import com.xiaohelab.guard.server.common.util.BusinessNoUtil;
import com.xiaohelab.guard.server.common.util.DesensitizeUtil;
import com.xiaohelab.guard.server.common.util.RedisKeys;
import com.xiaohelab.guard.server.patient.entity.PatientProfileEntity;
import com.xiaohelab.guard.server.patient.repository.PatientProfileRepository;
import com.xiaohelab.guard.server.rescue.entity.RescueTaskEntity;
import com.xiaohelab.guard.server.rescue.repository.RescueTaskRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDate;
import java.time.Period;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 公开救援信息视图接口（V2.1 §3.8.2.1）。
 * <ul>
 *   <li>GET /api/v1/public/patients/{short_code}/rescue-info</li>
 *   <li>匿名可访问；需 entry_token（Cookie 或 X-Entry-Token 头）；TTL 120s。</li>
 *   <li>脱敏：姓名首字+**，年龄 10 岁分段，手机 3+4，完整地址 / 邮箱不返回。</li>
 *   <li>HC-07 BR-010：MISSING 状态下 avatar 走水印代理端点下发。</li>
 * </ul>
 */
@Tag(name = "Public.Rescue", description = "公开救援信息视图")
@RestController
@RequestMapping("/api/v1/public/patients")
public class PublicRescueController {

    private static final Logger log = LoggerFactory.getLogger(PublicRescueController.class);

    private static final Set<String> RESCUE_ELIGIBLE = Set.of("MISSING", "MISSING_PENDING", "SUSTAINED");

    private final PatientProfileRepository patientRepository;
    private final RescueTaskRepository rescueTaskRepository;
    private final StringRedisTemplate redis;

    @Value("${guard.entry-token.ttl-seconds:120}")
    private long entryTokenTtl;

    public PublicRescueController(PatientProfileRepository patientRepository,
                                  RescueTaskRepository rescueTaskRepository,
                                  StringRedisTemplate redis) {
        this.patientRepository = patientRepository;
        this.rescueTaskRepository = rescueTaskRepository;
        this.redis = redis;
    }

    @GetMapping("/{shortCode}/rescue-info")
    public Result<Map<String, Object>> rescueInfo(@PathVariable("shortCode") String shortCode,
                                                  HttpServletRequest http) {
        // 1. entry_token 校验（Cookie 优先，其次头）
        String entryToken = extractEntryToken(http);
        if (entryToken == null || entryToken.isBlank()) {
            throw BizException.of(ErrorCode.E_CLUE_4011);
        }
        String redisKey = RedisKeys.entryTokenConsumed(entryToken);
        String v = redis.opsForValue().get(redisKey);
        if (v == null) {
            throw BizException.of(ErrorCode.E_CLUE_4011);
        }
        Long ttlSec = redis.getExpire(redisKey);
        if (ttlSec == null || ttlSec <= 0) {
            throw BizException.of(ErrorCode.E_CLUE_4011);
        }

        // 2. 短码校验
        if (shortCode == null || !shortCode.matches("[A-Z0-9]{4,10}")) {
            throw BizException.of(ErrorCode.E_CLUE_4005);
        }
        PatientProfileEntity p = patientRepository.findByShortCode(shortCode)
                .orElseThrow(() -> BizException.of(ErrorCode.E_CLUE_4041));
        if (p.getDeletedAt() != null) {
            throw BizException.of(ErrorCode.E_CLUE_4041);
        }

        // 3. 构建脱敏患者段
        Map<String, Object> out = new HashMap<>();
        out.put("patient", buildPatient(p));

        // 4. 救援段：仅在 MISSING / MISSING_PENDING / SUSTAINED 返回
        if (RESCUE_ELIGIBLE.contains(p.getLostStatus())) {
            RescueTaskEntity task = rescueTaskRepository.findActiveByPatient(p.getId()).orElse(null);
            out.put("rescue", task == null ? null : buildRescue(task, p));
        } else {
            out.put("rescue", null);
        }

        // 5. 应急联系人（主监护手机脱敏）
        out.put("emergency_contact", buildEmergencyContact(p));

        // 6. 剩余 TTL
        out.put("entry_token_ttl_sec", ttlSec);
        log.info("[PublicRescue] short_code={} token_ttl_sec={} rescue_eligible={}",
                shortCode, ttlSec, RESCUE_ELIGIBLE.contains(p.getLostStatus()));
        return Result.ok(out);
    }

    private Map<String, Object> buildPatient(PatientProfileEntity p) {
        Map<String, Object> pm = new HashMap<>();
        pm.put("short_code", p.getShortCode());
        pm.put("display_name", DesensitizeUtil.chineseName(p.getName()));
        pm.put("age_range", ageRange(p.getBirthday()));
        pm.put("gender", p.getGender());
        // HC-07 / BR-010: MISSING 时照片走水印代理
        pm.put("avatar_url", buildAvatarUrl(p));
        pm.put("appearance_summary", p.getAppearanceFeatures());
        pm.put("medical_note", mergeMedicalNote(p));
        pm.put("language_tags", java.util.List.of("zh-CN"));
        return pm;
    }

    private Map<String, Object> buildRescue(RescueTaskEntity t, PatientProfileEntity p) {
        Map<String, Object> r = new HashMap<>();
        r.put("task_id", t.getTaskNo());
        r.put("state", t.getStatus());
        r.put("missing_at", p.getLostStatusEventTime());
        r.put("last_seen", null);
        r.put("last_area", null);
        r.put("reward_note", "感谢您的帮助,家属愿意致谢");
        return r;
    }

    private Map<String, Object> buildEmergencyContact(PatientProfileEntity p) {
        Map<String, Object> ec = new HashMap<>();
        String masked = p.getEmergencyContactPhone() == null
                ? null : DesensitizeUtil.phone(p.getEmergencyContactPhone());
        ec.put("display_name", "紧急联系人");
        ec.put("masked_phone", masked);
        return ec;
    }

    private String buildAvatarUrl(PatientProfileEntity p) {
        String avatar = p.getAvatarUrl();
        if (avatar == null || avatar.isBlank()) return null;
        if (!"MISSING".equals(p.getLostStatus())) return avatar;
        String wmToken = BusinessNoUtil.ticket();
        redis.opsForValue().set(RedisKeys.photoWmToken(wmToken), avatar,
                Duration.ofSeconds(entryTokenTtl));
        return "/api/v1/public/photos/view?token=" + wmToken;
    }

    private String ageRange(LocalDate birthday) {
        if (birthday == null) return null;
        int age = Period.between(birthday, LocalDate.now()).getYears();
        if (age < 0) return null;
        int lo = age / 10 * 10;
        return lo + "-" + (lo + 9);
    }

    private String mergeMedicalNote(PatientProfileEntity p) {
        StringBuilder sb = new StringBuilder();
        if (p.getChronicDiseases() != null) sb.append(p.getChronicDiseases());
        if (p.getAllergy() != null) {
            if (sb.length() > 0) sb.append("；");
            sb.append("过敏: ").append(p.getAllergy());
        }
        return sb.length() == 0 ? null : sb.toString();
    }

    private String extractEntryToken(HttpServletRequest http) {
        String hdr = http.getHeader("X-Entry-Token");
        if (hdr != null && !hdr.isBlank()) return hdr;
        Cookie[] cookies = http.getCookies();
        if (cookies != null) {
            for (Cookie c : cookies) {
                if ("entry_token".equals(c.getName())) return c.getValue();
            }
        }
        return null;
    }

}
