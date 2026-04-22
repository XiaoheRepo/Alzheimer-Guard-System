package com.xiaohelab.guard.server.gov.controller;

import com.xiaohelab.guard.server.auth.service.AuthService;
import com.xiaohelab.guard.server.common.dto.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * WebSocket 连接管理（API §4.1.1）。
 * <ul>
 *   <li>POST /api/v1/ws/ticket — 签发一次性 WebSocket Ticket（TTL = 30s）</li>
 * </ul>
 */
@Tag(name = "WebSocket", description = "WebSocket 连接管理")
@RestController
@RequestMapping("/api/v1/ws")
public class WsController {

    private final AuthService authService;

    public WsController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 签发一次性 WebSocket Ticket（§4.1.1）。
     * 客户端用此 ticket 建立 WebSocket 连接：{@code wss://{host}/ws?ticket={ws_ticket}}。
     * Ticket 30 秒内有效，一次性消费。
     *
     * @return {@code {"ws_ticket": "wst_xxx", "expires_in": 30}}
     */
    @Operation(summary = "签发 WebSocket Ticket")
    @PostMapping("/ticket")
    public Result<Map<String, Object>> ticket() {
        String wsTicket = authService.issueWsTicket();
        return Result.ok(Map.of("ws_ticket", wsTicket, "expires_in", 30));
    }
}
