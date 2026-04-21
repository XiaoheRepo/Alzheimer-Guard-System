package com.xiaohelab.guard.server.common.aspect;

import com.xiaohelab.guard.server.common.annotation.Idempotent;
import com.xiaohelab.guard.server.common.error.ErrorCode;
import com.xiaohelab.guard.server.common.exception.BizException;
import com.xiaohelab.guard.server.common.util.TraceIdUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;

/**
 * 幂等切面。基于 Redis SETNX 实现请求级幂等防重。
 */
@Aspect
@Component
public class IdempotentAspect {

    private static final Logger log = LoggerFactory.getLogger(IdempotentAspect.class);
    private static final String KEY_PREFIX = "idem:req:";

    private final StringRedisTemplate redisTemplate;

    public IdempotentAspect(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Around("@annotation(idempotent)")
    public Object around(ProceedingJoinPoint pjp, Idempotent idempotent) throws Throwable {
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return pjp.proceed();
        }
        String requestId = request.getHeader("X-Request-Id");
        if (requestId == null || requestId.isBlank()) {
            throw BizException.of(ErrorCode.E_REQ_4001, "X-Request-Id 必填");
        }
        if (requestId.length() < 8 || requestId.length() > 128) {
            throw BizException.of(ErrorCode.E_REQ_4001);
        }
        String key = KEY_PREFIX + requestId;
        Boolean ok = redisTemplate.opsForValue().setIfAbsent(key, TraceIdUtil.currentTraceId(),
                Duration.ofSeconds(idempotent.ttlSeconds()));
        if (Boolean.FALSE.equals(ok)) {
            log.info("[Idempotent-Hit] requestId={} trace={}", requestId, TraceIdUtil.currentTraceId());
            throw BizException.of(ErrorCode.E_REQ_4001, "重复请求,已被幂等拦截");
        }
        return pjp.proceed();
    }

    private HttpServletRequest currentRequest() {
        Object attr = RequestContextHolder.getRequestAttributes();
        if (attr instanceof ServletRequestAttributes sra) {
            return sra.getRequest();
        }
        return null;
    }
}
