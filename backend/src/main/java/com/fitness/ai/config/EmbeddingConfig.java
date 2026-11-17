package com.fitness.ai.config;

import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Embedding 单独指向硅基流动。
 * spring.ai.openai.* 自动配置中的 base-url 已被对话占用（DeepSeek 官方），
 * DeepSeek 官方不提供 Embedding 模型，因此这里手动构建一个指向硅基流动的
 * EmbeddingModel 并标记 @Primary，Milvus 向量库的自动配置会使用它。
 */
@Configuration
public class EmbeddingConfig {

    @Value("${app.siliconflow.api-key}")
    private String siliconFlowApiKey;

    @Bean
    @Primary
    public EmbeddingModel embeddingModel() {
        // 注意：不要带 /v1 后缀，Spring AI 会自动拼接 /v1
        OpenAiApi api = OpenAiApi.builder()
                .baseUrl("https://api.siliconflow.cn")
                .apiKey(siliconFlowApiKey)
                .build();
        return new OpenAiEmbeddingModel(api, MetadataMode.EMBED,
                OpenAiEmbeddingOptions.builder()
                        // bge-m3 输出 1024 维向量，与 milvus embedding-dimension 一致
                        .model("BAAI/bge-m3")
                        .build());
    }
}
