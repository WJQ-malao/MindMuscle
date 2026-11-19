package com.fitness.ai.controller;

import com.fitness.ai.common.Result;
import com.fitness.ai.dto.ArchiveCreateRequest;
import com.fitness.ai.dto.ArchiveRenameRequest;
import com.fitness.ai.entity.PlanArchive;
import com.fitness.ai.entity.PlanArchiveItem;
import com.fitness.ai.util.UserContext;
import com.fitness.ai.service.PlanArchiveService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 计划存档（模板）：保存/重命名/删除/读取，供训练计划复用 */
@RestController
@RequestMapping("/api/plan-archives")
public class PlanArchiveController {

    private final PlanArchiveService archiveService;

    public PlanArchiveController(PlanArchiveService archiveService) {
        this.archiveService = archiveService;
    }

    @GetMapping
    public Result<List<PlanArchive>> list() {
        return Result.ok(archiveService.list(UserContext.getUserId()));
    }

    /** 存档明细（按天排序） */
    @GetMapping("/{id}/items")
    public Result<List<PlanArchiveItem>> items(@PathVariable Long id) {
        return Result.ok(archiveService.items(UserContext.getUserId(), id));
    }

    /** 把某个训练计划快照保存为存档 */
    @PostMapping
    public Result<PlanArchive> create(@Valid @RequestBody ArchiveCreateRequest request) {
        return Result.ok(archiveService.createFromPlan(
                UserContext.getUserId(), request.planId(), request.name()));
    }

    /** 重命名存档 */
    @PutMapping("/{id}")
    public Result<PlanArchive> rename(@PathVariable Long id,
                                      @Valid @RequestBody ArchiveRenameRequest request) {
        return Result.ok(archiveService.rename(UserContext.getUserId(), id, request.name()));
    }

    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id) {
        archiveService.delete(UserContext.getUserId(), id);
        return Result.ok("已删除");
    }
}
