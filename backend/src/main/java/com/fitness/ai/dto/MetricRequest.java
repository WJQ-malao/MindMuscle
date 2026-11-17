package com.fitness.ai.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record MetricRequest(
        @NotNull(message = "体重不能为空") Double weightKg,
        Double bodyFat,
        LocalDate recordDate
) {
}
