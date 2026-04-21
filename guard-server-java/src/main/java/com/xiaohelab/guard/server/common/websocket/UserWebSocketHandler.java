package com.xiaohelab.guard.server.common.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.Set;

/**
 * 用户级 WebSocket 会话管理器。
 *  - 单个用户可同时建立多个 session（多端）；
 *  - 支持向特定用户推送 JSON 文本消息；
 *  - 支持向全部在线用户广播。
 */
@Component
public class UserWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(UserWebSocketHandler.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final Map<Long, Set<WebSocketSession>> userSessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long userId = (Long) session.getAttributes().get("userId");
        if (userId == null) {
            try { session.close(CloseStatus.NOT_ACCEPTABLE); } catch (IOException ignored) {}
            return;
        }
        userSessions.computeIfAbsent(userId, k -> new CopyOnWriteArraySet<>()).add(session);
        log.info("[WS] connected userId={} sessionId={}", userId, session.getId());
        push(session, Map.of("type", "hello", "user_id", userId));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // 心跳 / 测试回显（毕设阶段）
        try {
            session.sendMessage(new TextMessage("{\"type\":\"pong\"}"));
        } catch (IOException ignored) {}
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = (Long) session.getAttributes().get("userId");
        if (userId != null) {
            Set<WebSocketSession> set = userSessions.get(userId);
            if (set != null) {
                set.remove(session);
                if (set.isEmpty()) userSessions.remove(userId);
            }
        }
        log.info("[WS] closed sessionId={} status={}", session.getId(), status);
    }

    /** 推送给指定用户（所有活跃 session）。 */
    public void sendToUser(Long userId, Object payload) {
        Set<WebSocketSession> set = userSessions.get(userId);
        if (set == null || set.isEmpty()) return;
        for (WebSocketSession s : set) push(s, payload);
    }

    /** 广播给所有在线用户（毕设阶段简单实现）。 */
    public void broadcast(Object payload) {
        for (Set<WebSocketSession> set : userSessions.values()) {
            for (WebSocketSession s : set) push(s, payload);
        }
    }

    private void push(WebSocketSession session, Object payload) {
        try {
            session.sendMessage(new TextMessage(MAPPER.writeValueAsString(payload)));
        } catch (IOException e) {
            log.warn("[WS] push failed sessionId={} err={}", session.getId(), e.getMessage());
        }
    }

    public int onlineUserCount() { return userSessions.size(); }
}
