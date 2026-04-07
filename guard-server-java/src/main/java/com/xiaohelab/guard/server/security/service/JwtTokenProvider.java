package com.xiaohelab.guard.server.security.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

/**
 * JWT 令牌工具：负责生成、解析、校验 access_token。
 * 密钥从环境变量 JWT_SECRET 注入，禁止硬编码。
 */
@Slf4j
@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long expirationMs;

    public JwtTokenProvider(
            @Value("${guard.jwt.secret}") String secret,
            @Value("${guard.jwt.expiration-ms}") long expirationMs) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    /**
     * 生成 access_token，载荷包含 userId、username、role。
     */
    public String generate(Long userId, String username, String role) {
        Date now = new Date();
        return Jwts.builder()
                .subject(username)
                .claims(Map.of("userId", userId, "role", role))
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .signWith(secretKey)
                .compact();
    }

    /**
     * 解析令牌，返回 Claims 载荷；解析失败抛 JwtException。
     */
    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 校验令牌是否合法（不抛异常版本，供过滤器使用）。
     */
    public boolean validate(String token) {
        try {
            parse(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("[JWT] 令牌校验失败: {}", e.getMessage());
            return false;
        }
    }

    /** 从 Claims 中提取用户 ID */
    public Long getUserId(Claims claims) {
        Object userId = claims.get("userId");
        if (userId instanceof Number n) return n.longValue();
        return null;
    }

    /** 从 Claims 中提取角色 */
    public String getRole(Claims claims) {
        return (String) claims.get("role");
    }
}
