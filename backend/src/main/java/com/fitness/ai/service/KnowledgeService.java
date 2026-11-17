package com.fitness.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fitness.ai.config.SimpleVectorStoreConfig;
import com.fitness.ai.entity.KnowledgeFile;
import com.fitness.ai.mapper.KnowledgeFileMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RAG 知识库服务：文档上传 -> Tika 解析 -> 切片 -> 向量化 -> 写入 Milvus。
 * 入库与检索共享同一个 Embedding 模型（bge-m3），由 ChatClient 上的
 * RetrievalAugmentationAdvisor 在问答时自动完成检索。
 */
@Service
public class KnowledgeService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeService.class);

    private final VectorStore vectorStore;
    private final KnowledgeFileMapper knowledgeFileMapper;
    private final ObjectProvider<SimpleVectorStoreConfig> simpleStoreProvider;

    public KnowledgeService(VectorStore vectorStore, KnowledgeFileMapper knowledgeFileMapper,
                            ObjectProvider<SimpleVectorStoreConfig> simpleStoreProvider) {
        this.vectorStore = vectorStore;
        this.knowledgeFileMapper = knowledgeFileMapper;
        this.simpleStoreProvider = simpleStoreProvider;
    }

    public Map<String, Object> upload(MultipartFile file) {
        String fileName = file.getOriginalFilename();
        try {
            // 1. 解析（PDF/TXT/DOCX）
            TikaDocumentReader reader = new TikaDocumentReader(new InputStreamResource(file.getInputStream()));
            List<Document> documents = reader.read();

            // 2. 切片：500 token，50 重叠
            TokenTextSplitter splitter = new TokenTextSplitter(500, 50, 5, 5000, true);
            List<Document> chunks = splitter.apply(documents);

            // 3. 元数据：来源文件 + 切片序号，用于溯源和按文件删除
            for (int i = 0; i < chunks.size(); i++) {
                chunks.get(i).getMetadata().put("file_name", fileName);
                chunks.get(i).getMetadata().put("chunk_index", i);
            }

            // 4. 向量化并写入 Milvus（Embedding 走 EmbeddingConfig 的 bge-m3）
            vectorStore.add(chunks);

            // 5. 登记文件元信息（MySQL 维护，向量库只存切片）
            KnowledgeFile record = new KnowledgeFile();
            record.setFileName(fileName);
            record.setChunkCount(chunks.size());
            knowledgeFileMapper.insert(record);
            persistIfSimpleStore();

            log.info("知识库入库完成: {}, 切片数: {}", fileName, chunks.size());
            Map<String, Object> result = new HashMap<>();
            result.put("fileName", fileName);
            result.put("chunkCount", chunks.size());
            return result;
        } catch (Exception e) {
            log.error("文档入库失败: {}", fileName, e);
            throw new RuntimeException("文档处理失败: " + e.getMessage(), e);
        }
    }

    public List<KnowledgeFile> listFiles() {
        return knowledgeFileMapper.selectList(new LambdaQueryWrapper<KnowledgeFile>()
                .orderByDesc(KnowledgeFile::getId));
    }

    /** 按 file_name 元数据删除 Milvus 中的切片，并移除登记记录 */
    public void delete(String fileName) {
        Filter.Expression expression = new FilterExpressionBuilder()
                .eq("file_name", "'" + fileName.replace("'", "") + "'")
                .build();
        vectorStore.delete(expression);
        knowledgeFileMapper.delete(new LambdaQueryWrapper<KnowledgeFile>()
                .eq(KnowledgeFile::getFileName, fileName));
        persistIfSimpleStore();
        log.info("知识库文件已删除: {}", fileName);
    }

    /**
     * 降级模式（SimpleVectorStore）下知识库变更即落盘，
     * 避免依赖停机钩子（进程被强杀时钩子不执行导致数据丢失）。
     * Milvus 模式下该 Bean 不存在，自动跳过。
     */
    private void persistIfSimpleStore() {
        simpleStoreProvider.ifAvailable(SimpleVectorStoreConfig::persist);
    }
}
