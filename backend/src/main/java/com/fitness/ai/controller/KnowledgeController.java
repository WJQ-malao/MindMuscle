package com.fitness.ai.controller;

import com.fitness.ai.common.Result;
import com.fitness.ai.entity.KnowledgeFile;
import com.fitness.ai.service.KnowledgeService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/** 健身知识库管理：上传文档入库 Milvus，供 RAG 问答检索 */
@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeController {

    private final KnowledgeService knowledgeService;

    public KnowledgeController(KnowledgeService knowledgeService) {
        this.knowledgeService = knowledgeService;
    }

    @PostMapping("/upload")
    public Result<Map<String, Object>> upload(@RequestParam("file") MultipartFile file) {
        return Result.ok(knowledgeService.upload(file));
    }

    @GetMapping("/files")
    public Result<List<KnowledgeFile>> list() {
        return Result.ok(knowledgeService.listFiles());
    }

    @DeleteMapping("/files")
    public Result<String> delete(@RequestParam String fileName) {
        knowledgeService.delete(fileName);
        return Result.ok("已删除");
    }
}
