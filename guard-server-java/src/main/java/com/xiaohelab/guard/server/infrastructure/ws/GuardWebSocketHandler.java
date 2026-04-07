package com.xiaohelab.guard.server.infrastructure.ws;

import com.xiaohelab.guard.server.security.service.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.net.URI;

/**
 * WebSocket 连接处理器。
 * 连接时从 token 参数解析用户身份，注册到 WsSessionRegistry。
 * HC-05：通过路由注册实现定向下发，禁止全量广播。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GuardWebSocketHandler extends TextWebSocketHandler {

    private final WsSessionRegistry registry;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long userId = extractUserId(session);
        if (userId == null) {
            closeQuietly(session, CloseStatus.POLICY_VIOLATION);
            return;
        }
        session.getAttributes().put("userId", userId);
        registry.register(userId, session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = (Long) session.getAttributes().get("userId");
        if (userId != null) {
            registry.unregister(userId);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // 服务端不处理客户端上行 WS 消息（当前仅下发通知，保持简单）
        log.debug("[WS] 收到客户端消息（忽略）. payload={}", message.getPayload());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.warn("[WS] 连接异常. sessionId={}, error={}", session.getId(), exception.getMessage());
        Long userId = (Long) session.getAttributes().get("userId");
        if (userId != null) {
            registry.unregister(userId);
        }
    }

    /** 从 WebSocket 握手 URI 的 query 参数中提取 JWT 并验证 */
    private Long extractUserId(WebSocketSession session) {
        try {
            URI uri = session.getUri();
            if (uri == null) return null;
            String query = uri.getQuery();
            if (query == null || !query.contains("token=")) return null;

            String token = null;
            for (String param : query.split("&")) {
                if (param.startsWith("token=")) {
                    token = param.substring(6);
                    break;
                }
            }
            if (token == null || !jwtTokenProvider.validate(token)) return null;

            Claims claims = jwtTokenProvider.parse(token);
            return Long.parseLong(jwtTokenProvider.getUserId(claims));
        } catch (Exception e) {
            log.warn("[WS] token 解析失败. error={}", e.getMessage());
            return null;
        }
    }

    private void closeQuietly(WebSocketSession session, CloseStatus status) {
        try {
            session.close(status);
        } catch (Exception ignored) {}
    }
}
