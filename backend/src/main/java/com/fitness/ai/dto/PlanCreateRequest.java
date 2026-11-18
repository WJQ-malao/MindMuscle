package com.fitness.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PlanCreateRequest(
        @NotBlank(message = "计划名称不能为空")
        @Size(max = 200, message = "计划名称过长")
        String title,

        @NotBlank(message = "训练目标不能为空")
        @Size(max = 50, message = "目标过长")
        String goal
) {
}
