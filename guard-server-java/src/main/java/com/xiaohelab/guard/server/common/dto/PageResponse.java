package com.xiaohelab.guard.server.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Offset 分页响应通用封装。
 */
public class PageResponse<T> {
    private List<T> items;
    @JsonProperty("page_no")
    private int pageNo;
    @JsonProperty("page_size")
    private int pageSize;
    private long total;
    @JsonProperty("has_next")
    private boolean hasNext;

    public PageResponse() {}

    public PageResponse(List<T> items, int pageNo, int pageSize, long total) {
        this.items = items;
        this.pageNo = pageNo;
        this.pageSize = pageSize;
        this.total = total;
        this.hasNext = (long) pageNo * pageSize < total;
    }

    public static <T> PageResponse<T> of(List<T> items, int pageNo, int pageSize, long total) {
        return new PageResponse<>(items, pageNo, pageSize, total);
    }

    public List<T> getItems() { return items; }
    public void setItems(List<T> items) { this.items = items; }
    public int getPageNo() { return pageNo; }
    public void setPageNo(int pageNo) { this.pageNo = pageNo; }
    public int getPageSize() { return pageSize; }
    public void setPageSize(int pageSize) { this.pageSize = pageSize; }
    public long getTotal() { return total; }
    public void setTotal(long total) { this.total = total; }
    public boolean isHasNext() { return hasNext; }
    public void setHasNext(boolean hasNext) { this.hasNext = hasNext; }
}
