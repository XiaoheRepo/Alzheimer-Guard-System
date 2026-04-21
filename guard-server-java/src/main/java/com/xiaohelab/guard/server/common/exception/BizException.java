package com.xiaohelab.guard.server.common.exception;

import com.xiaohelab.guard.server.common.error.ErrorCode;

/**
 * 业务异常。所有业务层抛出的异常均应继承或直接使用该类型。
 */
public class BizException extends RuntimeException {

    private final ErrorCode errorCode;
    private final String overrideMessage;

    public BizException(ErrorCode errorCode) {
        super(errorCode.defaultMessage());
        this.errorCode = errorCode;
        this.overrideMessage = null;
    }

    public BizException(ErrorCode errorCode, String overrideMessage) {
        super(overrideMessage != null ? overrideMessage : errorCode.defaultMessage());
        this.errorCode = errorCode;
        this.overrideMessage = overrideMessage;
    }

    public static BizException of(ErrorCode code) {
        return new BizException(code);
    }

    public static BizException of(ErrorCode code, String msg) {
        return new BizException(code, msg);
    }

    public ErrorCode getErrorCode() { return errorCode; }

    public String getDisplayMessage() {
        return overrideMessage != null ? overrideMessage : errorCode.defaultMessage();
    }
}
