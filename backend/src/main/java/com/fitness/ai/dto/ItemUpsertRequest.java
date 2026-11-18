package com.fitness.ai.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ItemUpsertRequest(
        @NotBlank(message = "动作名称不能为空")
        @Size(max = 100, message = "动作名称过长")
        String exerciseName,

        @NotBlank(message = "肌群不能为空")
        String muscleGroup,

        @NotNull(message = "训练日不能为空")
        @Min(value = 1, message = "训练日必须为 1-7")
        @Max(value = 7, message = "训练日必须为 1-7")
        Integer dayOfWeek,

        @NotNull(message = "组数不能为空")
        @Min(value = 1, message = "组数必须在 1-30 之间")
        @Max(value = 30, message = "组数必须在 1-30 之间")
        Integer sets,

        @NotNull(message = "次数不能为空")
        @Min(value = 1, message = "次数必须在 1-100 之间")
        @Max(value = 100, message = "次数必须在 1-100 之间")
        Integer reps
) {
}
