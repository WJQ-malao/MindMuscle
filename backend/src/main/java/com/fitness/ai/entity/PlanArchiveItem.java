package com.fitness.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("plan_archive_item")
public class PlanArchiveItem {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long archiveId;
    /** 1-7 对应周一到周日 */
    private Integer dayOfWeek;
    private String exerciseName;
    private String muscleGroup;
    private Integer sets;
    private Integer reps;
    /** 建议重量（kg），NULL = 自重/未指定 */
    private Double weightKg;
    private Integer sort;
}
