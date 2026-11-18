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
    /** 器械分类：杠铃/哑铃/器械/自重/有氧 */
    private String equipment;
    /** 动作要点（文本讲解） */
    private String tips;
    /** 插图地址（预留扩展） */
    private String imageUrl;
    private Integer sort;
}
