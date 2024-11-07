package com.datastat.aop;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LimitRequest {
    /**
     * 限流时间.
     * @return 返回结果
     */
    int callTime() default 1;
    /**
     * 限流次数.
     * @return 返回结果
     */
    int callCount() default 10;
}

