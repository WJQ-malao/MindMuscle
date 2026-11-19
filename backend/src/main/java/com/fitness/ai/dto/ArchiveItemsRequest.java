package com.fitness.ai.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/** 存档内容的创建/整体替换（从选择器直接保存或编辑存档） */
public record ArchiveItemsRequest(
        /** 存档名称：创建时必填，替换时可空（表示不改名） */
        @Size(max = 100, message = "存档名称过长")
        String name,

        @Size(max = 50, message = "目标过长")
        String goal,

        @NotNull(message = "动作清单不能为空")
        @Size(min = 1, message = "至少包含一个动作")
        List<@Valid Item> items
) {

    public record Item(
            @NotNull(message = "训练日不能为空")
            @Min(value = 1, message = "训练日必须在 1-7 之间")
            @Max(value = 7, message = "训练日必须在 1-7 之间")
            Integer dayOfWeek,

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
            int reps,

            /** 建议重量（kg），空=自重 */
            @Min(value = 0, message = "重量不能为负")
            @Max(value = 1000, message = "重量不能超过 1000kg")
            Double weightKg
    ) {
    }
}
