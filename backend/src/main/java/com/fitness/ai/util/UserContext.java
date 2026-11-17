package com.fitness.ai.util;

/**
 * 当前登录用户上下文，由 JwtInterceptor 在请求开始时写入。
 * AI 工具方法从这里取 userId —— 模型既看不到也无法伪造，防止越权操作。
 */
public final class UserContext {

    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> ROLE = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(Long userId, String role) {
        USER_ID.set(userId);
        ROLE.set(role);
    }

    public static Long getUserId() {
        Long id = USER_ID.get();
        if (id == null) {
            throw new IllegalStateException("未登录或登录已过期");
        }
        return id;
    }

    public static String getRole() {
        return ROLE.get();
    }

    public static void clear() {
        USER_ID.remove();
        ROLE.remove();
    }
}
