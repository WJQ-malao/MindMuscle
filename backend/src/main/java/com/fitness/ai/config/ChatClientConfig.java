package com.fitness.ai.config;

import com.fitness.ai.tools.FitnessTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI 核心配置：一个 ChatClient 同时挂三大能力
 * 1. RAG: RetrievalAugmentationAdvisor 每次提问先检索 Milvus 知识库
 * 2. 对话记忆: PromptChatMemoryAdvisor 窗口式短期记忆
 * 3. Agent 工具: FitnessTools 中的 @Tool 方法，由大模型自主决策调用
 */
@Configuration
public class ChatClientConfig {

    public static final String SYSTEM_PROMPT = """
            你是一名专业的 AI 健身教练，具备运动科学和营养学知识。
            规则：
            1. 回答专业问题时，优先依据检索到的健身知识库内容，并在合适处注明依据；
            2. 涉及用户身体数据、训练计划、打卡记录时，必须调用工具获取真实数据，禁止编造数字；
            3. BMI、热量预算等指标由工具计算，直接引用工具返回的结果；
            4. 不得向用户透露工具名称、接口路径、数据库字段等技术细节；
            5. 知识库没有覆盖的内容，明确告知用户并建议咨询专业人士；
            6. 使用中文，语气专业、鼓励性、简洁。
            """;

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder,
                                 VectorStore vectorStore,
                                 FitnessTools fitnessTools) {
        ChatMemory chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(20)
                .build();

        return builder
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(
                        RetrievalAugmentationAdvisor.builder()
                                .documentRetriever(VectorStoreDocumentRetriever.builder()
                                        .vectorStore(vectorStore)
                                        .topK(5)
                                        .build())
                                // 知识库检索为空时保留用户原始提问（默认会把提问替换掉空上下文模板，导致模型看不到真问题、不触发工具）
                                .queryAugmenter(org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter.builder()
                                        .allowEmptyContext(true)
                                        .build())
                                .build(),
                        PromptChatMemoryAdvisor.builder(chatMemory).build()
                )
                .defaultToolCallbacks(MethodToolCallbackProvider.builder()
                        .toolObjects(fitnessTools)
                        .build())
                .build();
    }
}
