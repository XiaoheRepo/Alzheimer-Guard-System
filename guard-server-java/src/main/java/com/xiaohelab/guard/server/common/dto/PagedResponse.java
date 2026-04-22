package com.xiaohelab.guard.server.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.domain.Page;

import java.util.Collections;
import java.util.List;

/**
 * Offset 模式分页响应封装（API V2.0 §1.5）。
 * <p>字段契约：{@code items / page_no / page_size / total / has_next}。</p>
 * <p>用于普通查询接口；追加写入型流水表必须改用 {@link CursorResponse}。</p>
 */
public class PagedResponse<T> {

    private List<T> items;
    @JsonProperty("page_no")
    private int pageNo;
    @JsonProperty("page_size")
    private int pageSize;
    private long total;
    @JsonProperty("has_next")
    private boolean hasNext;

    public PagedResponse() {}

    public PagedResponse(List<T> items, int pageNo, int pageSize, long total, boolean hasNext) {
        this.items = items;
        this.pageNo = pageNo;
        this.pageSize = pageSize;
        this.total = total;
        this.hasNext = hasNext;
    }

    /** 从 Spring Data Page 直接转换；pageNo 走客户端显式 1-based 传参，Page 自身 0-based。 */
    public static <T> PagedResponse<T> fromPage(Page<T> page, int pageNo, int pageSize) {
        if (page == null) {
            return new PagedResponse<>(Collections.emptyList(), pageNo, pageSize, 0L, false);
        }
        return new PagedResponse<>(page.getContent(), pageNo, pageSize,
                page.getTotalElements(), page.hasNext());
    }

    public static <T> PagedResponse<T> empty(int pageNo, int pageSize) {
        return new PagedResponse<>(Collections.emptyList(), pageNo, pageSize, 0L, false);
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
