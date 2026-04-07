package com.xiaohelab.guard.server.common.exception;

import lombok.Getter;

@Getter
public class BizException extends RuntimeException {

    private final String code;
    private final int httpStatus;

    public BizException(String code, String message, int httpStatus) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus;
    }

    public static BizException of(String code) {
        return new BizException(code, ErrorMessages.get(code), resolveStatus(code));
    }

    public static BizException of(String code, String message) {
        return new BizException(code, message, resolveStatus(code));
    }

    private static int resolveStatus(String code) {
        if (code == null) return 500;
        if (code.contains("_4001") || code.contains("_4002") || code.contains("_4003")
                || code.contains("_4004") || code.contains("_4005") || code.contains("_4006")
                || code.contains("_4007") || code.contains("_4008") || code.contains("_4009")
                || code.contains("_4010") || code.contains("_4012") || code.contains("_4150")
                || code.contains("_4221") || code.contains("_4222") || code.contains("_4223")
                || code.contains("_4224")) {
            return 400;
        }
        if (code.contains("_4011") || code.contains("_4030") || code.contains("_4031")
                || code.contains("_4032") || code.contains("_4033") || code.contains("_4034")
                || code.contains("_4039") || code.contains("_4038") || code.contains("_4097")
                || code.contains("_4226") || code.contains("_4231")) {
            return 403;
        }
        if (code.contains("_4041") || code.contains("_4042") || code.contains("_4043")
                || code.contains("_4044") || code.contains("_4045") || code.contains("_4046")) {
            return 404;
        }
        if (code.contains("_4091") || code.contains("_4092") || code.contains("_4093")
                || code.contains("_4094") || code.contains("_4095") || code.contains("_4096")
                || code.contains("_4098") || code.contains("_4099")) {
            return 409;
        }
        if (code.contains("_4291") || code.contains("_4292") || code.contains("_4293")) {
            return 429;
        }
        if (code.contains("_5")) {
            return 500;
        }
        return 400;
    }
}
