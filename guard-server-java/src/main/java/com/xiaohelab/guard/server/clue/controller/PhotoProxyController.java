package com.xiaohelab.guard.server.clue.controller;

import com.xiaohelab.guard.server.common.util.PhotoWatermarkService;
import com.xiaohelab.guard.server.common.util.RedisKeys;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * 路人端照片代理控制器（BR-010, HC-07）。
 *
 * <p>设计意图：
 * <ol>
 *   <li>通过 Redis 令牌（{@code photo:wm:{token}}）间接传递原始 OSS URL，
 *       避免将 OSS 私有 URL 直接暴露给外部路人端（防止批量爬取）。</li>
 *   <li>代理拉取原始图片后，即时叠加当前时间戳水印，防止截图滥用（BR-010）。</li>
 *   <li>令牌 TTL 与 entry_token 保持一致，到期后自动失效（与走失信息展示时间同步）。</li>
 * </ol>
 *
 * <p>性能说明：本实现为同步 I/O 代理，适用于毕设场景（并发量有限）。
 * 生产高并发场景建议升级为 OSS 图片处理 URL（?x-oss-process=image/watermark,...）或
 * 异步流式代理。
 */
@Tag(name = "Public", description = "无需登录的公开入口（扫码 / 海报 / 匿名线索）")
@RestController
public class PhotoProxyController {

    private static final Logger log = LoggerFactory.getLogger(PhotoProxyController.class);

    /** 拉取原始 OSS 图片的超时时间。 */
    private static final Duration DOWNLOAD_TIMEOUT = Duration.ofSeconds(8);

    /** 单次照片下载最大字节数（5MB），防止异常大文件撑爆内存。 */
    private static final long MAX_PHOTO_BYTES = 5L * 1024 * 1024;

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private final StringRedisTemplate redis;
    private final PhotoWatermarkService watermarkService;

    public PhotoProxyController(StringRedisTemplate redis,
                                PhotoWatermarkService watermarkService) {
        this.redis = redis;
        this.watermarkService = watermarkService;
    }

    /**
     * 路人端带水印照片代理（BR-010）。
     *
     * <p>处理流程：
     * <ol>
     *   <li>从 Redis 解析 {@code token} 对应的原始 OSS URL；令牌不存在则 404。</li>
     *   <li>通过 JDK {@link HttpClient} 从 OSS 拉取原始图片（超时 8s，限 5MB）。</li>
     *   <li>调用 {@link PhotoWatermarkService#applyWatermark} 叠加时间戳水印。</li>
     *   <li>以 {@code image/jpeg} 直接写入响应流（零中间态存储）；
     *       响应头 {@code Cache-Control: no-store} 防止客户端缓存无水印版本。</li>
     * </ol>
     *
     * @param token    水印代理令牌（PublicEntryController 签发，存于 Redis）
     * @param response {@link HttpServletResponse}，直接写入图片字节流
     */
    @Operation(summary = "路人端照片代理（含时间戳水印）", hidden = true)
    @GetMapping("/api/v1/public/photos/view")
    public void viewWatermarked(@RequestParam String token,
                                HttpServletResponse response) throws IOException {
        // 1. 从 Redis 读取原始 OSS URL（token 过期或不存在则 404）
        String originalUrl = redis.opsForValue().get(RedisKeys.photoWmToken(token));
        if (originalUrl == null || originalUrl.isBlank()) {
            log.warn("[PhotoProxy] 水印 token 无效或已过期: token={}", token);
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        // 2. 拉取原始照片
        byte[] imageBytes;
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(originalUrl))
                    .timeout(DOWNLOAD_TIMEOUT)
                    .GET()
                    .build();
            HttpResponse<byte[]> resp =
                    HTTP_CLIENT.send(req, HttpResponse.BodyHandlers.ofByteArray());

            if (resp.statusCode() != 200) {
                log.warn("[PhotoProxy] OSS 返回非 200: status={}, url={}", resp.statusCode(), originalUrl);
                response.setStatus(HttpServletResponse.SC_BAD_GATEWAY);
                return;
            }
            imageBytes = resp.body();
            if (imageBytes.length > MAX_PHOTO_BYTES) {
                log.warn("[PhotoProxy] 照片超过 5MB 上限，拒绝处理: size={}", imageBytes.length);
                response.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
                return;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("[PhotoProxy] 照片下载被中断: url={}", originalUrl);
            response.setStatus(HttpServletResponse.SC_GATEWAY_TIMEOUT);
            return;
        } catch (Exception e) {
            log.error("[PhotoProxy] 照片下载失败: url={}, error={}", originalUrl, e.getMessage());
            response.setStatus(HttpServletResponse.SC_BAD_GATEWAY);
            return;
        }

        // 3. 叠加当前时间戳水印（"yyyy-MM-dd HH:mm 寻人专用"）
        String wmText = watermarkService.buildWatermarkText();
        byte[] wmBytes;
        try {
            wmBytes = watermarkService.applyWatermark(imageBytes, wmText);
        } catch (IOException e) {
            log.error("[PhotoProxy] 水印叠加失败: error={}", e.getMessage());
            // 降级：直接返回原图（不含水印，记录 WARN 但不阻断救援）
            log.warn("[PhotoProxy] 降级返回原图（无水印）: token={}", token);
            wmBytes = imageBytes;
        }

        // 4. 写回响应（image/jpeg，禁止客户端缓存，防截图滥用）
        response.setContentType("image/jpeg");
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, private");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setContentLength(wmBytes.length);
        response.getOutputStream().write(wmBytes);
        response.getOutputStream().flush();
        log.debug("[PhotoProxy] 水印图片已下发: token={}, size={}bytes, wm={}", token, wmBytes.length, wmText);
    }
}
