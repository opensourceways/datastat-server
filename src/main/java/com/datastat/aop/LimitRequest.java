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
   * 获取或设置允许调用的时间（秒）。
   * 
   * @return 允许调用的时间（秒）
   */
    int callTime() default 1;

    /**
   * 获取或设置允许连续调用的次数。
   * 
   * @return 允许连续调用的次数
   */
    int callCount() default 10;
}

