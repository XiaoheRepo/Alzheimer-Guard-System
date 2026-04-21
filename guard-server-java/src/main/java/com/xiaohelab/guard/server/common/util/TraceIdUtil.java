package com.xiaohelab.guard.server.common.util;

import org.slf4j.MDC;

import java.util.UUID;

/**
 * MDC / trace_id 工具 (HC-04)。
 */
public final class TraceIdUtil {

    public static final String MDC_TRACE_ID = "trace_id";
    public static final String MDC_REQUEST_ID = "request_id";
    public static final String MDC_USER_ID = "user_id";

    private TraceIdUtil() {
    }

    public static String currentTraceId() {
        String t = MDC.get(MDC_TRACE_ID);
        return t == null ? "" : t;
    }

    public static String currentRequestId() {
        String t = MDC.get(MDC_REQUEST_ID);
        return t == null ? "" : t;
    }

    public static String newTraceId() {
        return "tr_" + UUID.randomUUID().toString().replace("-", "");
    }

    public static void setTraceId(String traceId) {
        if (traceId != null && !traceId.isBlank()) {
            MDC.put(MDC_TRACE_ID, traceId);
        }
    }

    public static void setRequestId(String requestId) {
        if (requestId != null && !requestId.isBlank()) {
            MDC.put(MDC_REQUEST_ID, requestId);
        }
    }

    public static void setUserId(Long userId) {
        if (userId != null) {
            MDC.put(MDC_USER_ID, userId.toString());
        }
    }

    public static void clear() {
        MDC.remove(MDC_TRACE_ID);
        MDC.remove(MDC_REQUEST_ID);
        MDC.remove(MDC_USER_ID);
    }
}
