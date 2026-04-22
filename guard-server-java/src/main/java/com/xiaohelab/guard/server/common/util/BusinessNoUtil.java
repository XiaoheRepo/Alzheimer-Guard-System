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

    /**
     * 通用单号生成：{@code prefix + 13 位毫秒时间戳 + 4 位序号 + 5 位随机}。
     * <p>特点：
     * <ul>
     *   <li>有序（时间戳递增）→ 数据库索引友好；</li>
     *   <li>单 JVM 内靠 {@link AtomicLong} 保证同毫秒内不重复；</li>
     *   <li>末尾 5 位 {@link SecureRandom} 随机 → 跨 JVM 几乎无碰撞；</li>
     *   <li>非强唯一性保证，最终唯一性由数据库唯一索引兜底。</li>
     * </ul>
     *
     * @param prefix 业务前缀（如 T/C/O）
     * @return 形如 {@code T1711000000001234567890} 的单号
     */
    public static String generate(String prefix) {
        long ts = System.currentTimeMillis();
        long seq = SEQ.incrementAndGet() & 0xFFFF;
        int rnd = RANDOM.nextInt(1 << 20);
        return prefix + ts + String.format("%04d%05d", seq, rnd);
    }

    /** 生成寻回任务号，前缀 {@code T}。 */
    public static String taskNo()      { return generate("T"); }
    /** 生成线索号，前缀 {@code C}。 */
    public static String clueNo()      { return generate("C"); }
    /** 生成物资工单号，前缀 {@code O}。 */
    public static String orderNo()     { return generate("O"); }
    /** 生成患者档案号，前缀 {@code P}。 */
    public static String profileNo()   { return generate("P"); }
    /** 生成 AI 会话 ID，前缀 {@code S}。 */
    public static String sessionId()   { return generate("S"); }
    /** 生成监护邀请 ID，前缀 {@code INV}。 */
    public static String inviteId()    { return generate("INV"); }
    /** 生成监护权转移请求 ID，前缀 {@code TRF}。 */
    public static String transferId()  { return generate("TRF"); }
    /** 生成 AI 意图 ID，前缀 {@code INT}。 */
    public static String intentId()    { return generate("INT"); }
    /** 生成批量任务 ID，前缀 {@code BJ}。 */
    public static String batchJobId()  { return generate("BJ"); }
    /** 生成 RFID/NFC 标签码，前缀 {@code TG}。 */
    public static String tagCode()     { return generate("TG"); }
    /** 生成患者记忆笔记 ID，前缀 {@code N}。 */
    public static String noteId()      { return generate("N"); }
    /** 生成 Outbox 事件 ID，前缀 {@code E}。 */
    public static String eventId()     { return generate("E"); }
    /** 生成一次性 WebSocket 握手 ticket，前缀 {@code W}。 */
    public static String ticket()      { return generate("W"); }

    /**
     * 生成 6 位短码，字符集去除了容易混淆的 {@code I/O/0/1}。
     * 用于对外可分享的患者识别短码（贴纸/二维码等）。
     */
    public static String shortCode() {
        final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder sb = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        }
        return sb.toString();
    }
}
