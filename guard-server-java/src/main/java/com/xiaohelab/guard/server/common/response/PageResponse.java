package com.xiaohelab.guard.server.common.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class PageResponse<T> {

    private final List<T> items;

    @JsonProperty("page_no")
    private final int pageNo;

    @JsonProperty("page_size")
    private final int pageSize;

    private final long total;

    @JsonProperty("has_next")
    private final boolean hasNext;
}
