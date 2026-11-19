package com.fitness.ai.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 动作库新增/编辑请求（仅管理员）
 */
public record ExerciseUpsertRequest(

        @NotBlank(message = "动作名称不能为空")
        @Size(max = 100, message = "动作名称过长")
        String name,

        /** 肌群: 胸/背/腿/肩/手臂/核心/有氧 */
        @NotBlank(message = "肌群不能为空")
        String muscleGroup,

        /** 器械分类：杠铃/哑铃/器械/自重/有氧，可空 */
        @Size(max = 20, message = "器械分类过长")
        String equipment,

        /** 动作要点 */
        @Size(max = 2000, message = "动作要点过长")
        String tips,

        /** 组内排序 */
        @Min(value = 0, message = "排序不能为负")
        @Max(value = 9999, message = "排序过大")
        int sort
) {
}
