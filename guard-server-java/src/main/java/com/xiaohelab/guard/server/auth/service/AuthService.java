package com.xiaohelab.guard.server.auth.service;

import com.xiaohelab.guard.server.auth.dto.*;
import com.xiaohelab.guard.server.common.error.ErrorCode;
import com.xiaohelab.guard.server.common.exception.BizException;
import com.xiaohelab.guard.server.common.security.AuthUser;
import com.xiaohelab.guard.server.common.security.JwtTokenProvider;
import com.xiaohelab.guard.server.common.security.SecurityUtil;
import com.xiaohelab.guard.server.common.util.BusinessNoUtil;
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
        if (authHeader == null || !authHeader.startsWith("Bearer ")) return;
        String token = authHeader.substring(7).trim();
        try {
            Claims c = tokenProvider.parse(token);
            long ttl = (c.getExpiration().getTime() - System.currentTimeMillis()) / 1000L;
            if (ttl > 0) {
                redisTemplate.opsForValue().set("auth:blacklist:" + token.hashCode(),
                        "1", Duration.ofSeconds(ttl));
            }
        } catch (Exception ignore) {
            // 已失效令牌，无需处理
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
