package com.xiaohelab.guard.server.config;

import com.xiaohelab.guard.server.infrastructure.ws.GuardWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket 配置。
 * WS 端点：/ws/notifications，通过 token 参数鉴权（HC-05）。
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final GuardWebSocketHandler guardWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(guardWebSocketHandler, "/ws/notifications")
                // 允许跨域（开发阶段放开，生产需收紧）
                .setAllowedOriginPatterns("*");
    }
}
