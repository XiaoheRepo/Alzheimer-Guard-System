package com.xiaohelab.guard.server.media.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** 阿里云 OSS 配置（backend_handbook §19.3）。 */
@ConfigurationProperties(prefix = "aliyun.oss")
public class AliyunOssProperties {

    private String endpoint = "oss-cn-hangzhou.aliyuncs.com";
    private String bucket;
    private String accessKeyId;
    private String accessKeySecret;
    private long urlExpirationSeconds = 3600L;
    private int maxFileSizeMb = 10;
    private String publicCdn;

    public boolean isEnabled() {
        return accessKeyId != null && !accessKeyId.isBlank()
                && accessKeySecret != null && !accessKeySecret.isBlank()
                && bucket != null && !bucket.isBlank();
    }

    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
    public String getBucket() { return bucket; }
    public void setBucket(String bucket) { this.bucket = bucket; }
    public String getAccessKeyId() { return accessKeyId; }
    public void setAccessKeyId(String accessKeyId) { this.accessKeyId = accessKeyId; }
    public String getAccessKeySecret() { return accessKeySecret; }
    public void setAccessKeySecret(String accessKeySecret) { this.accessKeySecret = accessKeySecret; }
    public long getUrlExpirationSeconds() { return urlExpirationSeconds; }
    public void setUrlExpirationSeconds(long urlExpirationSeconds) { this.urlExpirationSeconds = urlExpirationSeconds; }
    public int getMaxFileSizeMb() { return maxFileSizeMb; }
    public void setMaxFileSizeMb(int maxFileSizeMb) { this.maxFileSizeMb = maxFileSizeMb; }
    public String getPublicCdn() { return publicCdn; }
    public void setPublicCdn(String publicCdn) { this.publicCdn = publicCdn; }
}
