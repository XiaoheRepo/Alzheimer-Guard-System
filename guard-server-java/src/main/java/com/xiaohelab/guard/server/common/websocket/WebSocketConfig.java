package com.xiaohelab.guard.server.common.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * 注册 /ws 端点，使用 ticket 拦截器校验握手。
 * 前端连接示例：ws://host/ws?ticket=xxxxx
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final UserWebSocketHandler userWebSocketHandler;
    private final WsTicketHandshakeInterceptor wsTicketHandshakeInterceptor;

    public WebSocketConfig(UserWebSocketHandler userWebSocketHandler,
                           WsTicketHandshakeInterceptor wsTicketHandshakeInterceptor) {
        this.userWebSocketHandler = userWebSocketHandler;
        this.wsTicketHandshakeInterceptor = wsTicketHandshakeInterceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(userWebSocketHandler, "/ws")
                .addInterceptors(wsTicketHandshakeInterceptor)
                .setAllowedOriginPatterns("*");
    }
}
