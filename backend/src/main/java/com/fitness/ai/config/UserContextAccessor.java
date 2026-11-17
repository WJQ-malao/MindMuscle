package com.fitness.ai.config;

import com.fitness.ai.util.UserContext;
import io.micrometer.context.ThreadLocalAccessor;

import java.util.List;
import java.util.Map;

/**
 * 用户上下文的 Reactor 传播器。
 * SSE 流式响应中，大模型工具调用会运行在 Reactor 的 I/O 线程而非请求线程，
 * ThreadLocal 默认不跨线程，导致工具内取不到登录用户。
 * 注册后（配合 Hooks.enableAutomaticContextPropagation），Reactor 在每次
 * 线程切换时自动捕获/恢复用户上下文，工具方法在任何线程都能拿到正确的用户身份。
 * 通过 META-INF/services 的 ServiceLoader 机制自动发现。
 */
public class UserContextAccessor implements ThreadLocalAccessor<Map<String, Object>> {

    public static final String KEY = "fitness.user.context";

    @Override
    public Object key() {
        return KEY;
    }

    @Override
    public Map<String, Object> getValue() {
        Long userId = UserContext.peekUserId();
        if (userId == null) {
            return null;
        }
        return Map.of(
                "userId", userId,
                "roles", List.copyOf(UserContext.getRoles()));
    }

    @Override
    public void setValue(Map<String, Object> value) {
        if (value == null) {
            UserContext.clear();
            return;
        }
        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) value.get("roles");
        UserContext.set((Long) value.get("userId"), roles);
    }

    @Override
    public void setValue() {
        UserContext.clear();
    }

    @Override
    public void restore(Map<String, Object> previousValue) {
        setValue(previousValue);
    }
}
