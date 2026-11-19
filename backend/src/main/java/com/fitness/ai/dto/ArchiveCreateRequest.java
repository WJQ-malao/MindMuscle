package com.fitness.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 把某个训练计划快照保存为存档 */
public record ArchiveCreateRequest(
        @NotNull(message = "计划ID不能为空")
        Long planId,

        @NotBlank(message = "存档名称不能为空")
        @Size(max = 100, message = "存档名称过长")
        String name
) {
}
