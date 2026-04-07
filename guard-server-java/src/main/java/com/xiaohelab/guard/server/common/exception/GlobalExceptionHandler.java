package com.xiaohelab.guard.server.common.exception;

import com.xiaohelab.guard.server.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String HEADER_TRACE_ID = "X-Trace-Id";

    @ExceptionHandler(BizException.class)
    public ResponseEntity<ApiResponse<Void>> handleBiz(BizException ex, HttpServletRequest req) {
        String traceId = req.getHeader(HEADER_TRACE_ID);
        log.warn("[BizException] code={}, msg={}, traceId={}", ex.getCode(), ex.getMessage(), traceId);
        return ResponseEntity.status(ex.getHttpStatus())
                .body(ApiResponse.error(ex.getCode(), ex.getMessage(), traceId));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        String traceId = req.getHeader(HEADER_TRACE_ID);
        FieldError fe = ex.getBindingResult().getFieldErrors().stream().findFirst().orElse(null);
        String msg = fe != null ? fe.getField() + ": " + fe.getDefaultMessage() : "参数校验失败";
        return ResponseEntity.badRequest()
                .body(ApiResponse.error("E_REQ_4001", msg, traceId));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnknown(Exception ex, HttpServletRequest req) {
        String traceId = req.getHeader(HEADER_TRACE_ID);
        log.error("[UnhandledException] traceId={}", traceId, ex);
        return ResponseEntity.internalServerError()
                .body(ApiResponse.error("E_SYS_5000", "系统内部错误", traceId));
    }
}
