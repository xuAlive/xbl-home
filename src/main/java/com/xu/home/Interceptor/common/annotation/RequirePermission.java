package com.xu.home.Interceptor.common.annotation;

import java.lang.annotation.*;

/**
 * 权限注解
 * 用于Controller方法上，标识需要的权限
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequirePermission {
    /**
     * 需要的权限编码
     */
    String value();
}
