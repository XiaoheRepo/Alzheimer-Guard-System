package com.xiaohelab.guard.server.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 幂等注解（HC-03）。标注的方法必须带 X-Request-Id，拦截到相同请求会直接返回缓存结果。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {
    /** 幂等窗口（秒） */
    long ttlSeconds() default 24 * 3600;
}
