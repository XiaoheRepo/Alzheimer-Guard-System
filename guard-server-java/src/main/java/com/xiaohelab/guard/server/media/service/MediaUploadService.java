package com.xiaohelab.guard.server.media.service;

import com.xiaohelab.guard.server.common.error.ErrorCode;
import com.xiaohelab.guard.server.common.exception.BizException;
import com.xiaohelab.guard.server.common.util.CryptoUtil;
import com.xiaohelab.guard.server.media.dto.MediaUploadSignRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 对象存储直传凭证服务（V2.1 §3.8.3.1，backend_handbook §25.5、§19.3）。
 * <p>有 OSS access key 时调用 aliyun-sdk-oss 的 {@code generatePresignedUrl(PUT)} 签真实直传 URL；
 * 无 key 时回退到本地 HMAC stub，便于毕设阶段离线开发。</p>
 * 体积白名单（字节）：
 * <ul>
 *   <li>PATIENT_AVATAR / USER_AVATAR: 5 MB</li>
 *   <li>PATIENT_APPEARANCE / CLUE_PHOTO / POSTER_BG: 10 MB</li>
 * </ul>
 */
@Service
public class MediaUploadService {

    private static final Logger log = LoggerFactory.getLogger(MediaUploadService.class);

    private static final long SIZE_5MB  = 5L * 1024 * 1024;
    private static final long SIZE_10MB = 10L * 1024 * 1024;
    private static final long URL_TTL_SECONDS = 600L;

    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("yyyy/MM/dd")
            .withZone(ZoneOffset.UTC);

    private final AliyunOssClient ossClient;

    @Value("${guard.media.oss-endpoint:https://mh-dev.oss-cn-beijing.aliyuncs.com}")
    private String stubEndpoint;

    @Value("${guard.media.public-cdn:https://cdn.example.com}")
    private String stubCdnBase;

    @Value("${guard.media.bucket:mh-dev}")
    private String stubBucket;

    public MediaUploadService(AliyunOssClient ossClient) {
        this.ossClient = ossClient;
    }

    /** 签发直传凭证。 */
    public Map<String, Object> sign(Long userId, MediaUploadSignRequest req) {
        long max = switch (req.getScene()) {
            case "PATIENT_AVATAR", "USER_AVATAR" -> SIZE_5MB;
            case "PATIENT_APPEARANCE", "CLUE_PHOTO", "POSTER_BG" -> SIZE_10MB;
            default -> throw BizException.of(ErrorCode.E_GOV_4004);
        };
        if (req.getSizeBytes() == null || req.getSizeBytes() <= 0 || req.getSizeBytes() > max) {
            throw BizException.of(ErrorCode.E_GOV_4131);
        }
        String ext = inferExt(req.getContentType());
        String rand = CryptoUtil.randomToken(6);
        String day = DAY_FMT.format(OffsetDateTime.now().toInstant());
        String folder = sceneToFolder(req.getScene());
        String objectKey = String.format("media/%s/%s/u%d_%s%s", folder, day, userId, rand, ext);

        long ttl = ossClient.isEnabled() ? ossClient.getProps().getUrlExpirationSeconds() : URL_TTL_SECONDS;
        long expiresAtEpoch = System.currentTimeMillis() / 1000L + ttl;

        String uploadUrl;
        String publicUrl;
        String mode;
        if (ossClient.isEnabled()) {
            try {
                uploadUrl = ossClient.presignPut(objectKey, req.getContentType(), ttl);
                publicUrl = ossClient.publicUrl(objectKey);
                mode = "OSS";
            } catch (Exception ex) {
                log.warn("[OSS] presign 失败，回退 stub：{}", ex.getMessage());
                uploadUrl = buildStubUploadUrl(userId, objectKey, req.getContentType(), expiresAtEpoch);
                publicUrl = stubCdnBase + "/" + objectKey;
                mode = "STUB_FALLBACK";
            }
        } else {
            uploadUrl = buildStubUploadUrl(userId, objectKey, req.getContentType(), expiresAtEpoch);
            publicUrl = stubCdnBase + "/" + objectKey;
            mode = "STUB";
        }

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", req.getContentType());
        headers.put("x-oss-meta-scene", req.getScene());

        Map<String, Object> out = new HashMap<>();
        out.put("object_key", objectKey);
        out.put("upload_url", uploadUrl);
        out.put("method", "PUT");
        out.put("headers", headers);
        out.put("public_url", publicUrl);
        out.put("expires_at", OffsetDateTime.ofInstant(
                java.time.Instant.ofEpochSecond(expiresAtEpoch), ZoneOffset.UTC).toString());
        out.put("max_size_bytes", max);
        out.put("scene", req.getScene());
        out.put("mode", mode);
        log.info("[Media] upload-sign userId={} scene={} size={} object_key={} mode={}",
                userId, req.getScene(), req.getSizeBytes(), objectKey, mode);
        return out;
    }

    private String buildStubUploadUrl(Long userId, String objectKey, String contentType, long expiresAtEpoch) {
        String signBase = String.join("\n", "PUT", contentType,
                String.valueOf(expiresAtEpoch), "/" + stubBucket + "/" + objectKey);
        String signature = CryptoUtil.sha256Hex(signBase + "|" + userId);
        return String.format("%s/%s?Expires=%d&OSSAccessKeyId=STUB&Signature=%s",
                stubEndpoint, objectKey, expiresAtEpoch, signature);
    }

    private String sceneToFolder(String scene) {
        return switch (scene) {
            case "PATIENT_AVATAR"     -> "patient/avatar";
            case "PATIENT_APPEARANCE" -> "patient/appearance";
            case "CLUE_PHOTO"         -> "clue";
            case "POSTER_BG"          -> "poster";
            case "USER_AVATAR"        -> "user/avatar";
            default -> "misc";
        };
    }

    private String inferExt(String contentType) {
        return switch (contentType.toLowerCase(Locale.ROOT)) {
            case "image/png"  -> ".png";
            case "image/webp" -> ".webp";
            default           -> ".jpg";
        };
    }
}

