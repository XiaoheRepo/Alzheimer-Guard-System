package com.xiaohelab.guard.server.common.util;

import com.fasterxml.jackson.core.type.TypeReference;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

/**
 * Base64 游标编解码工具（对应 API 规约 `next_cursor = base64({"id": lastId})`）。
 */
public final class CursorUtil {

    private CursorUtil() {}

    /** 将最后一条记录的 id 编码为 `next_cursor`。 */
    public static String encode(Long lastId) {
        if (lastId == null) return null;
        String json = "{\"id\":" + lastId + "}";
        return Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }

    /** 解码 `cursor` 字符串，提取其中的 id；非法/为空时返回 null（交由调用方作为首页处理）。 */
    public static Long decodeId(String cursor) {
        if (cursor == null || cursor.isBlank()) return null;
        try {
            byte[] raw = Base64.getDecoder().decode(cursor);
            Map<String, Object> m = JsonUtil.fromJson(new String(raw, StandardCharsets.UTF_8),
                    new TypeReference<Map<String, Object>>() {});
            Object idVal = m.get("id");
            if (idVal == null) return null;
            return Long.valueOf(idVal.toString());
        } catch (Exception e) {
            // 非法游标：静默降级为首页查询，避免暴露内部错误
            return null;
        }
    }
}
