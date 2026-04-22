package com.xiaohelab.guard.server.clue.controller;

import com.xiaohelab.guard.server.clue.dto.ClueReportRequest;
import com.xiaohelab.guard.server.clue.entity.ClueRecordEntity;
import com.xiaohelab.guard.server.clue.service.ClueService;
import com.xiaohelab.guard.server.common.dto.Result;
import com.xiaohelab.guard.server.common.error.ErrorCode;
import com.xiaohelab.guard.server.common.exception.BizException;
import com.xiaohelab.guard.server.common.util.BusinessNoUtil;
import com.xiaohelab.guard.server.common.util.RedisKeys;
import com.xiaohelab.guard.server.material.entity.TagAssetEntity;
import com.xiaohelab.guard.server.material.repository.TagAssetRepository;
import com.xiaohelab.guard.server.patient.entity.PatientProfileEntity;
import com.xiaohelab.guard.server.patient.repository.PatientProfileRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * 公开入口控制器（无需登录）。
 * <ul>
 *   <li>GET  /r/{resource_token} —— 扫码 / NFC 进入患者公示页（限展示 + 一次性 entry_token）</li>
 *   <li>GET  /p/{short_code}    —— 海报短码进入</li>
 *   <li>POST /api/v1/public/clues/manual-entry —— 匿名线索上报</li>
 * </ul>
 * 公开入口必须做速率限制 + 一次性 token 防止信息被批量爬取。
 */
@Tag(name = "Public", description = "无需登录的公开入口（扫码 / 海报 / 匿名线索）")
@RestController
public class PublicEntryController {

    private final PatientProfileRepository patientRepository;
    private final TagAssetRepository tagRepository;
    private final ClueService clueService;
    private final StringRedisTemplate redis;

    @Value("${guard.entry-token.ttl-seconds:600}")
    private long entryTokenTtl;

    public PublicEntryController(PatientProfileRepository patientRepository,
                                 TagAssetRepository tagRepository,
                                 ClueService clueService,
                                 StringRedisTemplate redis) {
        this.patientRepository = patientRepository;
        this.tagRepository = tagRepository;
        this.clueService = clueService;
        this.redis = redis;
    }

    @GetMapping("/r/{resourceToken}")
    public Result<Map<String, Object>> scanResource(@PathVariable String resourceToken) {
        TagAssetEntity tag = tagRepository.findByResourceToken(resourceToken)
                .orElseThrow(() -> BizException.of(ErrorCode.E_CLUE_4041));
        if (!("BOUND".equals(tag.getStatus()) || "SUSPECTED_LOST".equals(tag.getStatus()))) {
            throw BizException.of(ErrorCode.E_CLUE_4041);
        }
        PatientProfileEntity p = patientRepository.findById(tag.getPatientId())
                .orElseThrow(() -> BizException.of(ErrorCode.E_PRO_4041));
        return Result.ok(buildPublicView(p, tag.getTagCode()));
    }

    @GetMapping("/p/{shortCode}")
    public Result<Map<String, Object>> scanShortCode(@PathVariable String shortCode) {
        PatientProfileEntity p = patientRepository.findAll().stream()
                .filter(x -> shortCode.equals(x.getShortCode())).findFirst()
                .orElseThrow(() -> BizException.of(ErrorCode.E_CLUE_4042));
        return Result.ok(buildPublicView(p, null));
    }

    private Map<String, Object> buildPublicView(PatientProfileEntity p, String tagCode) {
        String entryToken = BusinessNoUtil.ticket();
        redis.opsForValue().set(RedisKeys.entryTokenConsumed(entryToken), "0",
                Duration.ofSeconds(entryTokenTtl));
        Map<String, Object> out = new HashMap<>();
        out.put("patient_id", p.getId());
        out.put("name_masked", p.getName() == null ? null
                : (p.getName().length() <= 1 ? p.getName()
                : p.getName().charAt(0) + "*".repeat(Math.max(0, p.getName().length() - 1))));
        out.put("gender", p.getGender());
        // HC-07 / BR-010: 走失状态下路人端照片必须叠加水印，通过代理令牌间接下发，原始 OSS URL 不外露
        String avatarUrl = p.getAvatarUrl();
        if ("MISSING".equals(p.getLostStatus()) && avatarUrl != null) {
            String wmToken = BusinessNoUtil.ticket();
            redis.opsForValue().set(RedisKeys.photoWmToken(wmToken), avatarUrl,
                    Duration.ofSeconds(entryTokenTtl));
            avatarUrl = "/api/v1/public/photos/view?token=" + wmToken;
        }
        out.put("avatar_url", avatarUrl);
        out.put("lost_status", p.getLostStatus());
        out.put("appearance_features", p.getAppearanceFeatures());
        out.put("entry_token", entryToken);
        out.put("entry_token_ttl", entryTokenTtl);
        out.put("tag_code", tagCode);
        return out;
    }

    @PostMapping("/api/v1/public/clues/manual-entry")
    public Result<ClueRecordEntity> anonymousReport(
            @RequestHeader(value = "X-Entry-Token", required = true) String entryToken,
            @RequestHeader(value = "X-Device-Fingerprint", required = false) String fingerprint,
            @Valid @RequestBody ClueReportRequest req,
            HttpServletRequest http) {
        // 1. entry_token 一次性校验
        String key = RedisKeys.entryTokenConsumed(entryToken);
        String v = redis.opsForValue().get(key);
        if (v == null || "1".equals(v)) {
            throw BizException.of(ErrorCode.E_CLUE_4012);
        }
        redis.opsForValue().set(key, "1", Duration.ofSeconds(entryTokenTtl));
        // 2. 匿名上报
        return Result.ok(clueService.anonymousReport(req, entryToken, http.getRemoteAddr(), fingerprint));
    }
}
