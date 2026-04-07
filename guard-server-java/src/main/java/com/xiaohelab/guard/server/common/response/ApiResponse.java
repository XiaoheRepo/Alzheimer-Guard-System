package com.xiaohelab.guard.server.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final String code;
    private final String message;
    private final String traceId;
    private final T data;

    private ApiResponse(String code, String message, String traceId, T data) {
        this.code = code;
        this.message = message;
        this.traceId = traceId;
        this.data = data;
    }

    public static <T> ApiResponse<T> ok(T data, String traceId) {
        return new ApiResponse<>("OK", "success", traceId, data);
    }

    public static <T> ApiResponse<T> ok(String traceId) {
        return new ApiResponse<>("OK", "success", traceId, null);
    }

    public static <T> ApiResponse<T> error(String code, String message, String traceId) {
        return new ApiResponse<>(code, message, traceId, null);
    }
}
