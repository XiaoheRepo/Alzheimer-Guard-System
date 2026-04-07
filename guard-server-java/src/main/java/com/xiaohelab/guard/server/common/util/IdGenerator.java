package com.xiaohelab.guard.server.common.util;

import java.time.Instant;
import java.util.UUID;

public final class IdGenerator {

    private IdGenerator() {}

    public static String requestId() {
        return "req_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    public static String eventId() {
        return "evt_" + Instant.now().toEpochMilli() + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    public static String sessionId() {
        return "sess_" + UUID.randomUUID().toString().replace("-", "");
    }

    public static String noteId() {
        return "note_" + UUID.randomUUID().toString().replace("-", "");
    }

    public static String inviteId() {
        return "inv_" + UUID.randomUUID().toString().replace("-", "");
    }

    public static String transferRequestId() {
        return "trf_" + UUID.randomUUID().toString().replace("-", "");
    }

    public static String orderNo() {
        return "ORD" + System.currentTimeMillis();
    }

    public static String taskNo() {
        return "TSK" + System.currentTimeMillis();
    }

    public static String clueNo() {
        return "CLU" + System.currentTimeMillis();
    }

    public static String profileNo() {
        return "PAT" + System.currentTimeMillis();
    }
}
