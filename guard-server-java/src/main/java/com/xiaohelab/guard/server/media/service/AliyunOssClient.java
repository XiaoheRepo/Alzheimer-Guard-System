package com.xiaohelab.guard.server.media.service;

import com.aliyun.oss.HttpMethod;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.GeneratePresignedUrlRequest;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.Date;

/**
 * 阿里云 OSS 客户端封装（懒加载）。
 * 无 access key 时不实例化 SDK 客户端，{@link #isEnabled()} 返回 false，调用方降级走 stub。
 */
@Component
@EnableConfigurationProperties(AliyunOssProperties.class)
public class AliyunOssClient {

    private static final Logger log = LoggerFactory.getLogger(AliyunOssClient.class);

    private final AliyunOssProperties props;
    private volatile OSS ossClient;

    public AliyunOssClient(AliyunOssProperties props) {
        this.props = props;
    }

    @PostConstruct
    void init() {
        if (!props.isEnabled()) {
            log.warn("[OSS] aliyun.oss.access-key-* 未配置，OSS 功能将走 stub 降级模式");
            return;
        }
        try {
            this.ossClient = new OSSClientBuilder().build(
                    "https://" + props.getEndpoint(),
                    props.getAccessKeyId(),
                    props.getAccessKeySecret());
            log.info("[OSS] 客户端初始化完成 endpoint={} bucket={}", props.getEndpoint(), props.getBucket());
        } catch (Exception e) {
            log.error("[OSS] 客户端初始化失败：{}，回退 stub", e.getMessage());
            this.ossClient = null;
        }
    }

    @PreDestroy
    void shutdown() {
        if (ossClient != null) {
            try { ossClient.shutdown(); } catch (Exception ignore) {}
        }
    }

    public boolean isEnabled() { return ossClient != null; }

    public AliyunOssProperties getProps() { return props; }

    /** 生成 PUT 上传的 presigned URL。 */
    public String presignPut(String objectKey, String contentType, long ttlSeconds) {
        if (!isEnabled()) throw new IllegalStateException("OSS not enabled");
        Date expiration = new Date(System.currentTimeMillis() + ttlSeconds * 1000L);
        GeneratePresignedUrlRequest req = new GeneratePresignedUrlRequest(
                props.getBucket(), objectKey, HttpMethod.PUT);
        req.setExpiration(expiration);
        req.setContentType(contentType);
        URL url = ossClient.generatePresignedUrl(req);
        return url.toString();
    }

    /** 生成对象的对外访问 URL（优先 CDN，回退到 bucket 直链）。 */
    public String publicUrl(String objectKey) {
        if (props.getPublicCdn() != null && !props.getPublicCdn().isBlank()) {
            return props.getPublicCdn().replaceAll("/+$", "") + "/" + objectKey;
        }
        return String.format("https://%s.%s/%s", props.getBucket(), props.getEndpoint(), objectKey);
    }
}
