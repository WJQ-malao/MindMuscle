package com.fitness.ai.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PlanCreateRequest(
        @NotBlank(message = "计划名称不能为空")
        @Size(max = 200, message = "计划名称过长")
        String title,

        @NotBlank(message = "训练目标不能为空")
        @Size(max = 50, message = "目标过长")
        String goal,

        /** 每周训练天数，篮子动作将自动分摊到各训练日 */
        @NotNull(message = "每周训练天数不能为空")
        @Min(value = 1, message = "每周训练天数必须在 1-7 之间")
        @Max(value = 7, message = "每周训练天数必须在 1-7 之间")
        Integer daysPerWeek,

        /** 已选动作清单 */
        @NotNull(message = "请至少选择一个动作")
        @Size(min = 1, message = "请至少选择一个动作")
        List<@Valid Item> items
) {

    public record Item(
            @NotBlank(message = "动作名称不能为空")
            @Size(max = 100, message = "动作名称过长")
            String exerciseName,

            @NotBlank(message = "肌群不能为空")
            String muscleGroup,

            @Min(value = 1, message = "组数必须在 1-30 之间")
            @Max(value = 30, message = "组数必须在 1-30 之间")
            int sets,

            @Min(value = 1, message = "次数必须在 1-100 之间")
            @Max(value = 100, message = "次数必须在 1-100 之间")
            int reps
    ) {
    }
}
