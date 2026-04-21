package com.xiaohelab.guard.server.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 令牌签发与校验工具（HS256）。
 */
@Component
public class JwtTokenProvider {

    @Value("${guard.jwt.secret}")
    private String secret;

    @Value("${guard.jwt.expiration-ms:86400000}")
    private long expirationMs;

    @Value("${guard.jwt.refresh-expiration-ms:604800000}")
    private long refreshExpirationMs;

    private SecretKey key;

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String issueAccessToken(Long userId, String username, String role) {
        return issue(userId, username, role, "access", expirationMs);
    }

    public String issueRefreshToken(Long userId, String username, String role) {
        return issue(userId, username, role, "refresh", refreshExpirationMs);
    }

    public long accessExpirationSeconds() { return expirationMs / 1000; }

    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    private String issue(Long userId, String username, String role, String type, long ttl) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .claim("role", role)
                .claim("typ", type)
                .issuedAt(new Date(now))
                .expiration(new Date(now + ttl))
                .signWith(key)
                .compact();
    }
}
