package com.fitness.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("workout_plan")
public class WorkoutPlan {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String title;
    /** 目标：减脂 / 增肌 / 保持 等 */
    private String goal;
    /** ACTIVE / ARCHIVED */
    private String status;
    /** 来源：AI 生成或手动创建 */
    private String source;
    private LocalDateTime createTime;
}
