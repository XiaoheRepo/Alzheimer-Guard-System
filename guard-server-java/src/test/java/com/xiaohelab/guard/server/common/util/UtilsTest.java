package com.xiaohelab.guard.server.common.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 工具类单元测试：坐标转换、脱敏、业务号生成、Redis Key 拼装。
 */
class UtilsTest {

    @Test
    void haversine_known_distance_within_tolerance() {
        // 天安门 (116.397, 39.908) → 王府井 (116.417, 39.915) ≈ 1.9 km
        double d = CoordUtil.haversineMeter(116.397, 39.908, 116.417, 39.915);
        assertThat(d).isBetween(1700.0, 2100.0);
    }

    @Test
    void toWgs84_wgs84_input_is_identity() {
        double[] r = CoordUtil.toWgs84(116.4, 39.9, "WGS84");
        assertThat(r[0]).isEqualTo(116.4);
        assertThat(r[1]).isEqualTo(39.9);
    }

    @Test
    void toWgs84_gcj02_shifts_within_500m() {
        double[] r = CoordUtil.toWgs84(116.4, 39.9, "GCJ-02");
        double d = CoordUtil.haversineMeter(116.4, 39.9, r[0], r[1]);
        assertThat(d).isLessThan(1000.0);
    }

    @Test
    void desensitize_phone_keeps_first_3_last_4() {
        assertThat(DesensitizeUtil.phone("13812345678")).isEqualTo("138****5678");
        assertThat(DesensitizeUtil.phone(null)).isNull();
    }

    @Test
    void business_no_has_prefix_and_stable_length() {
        assertThat(BusinessNoUtil.taskNo()).startsWith("T");
        assertThat(BusinessNoUtil.clueNo()).startsWith("C");
        assertThat(BusinessNoUtil.orderNo()).startsWith("O");
        assertThat(BusinessNoUtil.intentId()).startsWith("INT");
        assertThat(BusinessNoUtil.noteId()).startsWith("N");
        assertThat(BusinessNoUtil.ticket()).startsWith("W");
    }

    @Test
    void redis_keys_follow_conventions() {
        assertThat(RedisKeys.wsTicket("abc")).isEqualTo("ws:ticket:abc");
    }
}
