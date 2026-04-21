package com.xiaohelab.guard.server.common.util;

import java.security.SecureRandom;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 业务编号生成（taskNo/clueNo/orderNo/profileNo/sessionId/inviteId 等）。
 */
public final class BusinessNoUtil {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final AtomicLong SEQ = new AtomicLong(0);

    private BusinessNoUtil() {}

    public static String generate(String prefix) {
        long ts = System.currentTimeMillis();
        long seq = SEQ.incrementAndGet() & 0xFFFF;
        int rnd = RANDOM.nextInt(1 << 20);
        return prefix + ts + String.format("%04d%05d", seq, rnd);
    }

    public static String taskNo()      { return generate("T"); }
    public static String clueNo()      { return generate("C"); }
    public static String orderNo()     { return generate("O"); }
    public static String profileNo()   { return generate("P"); }
    public static String sessionId()   { return generate("S"); }
    public static String inviteId()    { return generate("INV"); }
    public static String transferId()  { return generate("TRF"); }
    public static String intentId()    { return generate("INT"); }
    public static String batchJobId()  { return generate("BJ"); }
    public static String tagCode()     { return generate("TG"); }
    public static String noteId()      { return generate("N"); }
    public static String eventId()     { return generate("E"); }
    public static String ticket()      { return generate("W"); }

    /** 生成 6 位大写字母数字短码 */
    public static String shortCode() {
        final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder sb = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        }
        return sb.toString();
    }
}
