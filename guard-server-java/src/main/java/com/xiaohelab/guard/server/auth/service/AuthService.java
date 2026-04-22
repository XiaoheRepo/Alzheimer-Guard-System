package com.xiaohelab.guard.server.auth.service;

import com.xiaohelab.guard.server.auth.dto.*;
import com.xiaohelab.guard.server.common.error.ErrorCode;
import com.xiaohelab.guard.server.common.exception.BizException;
import com.xiaohelab.guard.server.common.security.AuthUser;
import com.xiaohelab.guard.server.common.security.JwtTokenProvider;
import com.xiaohelab.guard.server.common.security.SecurityUtil;
import com.xiaohelab.guard.server.common.util.BusinessNoUtil;
import com.xiaohelab.guard.server.common.util.CryptoUtil;
import com.xiaohelab.guard.server.common.util.RedisKeys;
import com.xiaohelab.guard.server.gov.entity.WsTicketEntity;
import com.xiaohelab.guard.server.gov.repository.WsTicketRepository;
import com.xiaohelab.guard.server.user.entity.UserEntity;
import com.xiaohelab.guard.server.user.repository.UserRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * 认证与会话服务。
 * <p>职责：
 * <ul>
 *   <li>账号注册、登录（含 IP 记录）、密码修改；</li>
 *   <li>JWT Access/Refresh 双 Token 签发与刷新；</li>
 *   <li>退出登录（将 Token 写入 Redis 黑名单，按 JWT 剩余 TTL 自动失效）；</li>
 *   <li>签发一次性 WebSocket 握手票据（写入 DB + Redis，TTL 短）。</li>
 * </ul>
 * 安全约束：密码全部经 BCrypt 哈希；不向外返回 passwordHash 字段。
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final StringRedisTemplate redisTemplate;
    private final WsTicketRepository wsTicketRepository;

    @Value("${guard.ws-ticket.ttl-seconds:30}")
    private int wsTicketTtl;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider tokenProvider,
                       StringRedisTemplate redisTemplate,
                       WsTicketRepository wsTicketRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.redisTemplate = redisTemplate;
        this.wsTicketRepository = wsTicketRepository;
    }

    /** 注册新用户。 */
    @Transactional(rollbackFor = Exception.class)
    public UserInfoResponse register(RegisterRequest req) {
        // 1. 用户名 / 邮箱唯一性校验
        if (userRepository.existsByUsername(req.getUsername())) {
            throw BizException.of(ErrorCode.E_GOV_4091);
        }
        if (userRepository.existsByEmail(req.getEmail())) {
            throw BizException.of(ErrorCode.E_GOV_4092);
        }
        // 2. 落库
        UserEntity u = new UserEntity();
        u.setUsername(req.getUsername());
        u.setEmail(req.getEmail());
        u.setEmailVerified(false);
        u.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        u.setNickname(req.getNickname() != null ? req.getNickname() : req.getUsername());
        u.setRole("FAMILY");
        u.setStatus("ACTIVE");
        userRepository.save(u);
        log.info("[Auth] register userId={} username={}", u.getId(), u.getUsername());
        return toInfo(u);
    }

    /** 登录。 */
    @Transactional(rollbackFor = Exception.class)
    public TokenResponse login(LoginRequest req, HttpServletRequest httpReq) {
        UserEntity u = userRepository.findByUsername(req.getUsername())
                .orElseThrow(() -> BizException.of(ErrorCode.E_AUTH_4011));
        if (!passwordEncoder.matches(req.getPassword(), u.getPasswordHash())) {
            throw BizException.of(ErrorCode.E_AUTH_4011);
        }
        if (!"ACTIVE".equals(u.getStatus())) {
            throw BizException.of(ErrorCode.E_GOV_4031);
        }
        u.setLastLoginAt(OffsetDateTime.now());
        u.setLastLoginIp(extractIp(httpReq));
        userRepository.save(u);
        return buildTokens(u);
    }

    /** 刷新 Access Token。 */
    public TokenResponse refresh(String refreshToken) {
        try {
            Claims c = tokenProvider.parse(refreshToken);
            if (!"refresh".equals(c.get("typ", String.class))) {
                throw BizException.of(ErrorCode.E_GOV_4011);
            }
            Long userId = Long.valueOf(c.getSubject());
            UserEntity u = userRepository.findById(userId)
                    .orElseThrow(() -> BizException.of(ErrorCode.E_GOV_4011));
            return buildTokens(u);
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw BizException.of(ErrorCode.E_GOV_4011);
        }
    }

    /**
     * 请求密码重置（§3.6.4）。
     * 无论邮箱是否存在均返回相同消息，防止用户枚举。
     * 生成 32 字节随机 Token，Redis 存储 30 分钟。
     */
    public void requestPasswordReset(PasswordResetRequestDto req) {
        // 1. 查找邮箱对应用户（不存在则静默忽略，防枚举）
        Optional<UserEntity> userOpt = userRepository.findByEmail(req.getEmail());
        if (userOpt.isEmpty()) {
            log.info("[Auth] password-reset requested for unknown email");
            return;
        }
        // 2. 生成一次性 Token，写入 Redis（TTL = 30 min）
        String token = CryptoUtil.randomToken(32);
        redisTemplate.opsForValue().set(
                RedisKeys.pwdReset(token),
                String.valueOf(userOpt.get().getId()),
                Duration.ofMinutes(30)
        );
        // TODO: 通过邮件渠道发送重置链接（HC-08）
        log.info("[Auth] password-reset token issued userId={}", userOpt.get().getId());
    }

    /**
     * 确认密码重置（§3.6.5）。
     * 验证 Token → 更新密码 → 删除 Token（一次性消费）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void confirmPasswordReset(PasswordResetConfirmDto req) {
        // 1. 从 Redis 取 userId
        String key = RedisKeys.pwdReset(req.getToken());
        String userIdStr = redisTemplate.opsForValue().get(key);
        if (userIdStr == null) {
            throw BizException.of(ErrorCode.E_GOV_4102);
        }
        // 2. 加载用户
        Long userId = Long.parseLong(userIdStr);
        UserEntity u = userRepository.findById(userId)
                .orElseThrow(() -> BizException.of(ErrorCode.E_GOV_4011));
        // 3. 更新密码
        u.setPasswordHash(passwordEncoder.encode(req.getNewPassword()));
        userRepository.save(u);
        // 4. 删除已用 Token（一次性消费）
        redisTemplate.delete(key);
        log.info("[Auth] password reset confirmed userId={}", userId);
    }

    /** 查询当前用户信息。 */
    public UserInfoResponse me() {
        AuthUser au = SecurityUtil.current();
        UserEntity u = userRepository.findById(au.getUserId())
                .orElseThrow(() -> BizException.of(ErrorCode.E_GOV_4011));
        return toInfo(u);
    }

    /** 修改密码。 */
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(ChangePasswordRequest req) {
        AuthUser au = SecurityUtil.current();
        UserEntity u = userRepository.findById(au.getUserId())
                .orElseThrow(() -> BizException.of(ErrorCode.E_GOV_4011));
        if (!passwordEncoder.matches(req.getOldPassword(), u.getPasswordHash())) {
            throw BizException.of(ErrorCode.E_USR_4011);
        }
        if (passwordEncoder.matches(req.getNewPassword(), u.getPasswordHash())) {
            throw BizException.of(ErrorCode.E_USR_4001);
        }
        u.setPasswordHash(passwordEncoder.encode(req.getNewPassword()));
        userRepository.save(u);
        log.info("[Auth] password changed userId={}", u.getId());
    }

    /** 注销（将 token 加入 Redis 黑名单）。 */
    public void logout(String authHeader) {
        logout(authHeader, null);
    }

    /**
     * 登出（V2.1 §3.8.7.1 扩展）：可同时吊销 refresh_token。
     * <p>幂等：token 已过期或为空时直接返回；返回当前服务端时间作为 revoked_at。</p>
     *
     * @param authHeader   Bearer access token 头
     * @param refreshToken 可选 refresh_token（一并拉黑）
     * @return 本次吊销的服务端时间
     */
    public OffsetDateTime logout(String authHeader, String refreshToken) {
        OffsetDateTime revokedAt = OffsetDateTime.now();
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            blacklistToken(authHeader.substring(7).trim());
        }
        if (refreshToken != null && !refreshToken.isBlank()) {
            blacklistToken(refreshToken.trim());
        }
        return revokedAt;
    }

    private void blacklistToken(String token) {
        try {
            Claims c = tokenProvider.parse(token);
            long ttl = (c.getExpiration().getTime() - System.currentTimeMillis()) / 1000L;
            if (ttl > 0) {
                redisTemplate.opsForValue().set("auth:blacklist:" + token.hashCode(),
                        "1", Duration.ofSeconds(ttl));
            }
        } catch (Exception ignore) {
            // 已失效令牌，幂等忽略
        }
    }

    /** 签发一次性 WebSocket 票据。 */
    @Transactional(rollbackFor = Exception.class)
    public String issueWsTicket() {
        AuthUser au = SecurityUtil.current();
        WsTicketEntity ticket = new WsTicketEntity();
        ticket.setTicket(BusinessNoUtil.ticket());
        ticket.setUserId(au.getUserId());
        ticket.setExpireAt(OffsetDateTime.now().plusSeconds(wsTicketTtl));
        wsTicketRepository.save(ticket);
        redisTemplate.opsForValue().set(RedisKeys.wsTicket(ticket.getTicket()),
                String.valueOf(au.getUserId()), Duration.ofSeconds(wsTicketTtl));
        return ticket.getTicket();
    }

    private TokenResponse buildTokens(UserEntity u) {
        String access = tokenProvider.issueAccessToken(u.getId(), u.getUsername(), u.getRole());
        String refresh = tokenProvider.issueRefreshToken(u.getId(), u.getUsername(), u.getRole());
        return new TokenResponse(access, refresh, tokenProvider.accessExpirationSeconds(), u.getId(), u.getRole());
    }

    private UserInfoResponse toInfo(UserEntity u) {
        UserInfoResponse r = new UserInfoResponse();
        r.setUserId(u.getId());
        r.setUsername(u.getUsername());
        r.setEmail(u.getEmail());
        r.setNickname(u.getNickname());
        r.setAvatarUrl(u.getAvatarUrl());
        r.setPhone(u.getPhone());
        r.setRole(u.getRole());
        r.setStatus(u.getStatus());
        r.setEmailVerified(u.getEmailVerified());
        r.setLastLoginAt(u.getLastLoginAt());
        return r;
    }

    private String extractIp(HttpServletRequest req) {
        if (req == null) return null;
        String h = req.getHeader("X-Forwarded-For");
        if (h != null && !h.isBlank()) return h.split(",")[0].trim();
        return req.getRemoteAddr();
    }
}
