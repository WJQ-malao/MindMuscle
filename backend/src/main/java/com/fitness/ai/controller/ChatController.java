package com.fitness.ai.controller;

import com.fitness.ai.common.Result;
import com.fitness.ai.dto.ChatRequest;
import com.fitness.ai.service.SysConfigService;
import com.fitness.ai.util.UserContext;
import jakarta.validation.Valid;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatClient chatClient;
    private final SysConfigService sysConfigService;

    public ChatController(ChatClient chatClient, SysConfigService sysConfigService) {
        this.chatClient = chatClient;
        this.sysConfigService = sysConfigService;
    }

    /** 非流式问答：一次返回完整回答 */
    @PostMapping
    public Result<Map<String, String>> chat(@Valid @RequestBody ChatRequest request) {
        if (!sysConfigService.isAiCoachEnabled()) {
            return Result.ok(Map.of("answer", SysConfigService.DISABLED_TIP));
        }
        String answer = chatClient.prompt()
                .user(request.message())
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId(request)))
                .call()
                .content();
        return Result.ok(Map.of("answer", answer == null ? "" : answer));
    }

    /** 流式问答：SSE 逐 token 推送，前端打字机效果 */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStream(@Valid @RequestBody ChatRequest request) {
        // 管理员关闭 AI 教练后，用户发消息只收到提示，不调用大模型
        if (!sysConfigService.isAiCoachEnabled()) {
            return Flux.just(SysConfigService.DISABLED_TIP);
        }
        // 会话隔离：以 用户ID+会话ID 作为记忆键，避免串号
        String convId = UserContext.getUserId() + ":" + conversationId(request);
        return chatClient.prompt()
                .user(request.message())
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, convId))
                .stream()
                .content();
    }

    private String conversationId(ChatRequest request) {
        return (request.conversationId() == null || request.conversationId().isBlank())
                ? "default" : request.conversationId();
    }
}
