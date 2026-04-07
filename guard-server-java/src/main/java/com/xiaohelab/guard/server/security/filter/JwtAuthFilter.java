package com.xiaohelab.guard.server.security.filter;

import com.xiaohelab.guard.server.security.service.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT 鉴权过滤器（每请求执行一次）。
 * <p>
 * 职责：
 * 1. 从 Authorization 头解析 Bearer token。
 * 2. 将 userId、username、role 注入 SecurityContext，供下游使用。
 * 3. 防止客户端伪造 X-User-Id / X-User-Role（HC-04 内部头防注入已在此屏蔽）。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    /** 网关注入的内部保留头，客户端不可伪造 */
    private static final List<String> RESERVED_HEADERS = List.of("X-User-Id", "X-User-Role", "X-Internal-Token");

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        // 检测客户端是否尝试伪造保留头 —— 单体模式下由过滤器自行拦截
        for (String header : RESERVED_HEADERS) {
            if (request.getHeader(header) != null) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json;charset=UTF-8");
                String traceId = request.getHeader("X-Trace-Id");
                response.getWriter().write(
                        "{\"code\":\"E_REQ_4003\",\"message\":\"客户端伪造内部保留头\",\"trace_id\":\""
                                + (traceId != null ? traceId : "") + "\",\"data\":null}");
                return;
            }
        }

        // 解析 Bearer token
        String token = extractToken(request);
        if (StringUtils.hasText(token) && jwtTokenProvider.validate(token)) {
            Claims claims = jwtTokenProvider.parse(token);
            Long userId = jwtTokenProvider.getUserId(claims);
            String role = jwtTokenProvider.getRole(claims);
            String username = claims.getSubject();

            // 构建 Spring Security 认证对象，权限固定前缀 ROLE_
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    userId,
                    username,
                    List.of(new SimpleGrantedAuthority("ROLE_" + role))
            );
            // 将 role 存入 details，供 Controller 快速取用
            auth.setDetails(new AuthDetails(userId, username, role));
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        chain.doFilter(request, response);
    }

    /** 从 Authorization: Bearer <token> 中截取令牌 */
    private String extractToken(HttpServletRequest req) {
        String bearer = req.getHeader("Authorization");
        if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }

    /** 携带当前请求身份信息的 Details 对象，存入 SecurityContext */
    public record AuthDetails(Long userId, String username, String role) {}
}
