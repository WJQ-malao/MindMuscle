package com.fitness.ai.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fitness.ai.common.Result;
import com.fitness.ai.entity.ExerciseLib;
import com.fitness.ai.mapper.ExerciseLibMapper;
import org.springframework.web.bind.annotation.GetMapping;
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
}
