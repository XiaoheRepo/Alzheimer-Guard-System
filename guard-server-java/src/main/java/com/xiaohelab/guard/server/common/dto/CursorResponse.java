package com.xiaohelab.guard.server.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Cursor 分页响应通用封装（适用于追加写入类表：轨迹 / 审计 / 通知 / Outbox DEAD）。
 */
public class CursorResponse<T> {
    private List<T> items;
    @JsonProperty("page_size")
    private int pageSize;
    @JsonProperty("next_cursor")
    private String nextCursor;
    @JsonProperty("has_next")
    private boolean hasNext;

    public CursorResponse() {}

    public CursorResponse(List<T> items, int pageSize, String nextCursor, boolean hasNext) {
        this.items = items;
        this.pageSize = pageSize;
        this.nextCursor = nextCursor;
        this.hasNext = hasNext;
    }

    public static <T> CursorResponse<T> of(List<T> items, int pageSize, String nextCursor, boolean hasNext) {
        return new CursorResponse<>(items, pageSize, nextCursor, hasNext);
    }

    public static <T> CursorResponse<T> empty() {
        return new CursorResponse<>(List.of(), 0, null, false);
    }

    public List<T> getItems() { return items; }
    public void setItems(List<T> items) { this.items = items; }
    public int getPageSize() { return pageSize; }
    public void setPageSize(int pageSize) { this.pageSize = pageSize; }
    public String getNextCursor() { return nextCursor; }
    public void setNextCursor(String nextCursor) { this.nextCursor = nextCursor; }
    public boolean isHasNext() { return hasNext; }
    public void setHasNext(boolean hasNext) { this.hasNext = hasNext; }
}
