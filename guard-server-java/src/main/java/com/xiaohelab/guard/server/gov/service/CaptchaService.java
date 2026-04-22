package com.xiaohelab.guard.server.gov.service;

import com.xiaohelab.guard.server.common.error.ErrorCode;
import com.xiaohelab.guard.server.common.exception.BizException;
import com.xiaohelab.guard.server.common.util.BusinessNoUtil;
import com.xiaohelab.guard.server.common.util.RedisKeys;
import com.xiaohelab.guard.server.gov.dto.CaptchaIssueRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * 行为/滑块验证码服务（V2.1 §3.8.2.2，backend_handbook §25.4）。
 * <ul>
 *   <li>Redis-only：captcha:token:{token} -&gt; "{scene}|{fingerprint}", TTL 5 min, SETNX NX 防重放。</li>
 *   <li>毕设最小校验：duration_ms &gt;= 300（过低判定机器），并返回一次性 token。</li>
 *   <li>消费逻辑由各业务接口在命中后 DEL key 完成。</li>
 * </ul>
 */
@Service
public class CaptchaService {

    private static final Logger log = LoggerFactory.getLogger(CaptchaService.class);
    private static final long TTL_SECONDS = 300L;

    private final StringRedisTemplate redis;

    public CaptchaService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /** 签发一次性验证 token。 */
    public Map<String, Object> issue(CaptchaIssueRequest req) {
        // 1. 最小行为校验：滑动耗时必须 >= 300ms 且 <= 60000ms（轨迹过快判定机器）
        Integer dur = req.getChallengeResult() == null ? null : req.getChallengeResult().getDurationMs();
        if (dur == null || dur < 300 || dur > 60_000) {
            throw BizException.of(ErrorCode.E_GOV_4038);
        }
        // 2. 生成 token 并 SETNX NX 防并发重放
        String token = "ct_" + BusinessNoUtil.ticket();
        String keyName = RedisKeys.captchaToken(token);
        Boolean ok = redis.opsForValue().setIfAbsent(keyName,
                req.getScene() + "|" + req.getDeviceFingerprint(),
                Duration.ofSeconds(TTL_SECONDS));
        if (!Boolean.TRUE.equals(ok)) {
            throw BizException.of(ErrorCode.E_GOV_4038);
        }
        log.info("[Captcha] issued scene={} fp={} dur={} token={}",
                req.getScene(), req.getDeviceFingerprint(), dur, token);
        Map<String, Object> out = new HashMap<>();
        out.put("captcha_token", token);
        out.put("expires_at", java.time.OffsetDateTime.now().plusSeconds(TTL_SECONDS).toString());
        out.put("usage_scene", req.getScene());
        return out;
    }

    /**
     * 消费一次性 token（返回 true 表示命中且已删除）。下游业务（如匿名线索上报）在进入前调用。
     * @param token         captcha_token
     * @param expectedScene 期望场景；传 null 不校验
     */
    public boolean consume(String token, String expectedScene) {
        if (token == null || token.isBlank()) return false;
        String key = RedisKeys.captchaToken(token);
        String v = redis.opsForValue().get(key);
        if (v == null) return false;
        if (expectedScene != null && !v.startsWith(expectedScene + "|")) return false;
        return Boolean.TRUE.equals(redis.delete(key));
    }
}
