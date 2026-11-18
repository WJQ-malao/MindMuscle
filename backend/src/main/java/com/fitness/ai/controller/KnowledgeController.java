package com.fitness.ai.controller;

import com.fitness.ai.common.Result;
import com.fitness.ai.entity.KnowledgeFile;
import com.fitness.ai.security.RequireRole;
import com.fitness.ai.service.KnowledgeService;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/** 健身知识库管理：仅管理员可维护（RBAC: knowledge:manage 权限点），供 RAG 问答检索 */
@RestController
@RequestMapping("/api/knowledge")
@RequireRole("ADMIN")
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

    /** 文本类文件在线预览（txt/md/json 等），其余格式走下载 */
    @GetMapping("/files/{id}/preview")
    public Result<Map<String, Object>> preview(@PathVariable Long id) {
        return Result.ok(knowledgeService.preview(id));
    }

    /** 下载原始文件 */
    @GetMapping("/files/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        return knowledgeService.download(id);
    }

    @DeleteMapping("/files/{id}")
    public Result<String> delete(@PathVariable Long id) {
        knowledgeService.delete(id);
        return Result.ok("已删除");
    }
}
