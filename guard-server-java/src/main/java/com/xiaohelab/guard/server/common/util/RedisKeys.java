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
}
