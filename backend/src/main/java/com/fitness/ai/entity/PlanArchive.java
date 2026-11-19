package com.fitness.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("plan_archive")
public class PlanArchive {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    /** 存档名称（可编辑） */
    private String name;
    /** 训练目标（快照自计划） */
    private String goal;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
