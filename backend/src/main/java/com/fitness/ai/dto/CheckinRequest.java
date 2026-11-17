package com.fitness.ai.dto;

import jakarta.validation.constraints.NotNull;

public record CheckinRequest(
        @NotNull(message = "训练项ID不能为空") Long itemId,
        @NotNull(message = "完成组数不能为空") Integer setsDone,
        @NotNull(message = "每组次数不能为空") Integer repsDone,
        Double weightUsed
) {
}
