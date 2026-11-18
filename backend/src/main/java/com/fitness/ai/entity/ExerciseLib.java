package com.fitness.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("exercise_lib")
public class ExerciseLib {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String muscleGroup;
    private Integer sort;
}
