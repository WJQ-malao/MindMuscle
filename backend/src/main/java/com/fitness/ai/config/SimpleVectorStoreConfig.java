package com.fitness.ai.config;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.FileSystemResource;

import java.io.File;

/**
 * 降级方案：未装 Docker / Milvus 不可用时，用 Spring AI 自带的本地向量库顶替。
 * 激活方式：-Dspring-boot.run.profiles=local,simple-vector
 * 与 Milvus 的差异：内存计算余弦相似度，无向量索引，数据量大时检索变慢，
 * 仅用于开发期验证 RAG 链路；正式环境（答辩演示）必须切回 Milvus。
 */
@Configuration
@Profile("simple-vector")
public class SimpleVectorStoreConfig {

    private static final Logger log = LoggerFactory.getLogger(SimpleVectorStoreConfig.class);

    @Value("${app.simple-vector.file:./data/simple-vector.json}")
    private String storeFile;

    private SimpleVectorStore store;

    @Bean
    public VectorStore vectorStore(EmbeddingModel embeddingModel) {
        store = SimpleVectorStore.builder(embeddingModel).build();
        File file = new File(storeFile);
        if (file.exists()) {
            store.load(new FileSystemResource(file));
            log.info("SimpleVectorStore 已从 {} 恢复数据", file.getAbsolutePath());
        }
        return store;
    }

    /** 停机时持久化到本地 JSON，重启后知识库不丢 */
    @PreDestroy
    public void persist() {
        if (store == null) return;
        try {
            File file = new File(storeFile);
            if (file.getParentFile() != null) {
                file.getParentFile().mkdirs();
            }
            store.save(file);
            log.info("SimpleVectorStore 已持久化到 {}", file.getAbsolutePath());
        } catch (Exception e) {
            log.warn("SimpleVectorStore 持久化失败: {}", e.getMessage());
        }
    }
}
