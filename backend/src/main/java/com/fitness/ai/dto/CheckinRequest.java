package com.fitness.ai.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CheckinRequest(
        @NotNull(message = "训练项ID不能为空")
        Long itemId,

        /** 第几组（按组打卡，从1开始） */
        @NotNull(message = "组序号不能为空")
        @Min(value = 1, message = "组序号必须在 1-30 之间")
        @Max(value = 30, message = "组序号必须在 1-30 之间")
        Integer setNo,

        @NotNull(message = "次数不能为空")
        @Min(value = 1, message = "次数必须在 1-100 之间")
        @Max(value = 100, message = "次数必须在 1-100 之间")
        Integer repsDone,

        Double weightUsed
) {
}
