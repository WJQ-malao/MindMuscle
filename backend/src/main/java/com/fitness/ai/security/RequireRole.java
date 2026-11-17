package com.fitness.ai.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口级角色鉴权注解（RBAC）。
 * 标注在 Controller 方法或类上，由 RoleCheckAspect 统一拦截校验。
 * 用法: @RequireRole("ADMIN")
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireRole {

    /** 需要的角色编码，满足其一即可 */
    String[] value();
}
