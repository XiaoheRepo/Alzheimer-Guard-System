package com.xiaohelab.guard.server.common.websocket;

import com.xiaohelab.guard.server.common.util.RedisKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * WS 握手拦截器：校验 query 参数 ticket 是否在 Redis 中有效；有效则：
 *  - 注入 userId 到 attributes；
 *  - 删除 ticket（一次性消费）。
 */
@Component
public class WsTicketHandshakeInterceptor implements HandshakeInterceptor {

    private static final Logger log = LoggerFactory.getLogger(WsTicketHandshakeInterceptor.class);

    private final StringRedisTemplate redisTemplate;

    public WsTicketHandshakeInterceptor(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String ticket = extractTicket(request);
        if (ticket == null || ticket.isBlank()) {
            log.warn("[WS] reject: missing ticket");
            return false;
        }
        String key = RedisKeys.wsTicket(ticket);
        String userId = redisTemplate.opsForValue().get(key);
        if (userId == null) {
            log.warn("[WS] reject: ticket expired or not found");
            return false;
        }
        redisTemplate.delete(key);
        attributes.put("userId", Long.parseLong(userId));
        attributes.put("ticket", ticket);
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                WebSocketHandler wsHandler, Exception exception) { /* no-op */ }

    private String extractTicket(ServerHttpRequest request) {
        if (request instanceof ServletServerHttpRequest s) {
            String t = s.getServletRequest().getParameter("ticket");
            if (t != null) return t;
        }
        URI uri = request.getURI();
        String q = uri.getQuery();
        if (q == null) return null;
        for (String kv : q.split("&")) {
            int i = kv.indexOf('=');
            if (i > 0 && "ticket".equals(kv.substring(0, i))) return kv.substring(i + 1);
        }
        return null;
    }
}
