package com.fitness.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("workout_item")
public class WorkoutItem {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long planId;
    /** 1-7 对应周一到周日 */
    private Integer dayOfWeek;
    private String exerciseName;
    /** 主要训练肌群：胸/背/腿/肩/手臂/核心/有氧 */
    private String muscleGroup;
    private Integer sets;
    private Integer reps;
    private Integer sort;
}
