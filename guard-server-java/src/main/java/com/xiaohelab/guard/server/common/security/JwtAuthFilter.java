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
    private final JwtRevocationService jwtRevocationService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 构造注入 Token 解析器与 Redis（用于黑名单校验）。 */
    public JwtAuthFilter(JwtTokenProvider tokenProvider,
                         StringRedisTemplate redisTemplate,
                         JwtRevocationService jwtRevocationService) {
        this.tokenProvider = tokenProvider;
        this.redisTemplate = redisTemplate;
        this.jwtRevocationService = jwtRevocationService;
    }

    /**
     * 核心过滤逻辑：
     * <ol>
     *   <li>读取 Authorization 头；无 Bearer Token 直接放行（白名单 URL 自行处理）；</li>
     *   <li>校验 Redis 黑名单（已 logout 的 Token）；</li>
     *   <li>解析 Claims，构造 {@link AuthUser} 并注入 {@link SecurityContextHolder}；</li>
     *   <li>设置 MDC user_id，便于日志追踪。</li>
     * </ol>
     * 解析失败或黑名单命中 → 直接写回 401 + {@link ErrorCode#E_GOV_4011}，中断过滤链。
     */
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
                // 用户级吊销校验（HC-02）：管理员禁用/注销/改角色后，对应用户的所有 JWT 立即失效
                long issuedAtSec = c.getIssuedAt() == null ? 0L : c.getIssuedAt().getTime() / 1000L;
                if (jwtRevocationService.isRevoked(userId, issuedAtSec)) {
                    writeUnauthorized(response, ErrorCode.E_GOV_4011, "Token 已被管理员吊销,请重新登录");
                    return;
                }
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

    /**
     * 直接向响应写入统一格式的错误 JSON。
     * @param response HTTP 响应
     * @param ec       错误码枚举（决定 HTTP 状态码与业务 code）
     * @param msg      对用户友好的错误描述
     */
    private void writeUnauthorized(HttpServletResponse response, ErrorCode ec, String msg) throws IOException {
        response.setStatus(ec.httpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(Result.fail(ec.code(), msg)));
    }
}
