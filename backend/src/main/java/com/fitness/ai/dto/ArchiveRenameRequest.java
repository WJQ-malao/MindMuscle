package com.fitness.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 重命名计划存档 */
public record ArchiveRenameRequest(
        @NotBlank(message = "存档名称不能为空")
        @Size(max = 100, message = "存档名称过长")
        String name
) {
}
