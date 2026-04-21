package com.xiaohelab.guard.server.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaohelab.guard.server.common.dto.Result;
import com.xiaohelab.guard.server.common.error.ErrorCode;
import com.xiaohelab.guard.server.common.util.TraceIdUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT 认证 Filter：解析 Bearer Token，注入 SecurityContext。
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);
    private static final String BEARER = "Bearer ";

    private final JwtTokenProvider tokenProvider;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JwtAuthFilter(JwtTokenProvider tokenProvider, StringRedisTemplate redisTemplate) {
        this.tokenProvider = tokenProvider;
        this.redisTemplate = redisTemplate;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith(BEARER)) {
            String token = authHeader.substring(BEARER.length()).trim();
            try {
                Claims c = tokenProvider.parse(token);
                String jti = token.hashCode() + "";
                if (Boolean.TRUE.equals(redisTemplate.hasKey("auth:blacklist:" + jti))) {
                    writeUnauthorized(response, ErrorCode.E_GOV_4011, "Token 已注销");
                    return;
                }
                Long userId = Long.valueOf(c.getSubject());
                String username = c.get("username", String.class);
                String role = c.get("role", String.class);
                AuthUser user = new AuthUser(userId, username, role);
                List<SimpleGrantedAuthority> auths = List.of(new SimpleGrantedAuthority("ROLE_" + role));
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(user, null, auths);
                SecurityContextHolder.getContext().setAuthentication(auth);
                TraceIdUtil.setUserId(userId);
            } catch (Exception e) {
                log.debug("[JwtAuth] parse failed: {}", e.getMessage());
                writeUnauthorized(response, ErrorCode.E_GOV_4011, "Token 无效或已过期");
                return;
            }
        }
        chain.doFilter(request, response);
    }

    private void writeUnauthorized(HttpServletResponse response, ErrorCode ec, String msg) throws IOException {
        response.setStatus(ec.httpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(Result.fail(ec.code(), msg)));
    }
}
