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

    /** 注入 StringRedisTemplate，用于读取/销毁 ticket。 */
    public WsTicketHandshakeInterceptor(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 在 WebSocket 握手前校验 ticket：
     * <ul>
     *   <li>ticket 缺失 → 直接拒绝握手；</li>
     *   <li>Redis 查不到对应 userId（过期或已消费）→ 拒绝；</li>
     *   <li>校验通过 → 删除 Redis key（一次性消费），并将 userId / ticket 写入 attributes，供后续 Handler 使用。</li>
     * </ul>
     * @return true 放行握手；false 拒绝握手（客户端收到 403）
     */
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

    /** 握手完成后回调，本实现无需特殊处理。 */
    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                WebSocketHandler wsHandler, Exception exception) { /* no-op */ }

    /**
     * 从 HTTP 请求中抽取 ticket：
     * 优先使用 ServletRequest.getParameter（Servlet 容器已解析好）；
     * 否则回退到手动解析 URI query 字符串。
     */
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
