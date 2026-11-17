package com.fitness.ai.dto;

import jakarta.validation.constraints.NotBlank;

public record ChatRequest(
        @NotBlank(message = "消息不能为空") String message,
        /** 会话ID，同一会话共享上下文记忆；为空则每次独立 */
        String conversationId
) {
}
