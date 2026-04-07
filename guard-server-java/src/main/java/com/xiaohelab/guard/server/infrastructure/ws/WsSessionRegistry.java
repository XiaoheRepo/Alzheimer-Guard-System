package com.xiaohelab.guard.server.infrastructure.ws;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * WebSocket 会话注册表。
 * HC-05 约束：定向下发，禁止全量广播。
 * 用户连接时将 session 注册到本地 Map，并在 Redis 中记录 userId → pod_id 的路由。
 * 跨 Pod 推送通过 Kafka Topic ws.push.{pod_id} 转发。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WsSessionRegistry {

    /** pod 唯一标识（同 OutboxDispatcher 统一） */
    private static final String POD_ID = System.getenv().getOrDefault("POD_NAME", "pod-default");

    /** Redis key 前缀：ws:route:{userId} → pod_id */
    private static final String ROUTE_KEY_PREFIX = "ws:route:";

    /** Redis TTL：WebSocket 最大生命周期（秒） */
    private static final long SESSION_TTL_SECONDS = 7200;

    /** 本地会话存储：userId → WebSocketSession */
    private final Map<Long, WebSocketSession> localSessions = new ConcurrentHashMap<>();

    private final StringRedisTemplate redisTemplate;

    /**
     * 注册用户 WebSocket 连接。
     * 同时更新 Redis 路由表，TTL 与会话保持一致。
     */
    public void register(Long userId, WebSocketSession session) {
        localSessions.put(userId, session);
        String routeKey = ROUTE_KEY_PREFIX + userId;
        redisTemplate.opsForValue().set(routeKey, POD_ID, SESSION_TTL_SECONDS, TimeUnit.SECONDS);
        log.info("[WS] 用户连接注册. userId={}, sessionId={}", userId, session.getId());
    }

    /**
     * 注销用户 WebSocket 连接（断开时调用）。
     */
    public void unregister(Long userId) {
        localSessions.remove(userId);
        redisTemplate.delete(ROUTE_KEY_PREFIX + userId);
        log.info("[WS] 用户连接注销. userId={}", userId);
    }

    /**
     * 向指定用户定向推送消息（HC-05：仅本 Pod 内推送）。
     * 如果该用户不在本 Pod，由 Kafka ws.push.{pod_id} 路由中转。
     */
    public boolean pushToLocal(Long userId, String message) {
        WebSocketSession session = localSessions.get(userId);
        if (session == null || !session.isOpen()) {
            return false;
        }
        try {
            session.sendMessage(new TextMessage(message));
            return true;
        } catch (IOException e) {
            log.warn("[WS] 推送消息失败. userId={}, error={}", userId, e.getMessage());
            localSessions.remove(userId);
            return false;
        }
    }

    /**
     * 查询指定用户所在 Pod ID（从 Redis 路由表获取）。
     * 返回 null 表示用户当前无 WebSocket 连接。
     */
    public String getPodIdForUser(Long userId) {
        return redisTemplate.opsForValue().get(ROUTE_KEY_PREFIX + userId);
    }

    /** 查询用户是否在本 Pod 连接 */
    public boolean isLocallyConnected(Long userId) {
        WebSocketSession session = localSessions.get(userId);
        return session != null && session.isOpen();
    }

    public String currentPodId() {
        return POD_ID;
    }
}
