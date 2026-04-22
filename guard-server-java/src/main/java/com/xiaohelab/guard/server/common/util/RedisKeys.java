package com.xiaohelab.guard.server.common.util;

/**
 * Redis 键模板。
 */
public final class RedisKeys {

    private RedisKeys() {}

    public static String idempotency(String requestId) { return "idem:req:" + requestId; }

    public static String wsRoute(Long userId) { return "ws:route:user:" + userId; }

    public static String quotaUser(Long userId, String yyyyMM) { return "quota:user:" + userId + ":" + yyyyMM; }

    public static String quotaPatient(Long patientId, String yyyyMM) { return "quota:patient:" + patientId + ":" + yyyyMM; }

    public static String quotaExemptPatient(Long patientId) { return "quota:exempt:patient:" + patientId; }

    public static String entryTokenConsumed(String jti) { return "entry_token:consumed:" + jti; }

    public static String aiIntent(String intentId) { return "intent:" + intentId; }

    public static String notifyThrottle(Long patientId, String eventType) {
        return "notify:throttle:patient:" + patientId + ":" + eventType;
    }

    public static String riskIp(String ip) { return "risk:ip:" + ip + ":m1"; }

    public static String riskDevice(String fp) { return "risk:device:" + fp + ":h1"; }

    public static String loginRisk(String ip) { return "risk:login:ip:" + ip + ":m1"; }

    public static String wsTicket(String ticket) { return "ws:ticket:" + ticket; }

    public static String shortCodeCooldown(String code) { return "shortcode:cd:" + code; }

    public static String configCache(String key) { return "cfg:" + key; }

    /**
     * 路人端照片水印代理 Token（HC-07, BR-010）。
     * 存储原始 OSS URL，TTL 与 entry_token 保持一致，到期后代理端点返回 404。
     */
    public static String photoWmToken(String token) { return "photo:wm:" + token; }

    /** 滑块/CAPTCHA 一次性 Token（V2.1 §3.8.2.2，TTL 5 min，SETNX NX 防重放）。 */
    public static String captchaToken(String token) { return "captcha:token:" + token; }

    /** AI 消息取消信号（V2.1 §3.8.1.5，给流式 worker 的协作标记，TTL 5 min）。 */
    public static String aiCancel(String sessionId, String messageId) {
        return "ai:cancel:" + sessionId + ":" + messageId;
    }

    /** 公开域限流（V2.1 §3.8.2/4291，按 IP+短码，1 min 窗口）。 */
    public static String publicRateLimit(String scene, String key) {
        return "ratelimit:public:" + scene + ":" + key + ":m1";
    }

    /** 推送令牌设备唯一校验（可选：注册时的分布式锁）。 */
    public static String pushTokenLock(Long userId, String deviceId) {
        return "lock:push:" + userId + ":" + deviceId;
    }

    /** 密码重置令牌（TTL = 30 分钟）。 */
    public static String pwdReset(String token) { return "auth:pwd_reset:" + token; }
}
