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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * RAG 知识库服务：
 * 上传 -> 原始文件落盘（供查看/下载）-> Tika 解析 -> 切片 -> 向量化 -> 写入向量库。
 * 同名文件重复上传视为"重新入库"（覆盖语义）：先清除旧向量与旧登记，再写入。
 * 入库与检索共享同一个 Embedding 模型（bge-m3），由 ChatClient 上的
 * RetrievalAugmentationAdvisor 在问答时自动完成检索。
 */
@Service
public class KnowledgeService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeService.class);
    /** 支持纯文本预览的扩展名 */
    private static final Set<String> TEXT_PREVIEW_EXT = Set.of("txt", "md", "markdown", "json", "log", "csv");
    /** 预览文本大小上限，防止大文件撑爆前端 */
    private static final int PREVIEW_MAX_BYTES = 200 * 1024;

    private final VectorStore vectorStore;
    private final KnowledgeFileMapper knowledgeFileMapper;
    private final ObjectProvider<SimpleVectorStoreConfig> simpleStoreProvider;
    private final Path storeDir;

    public KnowledgeService(VectorStore vectorStore, KnowledgeFileMapper knowledgeFileMapper,
                            ObjectProvider<SimpleVectorStoreConfig> simpleStoreProvider,
                            @Value("${app.knowledge.dir:./knowledge-files}") String storeDir) throws IOException {
        this.vectorStore = vectorStore;
        this.knowledgeFileMapper = knowledgeFileMapper;
        this.simpleStoreProvider = simpleStoreProvider;
        this.storeDir = Path.of(storeDir).toAbsolutePath().normalize();
        Files.createDirectories(this.storeDir);
    }

    public Map<String, Object> upload(MultipartFile file) {
        String fileName = file.getOriginalFilename();
        try {
            // 同名覆盖：清除旧的向量切片与登记记录（重复上传=重新入库）
            KnowledgeFile existing = knowledgeFileMapper.selectOne(new LambdaQueryWrapper<KnowledgeFile>()
                    .eq(KnowledgeFile::getFileName, fileName)
                    .orderByDesc(KnowledgeFile::getId)
                    .last("LIMIT 1"));
            if (existing != null) {
                deleteVectorsByFileName(fileName);
                deleteDiskFile(existing.getFilePath());
                knowledgeFileMapper.deleteById(existing.getId());
            }

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

            // 4. 向量化并写入向量库（Embedding 走 EmbeddingConfig 的 bge-m3）
            vectorStore.add(chunks);

            // 5. 登记文件元信息（MySQL 维护，向量库只存切片）
            KnowledgeFile record = new KnowledgeFile();
            record.setFileName(fileName);
            record.setChunkCount(chunks.size());
            knowledgeFileMapper.insert(record);

            // 6. 原始文件落盘，供管理页查看/下载
            String filePath = saveOriginal(file, record.getId());
            record.setFilePath(filePath);
            knowledgeFileMapper.updateById(record);

            persistIfSimpleStore();
            log.info("知识库入库完成: {}, 切片数: {}, 原件: {}", fileName, chunks.size(), filePath);
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

    /** 按 file_name 元数据删除向量库中的切片（仅向量，不动登记记录） */
    private void deleteVectorsByFileName(String fileName) {
        Filter.Expression expression = new FilterExpressionBuilder()
                .eq("file_name", "'" + fileName.replace("'", "") + "'")
                .build();
        vectorStore.delete(expression);
        persistIfSimpleStore();
    }

    /** 按文件ID删除：向量 + 登记记录 + 磁盘原件 */
    public void delete(Long id) {
        KnowledgeFile record = knowledgeFileMapper.selectById(id);
        if (record == null) {
            throw new IllegalArgumentException("文件不存在");
        }
        deleteVectorsByFileName(record.getFileName());
        deleteDiskFile(record.getFilePath());
        knowledgeFileMapper.deleteById(id);
        log.info("知识库文件已删除: {}", record.getFileName());
    }

    /** 文本预览：仅支持纯文本类格式，超大文件截断 */
    public Map<String, Object> preview(Long id) {
        KnowledgeFile record = requireFile(id);
        String ext = extensionOf(record.getFileName());
        if (!TEXT_PREVIEW_EXT.contains(ext)) {
            throw new IllegalArgumentException("该格式（." + ext + "）不支持在线预览，请下载查看");
        }
        try {
            byte[] bytes = Files.readAllBytes(resolveDiskFile(record.getFilePath()).toPath());
            boolean truncated = bytes.length > PREVIEW_MAX_BYTES;
            String text = new String(bytes, 0, truncated ? PREVIEW_MAX_BYTES : bytes.length, StandardCharsets.UTF_8);
            Map<String, Object> result = new HashMap<>();
            result.put("fileName", record.getFileName());
            result.put("text", text);
            result.put("truncated", truncated);
            return result;
        } catch (IOException e) {
            throw new RuntimeException("读取文件失败: " + e.getMessage(), e);
        }
    }

    /** 下载原始文件 */
    public ResponseEntity<Resource> download(Long id) {
        KnowledgeFile record = requireFile(id);
        Resource resource = new FileSystemResource(resolveDiskFile(record.getFilePath()));
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''" + java.net.URLEncoder.encode(record.getFileName(), StandardCharsets.UTF_8))
                .body(resource);
    }

    private KnowledgeFile requireFile(Long id) {
        KnowledgeFile record = knowledgeFileMapper.selectById(id);
        if (record == null) {
            throw new IllegalArgumentException("文件不存在");
        }
        if (record.getFilePath() == null) {
            throw new IllegalStateException("该记录为历史数据，未保留原件，请删除后重新上传");
        }
        return record;
    }

    private String saveOriginal(MultipartFile file, Long id) throws IOException {
        String original = file.getOriginalFilename() == null ? "unknown" : file.getOriginalFilename();
        String safe = original.replaceAll("[\\\\/:*?\"<>|]", "_");
        Path target = storeDir.resolve(id + "_" + safe);
        file.transferTo(target);
        // 存相对路径，避免后端换目录后失效
        return storeDir.relativize(target).toString();
    }

    private java.io.File resolveDiskFile(String filePath) {
        return storeDir.resolve(filePath).toFile();
    }

    private void deleteDiskFile(String filePath) {
        if (filePath == null) {
            return;
        }
        try {
            Files.deleteIfExists(resolveDiskFile(filePath).toPath());
        } catch (IOException e) {
            log.warn("删除磁盘原件失败 {}: {}", filePath, e.getMessage());
        }
    }

    private String extensionOf(String fileName) {
        int idx = fileName.lastIndexOf('.');
        return idx < 0 ? "" : fileName.substring(idx + 1).toLowerCase();
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
