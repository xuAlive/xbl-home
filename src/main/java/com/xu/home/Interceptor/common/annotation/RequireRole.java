package com.xu.home.Interceptor.common.annotation;

import java.lang.annotation.*;

/**
 * 角色权限注解
 * 用于Controller方法上，标识需要的角色
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireRole {
    /**
     * 需要的角色编码（ADMIN/USER/GUEST）
     */
    String value();
}
