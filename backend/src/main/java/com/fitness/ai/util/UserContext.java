package com.fitness.ai.util;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 当前登录用户上下文，由 JwtInterceptor 在请求开始时写入。
 * AI 工具方法从这里取 userId —— 模型既看不到也无法伪造，防止越权操作。
 * 角色集合来源：sys_user_role -> sys_role（RBAC）
 */
public final class UserContext {

    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<Set<String>> ROLES = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(Long userId, List<String> roles) {
        USER_ID.set(userId);
        ROLES.set(new LinkedHashSet<>(roles));
    }

    public static Long getUserId() {
        Long id = USER_ID.get();
        if (id == null) {
            throw new IllegalStateException("未登录或登录已过期");
        }
        return id;
    }

    public static Set<String> getRoles() {
        Set<String> roles = ROLES.get();
        return roles == null ? Set.of() : roles;
    }

    /** 是否拥有指定角色（RBAC 校验入口） */
    public static boolean hasRole(String roleCode) {
        return getRoles().contains(roleCode);
    }

    /** 主角色：优先返回 ADMIN（兼容前端单角色展示），无角色时返回 USER */
    public static String getRole() {
        Set<String> roles = getRoles();
        if (roles.contains("ADMIN")) {
            return "ADMIN";
        }
        return roles.isEmpty() ? "USER" : roles.iterator().next();
    }

    public static void clear() {
        USER_ID.remove();
        ROLES.remove();
    }
}
