package com.xiaohelab.guard.server.ai.service;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 文本切片器（按字符数滑动窗口，适合中文场景；LLD §7.1.3）。
 *
 * <p>不引入 tokenizer 以避免外部依赖；切片粒度由 {@link RagProperties#getChunkSize()} 控制。</p>
 */
@Component
public class TextChunker {

    /**
     * @param text     原文
     * @param size     单片字符数（>0）
     * @param overlap  相邻片段重叠字符数（0 <= overlap < size）
     * @return 至少 1 个非空切片；text 为空时返回空 list
     */
    public List<String> chunk(String text, int size, int overlap) {
        List<String> out = new ArrayList<>();
        if (text == null || text.isBlank()) return out;
        int s = Math.max(1, size);
        int o = Math.max(0, Math.min(overlap, s - 1));
        int step = s - o;
        int len = text.length();
        if (len <= s) {
            out.add(text);
            return out;
        }
        for (int start = 0; start < len; start += step) {
            int end = Math.min(start + s, len);
            String slice = text.substring(start, end);
            if (!slice.isBlank()) out.add(slice);
            if (end >= len) break;
        }
        return out;
    }
}
