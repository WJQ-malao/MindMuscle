package com.fitness.ai.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fitness.ai.common.Result;
import com.fitness.ai.dto.ExerciseUpsertRequest;
import com.fitness.ai.entity.ExerciseLib;
import com.fitness.ai.mapper.ExerciseLibMapper;
import com.fitness.ai.security.RequireRole;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 健身动作库：按肌群划分的标准动作，供训练计划添加动作时选择 */
@RestController
@RequestMapping("/api/exercises")
public class ExerciseController {

    private final ExerciseLibMapper exerciseLibMapper;

    public ExerciseController(ExerciseLibMapper exerciseLibMapper) {
        this.exerciseLibMapper = exerciseLibMapper;
    }

    /** 全部动作或按肌群过滤，按肌群+排序返回 */
    @GetMapping
    public Result<List<ExerciseLib>> list(@RequestParam(required = false) String muscleGroup) {
        LambdaQueryWrapper<ExerciseLib> wrapper = new LambdaQueryWrapper<>();
        if (muscleGroup != null && !muscleGroup.isBlank()) {
            wrapper.eq(ExerciseLib::getMuscleGroup, muscleGroup);
        }
        wrapper.orderByAsc(ExerciseLib::getMuscleGroup).orderByAsc(ExerciseLib::getSort);
        return Result.ok(exerciseLibMapper.selectList(wrapper));
    }

    /** 新增动作（仅管理员，RBAC: exercise:manage） */
    @PostMapping
    @RequireRole("ADMIN")
    public Result<ExerciseLib> create(@Valid @RequestBody ExerciseUpsertRequest request) {
        Long count = exerciseLibMapper.selectCount(new LambdaQueryWrapper<ExerciseLib>()
                .eq(ExerciseLib::getName, request.name()));
        if (count > 0) {
            throw new IllegalArgumentException("已存在同名动作「" + request.name() + "」");
        }
        ExerciseLib exercise = new ExerciseLib();
        apply(exercise, request);
        exerciseLibMapper.insert(exercise);
        return Result.ok(exercise);
    }

    /** 编辑动作（仅管理员） */
    @PutMapping("/{id}")
    @RequireRole("ADMIN")
    public Result<ExerciseLib> update(@PathVariable Long id, @Valid @RequestBody ExerciseUpsertRequest request) {
        ExerciseLib exercise = exerciseLibMapper.selectById(id);
        if (exercise == null) {
            throw new IllegalArgumentException("动作不存在");
        }
        Long count = exerciseLibMapper.selectCount(new LambdaQueryWrapper<ExerciseLib>()
                .eq(ExerciseLib::getName, request.name()).ne(ExerciseLib::getId, id));
        if (count > 0) {
            throw new IllegalArgumentException("已存在同名动作「" + request.name() + "」");
        }
        apply(exercise, request);
        exerciseLibMapper.updateById(exercise);
        return Result.ok(exercise);
    }

    /** 删除动作（仅管理员）。训练项仅存动作名字符串，无外键关联，删除不影响历史计划 */
    @DeleteMapping("/{id}")
    @RequireRole("ADMIN")
    public Result<String> delete(@PathVariable Long id) {
        if (exerciseLibMapper.deleteById(id) == 0) {
            throw new IllegalArgumentException("动作不存在");
        }
        return Result.ok("已删除");
    }

    private void apply(ExerciseLib exercise, ExerciseUpsertRequest request) {
        exercise.setName(request.name());
        exercise.setMuscleGroup(request.muscleGroup());
        exercise.setEquipment(request.equipment() == null || request.equipment().isBlank() ? null : request.equipment());
        exercise.setTips(request.tips() == null || request.tips().isBlank() ? null : request.tips());
        exercise.setSort(request.sort());
    }
}
