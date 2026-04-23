package com.xiaohelab.guard.server.common.filter;

import com.xiaohelab.guard.server.common.util.TraceIdUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * HC-04 全链路追踪 Filter：注入/透传 trace_id 与 request_id 到 MDC 与响应头。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

    public static final String HEADER_TRACE_ID = "X-Trace-Id";
    public static final String HEADER_REQUEST_ID = "X-Request-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String traceId = request.getHeader(HEADER_TRACE_ID);
        if (traceId == null || traceId.isBlank()) {
            traceId = TraceIdUtil.newTraceId();
        }
        String requestId = request.getHeader(HEADER_REQUEST_ID);
        // 客户端未传时自动生成，保证每条审计日志都有 request_id
        if (requestId == null || requestId.isBlank()) {
            requestId = "req_" + UUID.randomUUID().toString().replace("-", "");
        }

        try {
            TraceIdUtil.setTraceId(traceId);
            TraceIdUtil.setRequestId(requestId);
            response.setHeader(HEADER_TRACE_ID, traceId);
            if (requestId != null && !requestId.isBlank()) {
                response.setHeader(HEADER_REQUEST_ID, requestId);
            }
            chain.doFilter(request, response);
        } finally {
            TraceIdUtil.clear();
        }
    }
}
