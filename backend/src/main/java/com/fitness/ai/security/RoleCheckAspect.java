package com.fitness.ai.security;

import com.fitness.ai.util.UserContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * RBAC 鉴权切面：解析 @RequireRole 注解，校验当前登录用户是否具备所需角色。
 * 角色信息来自 JWT 解析时写入的用户上下文（源头是 sys_user_role 关联表）。
 */
@Aspect
@Component
public class RoleCheckAspect {

    @Around("@annotation(com.fitness.ai.security.RequireRole) || @within(com.fitness.ai.security.RequireRole)")
    public Object checkRole(ProceedingJoinPoint joinPoint) throws Throwable {
        // 方法级注解优先，其次类级注解
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        RequireRole requireRole = AnnotationUtils.findAnnotation(signature.getMethod(), RequireRole.class);
        if (requireRole == null) {
            requireRole = AnnotationUtils.findAnnotation(joinPoint.getTarget().getClass(), RequireRole.class);
        }
        if (requireRole != null) {
            List<String> required = Arrays.asList(requireRole.value());
            boolean granted = required.stream().anyMatch(UserContext::hasRole);
            if (!granted) {
                throw new IllegalStateException("无权限执行该操作，需要角色: " + String.join("/", required));
            }
        }
        return joinPoint.proceed();
    }
}
