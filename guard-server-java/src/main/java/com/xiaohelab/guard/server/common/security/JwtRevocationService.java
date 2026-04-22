package com.xiaohelab.guard.server.common.security;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * JWT 用户级吊销服务（HC-02）。
 * <p>原理：在 Redis 中记录每个用户的"吊销时间戳（秒）"。{@link JwtAuthFilter} 在解析 JWT 后，
 * 对比 `iat` 与吊销时间，只要 `iat &lt; revokedAt` 即视为已失效。</p>
 * <p>与逐 Token 黑名单的区别：逐 Token 黑名单无法覆盖"仍在有效期内但尚未登出"的全部 Token；
 * 本服务一次操作即可吊销目标用户的全部在途 JWT。</p>
 *
 * <p>适用场景（V2.1 增量）：</p>
 * <ul>
 *   <li>管理员禁用 / 注销目标账号（FR-GOV-013 / FR-GOV-014）</li>
 *   <li>管理员修改目标角色（FR-GOV-012，强制重登以刷新 role claim）</li>
 *   <li>主动退出登录的兜底</li>
 * </ul>
 */
@Service
public class JwtRevocationService {

    /** 每个用户独立一个 Redis Key；TTL 取 Refresh Token 最长有效期 + 缓冲。 */
    private static final String KEY_PREFIX = "auth:user_revoked_at:";

    /** Redis Key TTL（秒）— 默认 8 天，覆盖 7 天的 refresh token 上限，防 Key 永驻。 */
    private static final Duration KEY_TTL = Duration.ofDays(8);

    private final StringRedisTemplate redisTemplate;

    public JwtRevocationService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 吊销目标用户的全部在途 JWT：将 Redis 中的 `revokedAt` 写为当前时间（秒）。
     * <p>幂等：重复调用仅会更新时间戳。</p>
     *
     * @param userId 目标用户主键
     */
    public void revokeAllForUser(Long userId) {
        if (userId == null) return;
        long nowSec = System.currentTimeMillis() / 1000L;
        redisTemplate.opsForValue().set(KEY_PREFIX + userId, String.valueOf(nowSec), KEY_TTL);
    }

    /**
     * 判断某个 JWT 是否已被用户级吊销。
     *
     * @param userId  JWT 载荷中的 subject（用户主键）
     * @param issuedAtSec JWT `iat` 字段（秒级时间戳）
     * @return true = 该 JWT 已失效
     */
    public boolean isRevoked(Long userId, long issuedAtSec) {
        if (userId == null) return false;
        String val = redisTemplate.opsForValue().get(KEY_PREFIX + userId);
        if (val == null || val.isEmpty()) return false;
        try {
            long revokedAt = Long.parseLong(val);
            // JWT 在吊销时间点之前签发 → 视为已失效
            return issuedAtSec < revokedAt;
        } catch (NumberFormatException ignore) {
            return false;
        }
    }
}
