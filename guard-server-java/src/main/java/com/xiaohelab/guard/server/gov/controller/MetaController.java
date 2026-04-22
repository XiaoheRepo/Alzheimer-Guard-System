package com.xiaohelab.guard.server.gov.controller;

import com.xiaohelab.guard.server.common.dto.Result;
import com.xiaohelab.guard.server.common.error.ErrorCode;
import com.xiaohelab.guard.server.common.exception.BizException;
import com.xiaohelab.guard.server.gov.repository.SysConfigRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 客户端元信息接口（V2.1 §3.8.6）。
 * <p>匿名可访问；读取 {@code sys_config} 的键：
 * <ul>
 *   <li>{@code client.version.{platform}.latest}</li>
 *   <li>{@code client.version.{platform}.min_compatible}</li>
 *   <li>{@code client.version.{platform}.force_upgrade}（true/false）</li>
 *   <li>{@code client.version.{platform}.release_notes_url}</li>
 *   <li>{@code client.version.{platform}.download_url}</li>
 *   <li>{@code client.announcement.level / title / content / expires_at}</li>
 * </ul>
 */
@Tag(name = "Meta", description = "客户端版本与公告元信息")
@RestController
@RequestMapping("/api/v1/meta")
public class MetaController {

    private static final Set<String> ALLOWED_PLATFORMS = Set.of("WEB_ADMIN", "ANDROID", "H5");

    private final SysConfigRepository sysConfigRepository;

    public MetaController(SysConfigRepository sysConfigRepository) {
        this.sysConfigRepository = sysConfigRepository;
    }

    @GetMapping("/version")
    public Result<Map<String, Object>> version(
            @RequestParam String platform,
            @RequestParam(required = false, defaultValue = "RELEASE") String channel) {
        // 1. 参数校验（HC-01）
        if (platform == null || !ALLOWED_PLATFORMS.contains(platform)) {
            throw BizException.of(ErrorCode.E_GOV_4004, "platform 枚举非法");
        }
        String prefix = "client.version." + platform.toLowerCase() + ".";
        // 2. 组装版本信息（缺失则给出降级默认值）
        Map<String, Object> out = new HashMap<>();
        out.put("platform", platform);
        out.put("channel", channel);
        out.put("latest_version", readOrDefault(prefix + "latest", "2.1.0"));
        out.put("min_compatible_version", readOrDefault(prefix + "min_compatible", "2.0.0"));
        out.put("force_upgrade", "true".equalsIgnoreCase(readOrDefault(prefix + "force_upgrade", "false")));
        out.put("release_notes_url", readOrNull(prefix + "release_notes_url"));
        out.put("download_url", readOrNull(prefix + "download_url"));

        // 3. 公告段（任一键缺失则整体置 null）
        String annLevel = readOrNull("client.announcement.level");
        if (annLevel != null) {
            Map<String, Object> ann = new HashMap<>();
            ann.put("level", annLevel);
            ann.put("title", readOrNull("client.announcement.title"));
            ann.put("content", readOrNull("client.announcement.content"));
            ann.put("expires_at", readOrNull("client.announcement.expires_at"));
            out.put("announcement", ann);
        } else {
            out.put("announcement", null);
        }
        out.put("server_time", OffsetDateTime.now().toString());
        return Result.ok(out);
    }

    private String readOrDefault(String key, String def) {
        return sysConfigRepository.findById(key).map(e -> e.getConfigValue() != null ? e.getConfigValue() : def)
                .orElse(def);
    }

    private String readOrNull(String key) {
        return sysConfigRepository.findById(key).map(e -> e.getConfigValue()).orElse(null);
    }
}
