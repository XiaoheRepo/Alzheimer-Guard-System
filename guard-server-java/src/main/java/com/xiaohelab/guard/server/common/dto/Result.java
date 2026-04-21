package com.xiaohelab.guard.server.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.xiaohelab.guard.server.common.error.ErrorCode;
import com.xiaohelab.guard.server.common.util.TraceIdUtil;

/**
 * 全局统一响应封装。
 *
 * <pre>
 * 成功: { "code": "ok", "message": "success", "trace_id": "...", "data": {} }
 * 失败: { "code": "E_XXX_NNNN", "message": "...", "trace_id": "...", "data": null }
 * </pre>
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
public class Result<T> {

    /** 业务码：成功为 "ok"，失败为 E_XXX_NNNN */
    private String code;
    /** 描述性消息 */
    private String message;
    /** 链路追踪 ID (HC-04) */
    @JsonProperty("trace_id")
    private String traceId;
    /** 业务负载 */
    private T data;

    public Result() {}

    public Result(String code, String message, String traceId, T data) {
        this.code = code;
        this.message = message;
        this.traceId = traceId;
        this.data = data;
    }

    public static <T> Result<T> ok(T data) {
        return new Result<>("ok", "success", TraceIdUtil.currentTraceId(), data);
    }

    public static <T> Result<T> ok() {
        return ok(null);
    }

    public static <T> Result<T> fail(String code, String message) {
        return new Result<>(code, message, TraceIdUtil.currentTraceId(), null);
    }

    public static <T> Result<T> fail(ErrorCode errorCode) {
        return new Result<>(errorCode.code(), errorCode.defaultMessage(), TraceIdUtil.currentTraceId(), null);
    }

    public static <T> Result<T> fail(ErrorCode errorCode, String message) {
        return new Result<>(errorCode.code(), message, TraceIdUtil.currentTraceId(), null);
    }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
}
